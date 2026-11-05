# VeloCity reference context map: review (2026-11-05)

Reviewed: `src/main/cml/VeloCity-Context-Map.cml` and `velocity/velocity-context-map.md`. The original CML is unchanged. The proposed fix is in `src/main/cml/VeloCity-Context-Map-Proposed.cml`.

The domain expert couldn't state the modelling rules ("those are for the method you're running"). I checked the map against these common rules, so please confirm they match yours:

- R1: no Shared Kernel (and no Partnership unless you deliberately allow it).
- R2: no upstream/downstream cycles. Any directed cycle counts, not only two contexts pointing at each other.
- R3: ACL and CF go only on the downstream end, OHS and PL only on the upstream end.
- R4: each relationship names its integration pattern explicitly. A bare `[U]->[D]` doesn't.

I checked the business facts against the four processes (onboarding, rent/return, settle and recover debt, fault to maintenance) with the domain expert.

## Findings

| # | Severity | Rule | Finding |
|---|---|---|---|
| 1 | **Breaks a rule** | R2 | **Directed cycle: Accounts → Rentals → Settlement → Accounts.** `Accounts [U]->[D] Rentals`, `Rentals [U,PL]->[D,CF] Settlement`, `Settlement [U,PL]->[D,CF] Accounts`. The doc says "No cycles", but its check (*"if two contexts have arrows in both directions"*) only finds 2-node cycles, so this 3-node cycle isn't caught. |
| 2 | **Breaks a rule (in the debrief)** | R2 | The accepted alternative **"Debt Recovery → Accounts for unblocking"** adds another cycle: Accounts → Rentals → Settlement → DebtRecovery → Accounts. The expert's facts also contradict it: "debt is paid" comes from the side that owns the balance (Settlement), even when the rider pays during recovery. Drop it from the accepted alternatives, or keep it only as a discussion point. |
| 3 | Wrong vs. confirmed facts | - | **CustomerSupport → FleetMaintenance (unconfirmed)**. The expert has now confirmed the flow. Support accepts a fleet-issue incident and escalates it to the technicians' side. From then on the bike is broken. Technicians repair it and decide when it returns to circulation, and the incident is then resolved. Support needs Fleet Maintenance's "report broken" capability and its "fixed / back in circulation" outcome. So Fleet Maintenance is upstream (OHS/PL) and Customer Support is downstream, the opposite of the dashed arrow. |
| 4 | Missing relationship | - | **Billing dispute → pause Debt Recovery** is listed under "Deferred / Open", but the expert confirms it. Support raises "dispute under review" and Debt Recovery pauses (and resumes once the dispute is settled). Regulatory holds and payment plans come from within Debt Recovery. Add `CustomerSupport [U,PL]->[D,CF] DebtRecovery`. |
| 5 | Breaks a rule | R4 | Bare relationships: `Accounts [U]->[D] Rentals` and `CustomerSupport [U]->[D] FleetMaintenance`. The expert says Rentals only asks whether the account is active and nothing flows back, so `Accounts [U,OHS]->[D,CF] Rentals` (or `[D,ACL]`). The two external upstreams (`PaymentProcessor [U]`, `City [U]`) are bare as well. That's acceptable for external systems you can't influence, but say so explicitly. |
| 6 | OK | R1 | No Shared Kernel and no Partnership. |
| 7 | OK | R3 | All ACL/CF are on downstream ends and all OHS/PL on upstream ends. |
| 8 | Note | - | `City [U]->[D,CF] Pricing`: conforming directly to an external party's model is allowed, but an ACL is the more defensible default for an external system. Worth a sentence in the debrief. |

## Fixing the cycle (finding 1)

Each of the three edges matches the business on its own: Rentals asks Accounts, Settlement reacts to Rental Session Ended, and blocks follow Settlement's debt facts. So the problem is the direction of one integration, not a wrong boundary. The cheapest edge to flip is **Settlement ↔ Accounts**:

- **Proposed:** `Accounts [U,OHS,PL]->[D,CF] Settlement : DebtBlocking`. Accounts offers a block/unblock API with a reason, so the block stays in Accounts' language. Settlement calls it on *Rental Fee Unpaid* / *Outstanding Debt Cleared*. Settlement is still the *source* of the fact, as the expert requires. Only the language of the integration changes.
- Alternative for the debrief: keep Settlement upstream and make Accounts → Rentals not an upstream/downstream relationship. That isn't realistic, because Rentals must ask about account status before unlocking.

The result is a DAG: PaymentProcessor → Payments → {Accounts, Settlement}; Accounts → {Rentals, Settlement}; Rentals → {Settlement, FleetMaintenance}; Pricing, City → Settlement / Pricing; Settlement → DebtRecovery; FleetMaintenance → CustomerSupport → DebtRecovery.

## Doc changes to go with it (velocity-context-map.md)

- "No cycles" decision: define a cycle as *any directed cycle* and replace the two-arrows heuristic with "follow the arrows; you must never get back to where you started".
- Mermaid diagrams: flip SET→ACC in the main map and in the "ride to money" zoom, replace the dashed SUP→FLT with FLT→SUP, and add SUP→DEB.
- "Accepted alternatives": remove or caveat *Debt Recovery → Accounts* (finding 2). *Fleet Maintenance upstream of Rentals* is still cycle-free with the proposed map.
- "Deferred / Open": move the billing-dispute and fleet-issue routing items to confirmed. "Faults signaled by the bike itself → Rentals" remains open.

The proposed CML was not parsed by Context Mapper in this review. Run `VelocityContextMap` against it (after copying it over the original, if you accept it) to render and check it.
