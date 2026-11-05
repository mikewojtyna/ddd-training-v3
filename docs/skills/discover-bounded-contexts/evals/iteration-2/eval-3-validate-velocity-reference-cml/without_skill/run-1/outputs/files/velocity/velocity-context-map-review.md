# VeloCity Reference Context Map: Review (2026-11-05)

Reviewed file: `src/main/cml/VeloCity-Context-Map.cml` (unchanged). The proposed correction is in `src/main/cml/VeloCity-Context-Map.cml` under this review's output folder.

I checked the map against:
- `velocity/velocity-vision.md`
- `velocity/velocity-context-map.md`, the reference doc. Its own rule is: "No cycles. If two contexts have arrows in both directions, ask whose language the integration uses."
- the standard relationship rules:
  - no Shared Kernel;
  - no upstream/downstream cycles;
  - ACL and CF sit on the downstream side;
  - OHS and PL sit on the upstream side;
  - every relationship has explicit roles.
- the domain expert's answers on four open business questions.

## Findings

| # | Severity | Finding | Fix |
|---|---|---|---|
| 1 | **Breaks a rule** | **Upstream/downstream cycle: Settlement → Accounts → Rentals → Settlement.** The three edges are `Settlement [U]->[D] Accounts : DebtBlocking`, `Accounts [U]->[D] Rentals : AccountStatus` and `Rentals [U]->[D] Settlement : RentalSessionEnded`. The reference doc says "No cycles", and its "Zoom" section only checks the ride-to-money flow, which is how this cycle was missed. | Invert Accounts ↔ Rentals (see "Breaking the cycle" below). |
| 2 | Rule gap | `Accounts [U]->[D] Rentals` has no patterns on either side. It is the only internal relationship whose integration style is left open. | Fixed together with #1. |
| 3 | Rule gap | `City [U]->[D,CF] Pricing`: Pricing conforms to the zone-map model of an external party that VeloCity doesn't control (the expert confirmed City is external). Payments protects itself from the Payment Processor with an ACL, but Pricing has no protection from the City. | `[D,ACL]`, or justify CF explicitly in the debrief. |
| 4 | Minor | `Pricing [U,OHS]` has no PL, while every other OHS in the map is paired with a PL. | `[U,OHS,PL]`. |
| 5 | Unconfirmed in a reference solution | `CustomerSupport [U]->[D] FleetMaintenance : FleetIssueIncident_Unconfirmed`. **Now confirmed by the expert:** support classifies the incident as a fleet issue and hands it to maintenance, and never withdraws a bike itself. Maintenance marks the bike broken and has Rentals withdraw it. Support still drives the incident to resolved. The direction is wrong: support is the caller, so it is downstream. | `FleetMaintenance [U,OHS]->[D,CF] CustomerSupport : FleetIssueHandover`. Delete "Unconfirmed" from the name and from the doc's Deferred section. |
| 6 | **Missing relationship** | **Billing disputes pause debt recovery.** The expert confirmed that accepting a billing-dispute incident pauses recovery. Resolving the dispute resumes recovery, or ends it if the dispute cleared the debt. Payment plans and regulatory holds are paused by Debt Recovery itself. The map has no CustomerSupport ↔ DebtRecovery relationship, and the doc lists this as "not modeled yet". | `DebtRecovery [U,OHS]->[D,CF] CustomerSupport : RecoveryPause`. |
| 7 | Confirmed, no change | Debt blocking is owned by Accounts. Settlement only reports *Rental Fee Unpaid* and *Outstanding Debt Cleared*, including when the money came in through Debt Recovery, and Accounts blocks, notifies and unblocks. `Settlement [U,PL]->[D,CF] Accounts` is correct. | None. Drop the "Debt Recovery → Accounts" accepted alternative, or keep it only as a discussion prompt. |
| 8 | Confirmed, no change | Faults signalled by the bike reach Rentals first. Rentals withdraws the bike, ends the session on a lock failure and tells the rider. Maintenance is notified next and alone decides when the bike becomes available again. This matches `Rentals [U,OHS,PL]->[D,CF] FleetMaintenance`. | Remove it from "Deferred / Open". |
| 9 | Open question | If a dispute is resolved in the rider's favour and "the dispute cleared the debt", something has to cancel the debt in Settlement, the only owner of the balance. The map has no Customer Support → Settlement (or Debt Recovery → Settlement) path for writing off debt. | Ask the expert who writes off a disputed debt before adding an edge. |

No Shared Kernel or Partnership is used. The Payment Processor ACL is correctly on the downstream side, and all CF, OHS and PL markers are on the correct sides.

## Breaking the cycle

To break the cycle, one of its three edges has to change direction:

- **Rentals → Settlement** can't change: Settlement needs Rentals' facts in Rentals' language.
- **Settlement → Accounts** can't change either. The expert confirmed that Accounts reacts to Settlement's facts (*Rental Fee Unpaid*, *Outstanding Debt Cleared*) and owns the block rule.
- **Accounts → Rentals** is the edge to change, and the reference doc already supports this. Rentals offers "reserve & unlock for any authorised client" in generic language, the same way it offers *available / withdrawn* to Fleet Maintenance. Accounts calls Rentals' OHS to authorise the rider when the account is activated and to revoke authorisation when it is blocked or unblocked. Rentals stays free of the Accounts language (blocked, onboarding).

The resulting map is acyclic. One topological order is: PaymentProcessor, City, Rentals, Payments, Pricing, FleetMaintenance, Settlement, Accounts, DebtRecovery, CustomerSupport.

Alternative for the debrief: Accounts could stay upstream of Rentals if Accounts exposes an OHS/PL that Settlement calls ("report unpaid fee", "report debt cleared"). Settlement would then be downstream of Accounts. This conflicts less with the "is account active?" wording, but it puts Settlement in Accounts' language. The expert placed the facts on the money side, so I'd keep the version above.

## Proposed corrected map (relationships only)

```
PaymentProcessor [U]->[D,ACL] Payments          : PaymentProcessing
City             [U]->[D,ACL] Pricing           : ZoneMap               (changed: CF -> ACL)
Payments   [U,OHS,PL]->[D,CF] Accounts          : PaymentMethodVerification
Payments   [U,OHS,PL]->[D,CF] Settlement        : Charging
Rentals    [U,OHS,PL]->[D,CF] Accounts          : ClientAuthorization   (changed: was Accounts [U]->[D] Rentals)
Rentals       [U,PL]->[D,CF] Settlement         : RentalSessionEnded
Rentals    [U,OHS,PL]->[D,CF] FleetMaintenance  : BikeAvailability
Pricing    [U,OHS,PL]->[D,CF] Settlement        : PriceCalculation      (changed: + PL)
Settlement    [U,PL]->[D,CF] Accounts           : DebtBlocking
Settlement [U,OHS,PL]->[D,CF] DebtRecovery      : DebtCollection
FleetMaintenance [U,OHS]->[D,CF] CustomerSupport : FleetIssueHandover   (changed: direction, confirmed)
DebtRecovery  [U,OHS]->[D,CF] CustomerSupport   : RecoveryPause         (added)
```

The full CML is in `files/src/main/cml/VeloCity-Context-Map.cml`. It has not been run through the Context Mapper generator yet. Run `VelocityContextMap` against it before replacing the original.

## Follow-ups in `velocity/velocity-context-map.md`

- Update both mermaid diagrams to match the relationships above.
- "No cycles" bullet: add that the Accounts ↔ Rentals direction was chosen to avoid the Settlement → Accounts → Rentals cycle.
- "Deferred / Open": remove the fleet-issue incident and bike-signalled fault items, which are now confirmed. Replace the dispute item with the open question on writing off disputed debt (#9).
