# Bounded context discovery: VeloCity (validation of the reference context map)

- **Date:** 2026-11-05
- **Mode:** validate
- **Status:** map approved (canvases declined by the user: "No canvases, please.")
- **Input:** `src/main/cml/VeloCity-Context-Map.cml` (trainer's reference solution, left untouched), `velocity/velocity-context-map.md`, `velocity/velocity-vision.md`
- **Context map:** proposed corrected version `src/main/cml/VeloCity-Context-Map-validated.cml` (new file, next to the original)

## Reading of the input
The original CML is the trainer's reference map: 8 internal contexts (Accounts, Rentals, Pricing, Settlement, Payments, DebtRecovery, CustomerSupport, FleetMaintenance), 2 external parties (PaymentProcessor, City) and 11 relationships, one of them marked `_Unconfirmed`. The narrative was rebuilt from the vision and the map doc as P1–P7. The expert regrouped it into four test processes: (1) Onboarding and activation, (2) Rent and return a bike, (3) Settle a ride and recover debt, (4) Bike fault to maintenance. The incident flow is not a test process. The expert's corrections: onboarding is strictly sequential; reservation expiry and unlock failure release the bike; a lock can also fail at a drop-zone return; the location decides only the fee; faults have three sources; every block sends a notification.

## Heuristic trail
Validation mode: the heuristics were run over the existing contexts, not over a new board.

### H1: Pivotal events
| Event | Status | Before | After | Expert's answer |
|---|---|---|---|---|
| Account Activated | confirmed (unchanged) | Accounts onboarding | Rentals may reserve/unlock | "is this account activated and not blocked?" |
| Rental Session Ended | confirmed (unchanged) | Rentals | Settlement (pricing, charging) | lock failure / max time also end it |
| Rental Fee Unpaid | confirmed (unchanged) | Settlement retries | Accounts block + DebtRecovery | retries and debt belong to settling |

### H2: Swimlanes
| Flow | Verdict | Why |
|---|---|---|
| Debt recovery (days, pause/resume) | lane (unchanged) | own clock and rules |
| Maintenance (follows a bike, not a ride) | lane (unchanged) | technician unlock never starts a session |
| Station vs drop-zone return | variant (unchanged) | only the fee differs |

### H3: Single responsibility
| Duplicated responsibility | Where it appeared | Resolution |
|---|---|---|
| "Charging" | Payments ("Charging") and Settlement ("Charging finished rides") | Wording only. The expert says Payments "runs a charge it's told to run" and has no rules; retries and debt belong to Settlement. Renamed to "Executing charges" and "Settling finished rides (charge and retry decisions)". The critic agreed. |
| Account blocking | Settlement (DebtBlocking events) and Payments (Payment Method Invalidated) both drove Accounts | Accounts is the single owner of "block / unblock with a reason"; others only supply the reason |
| Bike withdrawal | Rentals (on faults it detects) and FleetMaintenance (on support reports) | One capability (Rentals' generic withdraw), two triggers. The expert confirmed: "renting withdraws it right there" and "maintenance withdraws it, not support". |

Only Payments was flagged for a context critic. No other context showed a duplicated responsibility, straddled pivot or unowned decision, so no other context critics were run.

## Bounded contexts
| Context | Question it answers | Owns | Heuristic justification | Classification |
|---|---|---|---|---|
| Accounts | Can this person use VeloCity right now? | onboarding, activation, block/unblock with reason, block notifications | pivot Account Activated; H3 single owner of blocking | not given |
| Rentals | Who has which bike, and when? | availability, reserve/unlock, sessions, returns and locking, max-time end, fault withdrawal | pivot Rental Session Ended | not given |
| Pricing | What does this ride cost? | total fee, tariffs, zone fees, penalty | changes independently | not given |
| Settlement | Has this ride been paid for? | charge and retry decisions, balance, outstanding debt | pivot Rental Fee Unpaid | not given |
| Payments | Is this payment method usable, and did this charge go through? | verification, executing charges, detecting invalid cards | H3: no rules, shields the processor | not given |
| DebtRecovery | How do we get the owed money back? | reminders, payment plans, pause/resume | H2 lane | not given |
| CustomerSupport | Is this rider's issue resolved? | incident lifecycle | H2 lane | not given |
| FleetMaintenance | Is this bike fit to ride? | broken bikes, maintenance, back to circulation | H2 lane | not given |

## Critic log
| Subject | Round | Verdict | Blocking points / resolution |
|---|---|---|---|
| Payments (context) | 1 | AGREE | The "Charging" overlap is only wording. The note to rename the responsibilities was applied. The question "who marks a card invalid" was already answered by the expert: Payments. |
| Map, final pass over the 4 processes | 1 | AGREE | No blocking points. Notes applied: dropped PL on AccountBlocking, reworded FleetMaintenance, added a comment on the unblock path, removed a duplicate responsibility. The service-area question went to the expert (VeloCity's own, so no City → Rentals link). |

## Context map
### Findings on the original `VeloCity-Context-Map.cml`
Validator: `VALID: 10 bounded contexts, 11 relationships, 0 errors`, with 11 RULE lines.
1. **Indirect U/D cycle** Accounts → Rentals → Settlement → Accounts (relationship rule 3). `velocity-context-map.md` claims "No cycles".
2. **8 unjustified Conformists** (integration rule 1): City→Pricing, Payments→Accounts, Payments→Settlement, Rentals→Settlement, Rentals→FleetMaintenance, Pricing→Settlement, Settlement→Accounts, Settlement→DebtRecovery. The expert: "None of the internal ones was a deliberate choice on my part."
3. **No ACL downstream** (integration rule 3): Accounts→Rentals (no patterns at all), CustomerSupport→FleetMaintenance.
4. **Unconfirmed relationship** CustomerSupport→FleetMaintenance. Confirmed as a hand-off, but in the **reverse** direction: support tells maintenance, so FleetMaintenance offers the OHS.
5. The internal team relationship isn't stated. Customer/Supplier is the default for one company and one product (relationship rule 5).
6. No Shared Kernel, Partnership, Big Ball of Mud or Separate Ways was found.

### Process walkthroughs
#### (1) Onboarding and activation
| Step | Owning context | Hand-off / relationship |
|---|---|---|
| register … personal data | Accounts | – |
| payment method added → verified (pre-auth) | Accounts → Payments → PaymentProcessor | PaymentMethodVerification (Payments OHS), PaymentProcessing (ACL) |
| agreement signed → activated | Accounts | – |

#### (2) Rent and return a bike
| Step | Owning context | Hand-off / relationship |
|---|---|---|
| location in service area (VeloCity's own) → reserve → unlock | Rentals | AccountStatus (Accounts OHS → Rentals ACL: may this rider reserve/unlock?) |
| return, lock, lock failure, max-time end | Rentals | RentalSessionEnded (PL → Settlement ACL); lock failure → Bike Withdrawn (BikeAvailability) |
| fee by location / penalty / tariff at start | Pricing | ZoneMap (City → Pricing ACL), PriceCalculation |

#### (3) Settle a ride and recover debt
| Step | Owning context | Hand-off / relationship |
|---|---|---|
| total fee | Settlement → Pricing | PriceCalculation |
| charge + retries | Settlement → Payments | Charging (Payments OHS/PL → Settlement ACL) |
| unpaid → outstanding debt → block | Settlement → Accounts | AccountBlocking (Accounts OHS → Settlement ACL) |
| recovery, reminders, plans, pause/resume, collect | DebtRecovery → Settlement | DebtCollection (Settlement OHS/PL → DebtRecovery ACL) |
| paid → unblock | Settlement → Accounts | AccountBlocking |
| invalid card → block | Payments → Accounts | PaymentMethodVerification (Payment Method Invalidated PL → Accounts ACL) |

#### (4) Bike fault to maintenance
| Step | Owning context | Hand-off / relationship |
|---|---|---|
| fault at station return or self-signalled → withdraw, end ride | Rentals | BikeAvailability (Bike Withdrawn PL) |
| fault via support → broken | CustomerSupport → FleetMaintenance | FaultReporting (FleetMaintenance OHS → CustomerSupport ACL); maintenance then withdraws through BikeAvailability |
| technician reserve/unlock → maintenance → fixed → make available | FleetMaintenance → Rentals | BikeAvailability (Rentals OHS) |

### Relationships (proposed)
| Upstream | Downstream | Patterns | Messages | Justification (rule) |
|---|---|---|---|---|
| PaymentProcessor | Payments | U → ACL | verify, charge | external; integration rule 3 |
| City | Pricing | U → ACL | zone map | imposed, not a well-established language; integration rules 1 and 3 |
| Payments | Accounts | OHS,PL → ACL | verify; Payment Method Invalidated | expert R2 Q1; avoids a direct cycle |
| Payments | Settlement | OHS,PL → ACL | execute charge → succeeded/failed | integration rules 3–5 |
| Accounts | Rentals | OHS → ACL | is the account active? | expert Q4 |
| Accounts | Settlement | OHS → ACL | block/unblock, reason: outstanding debt | expert Q2; breaks the cycle |
| Rentals | Settlement | PL → ACL | Rental Session Ended | integration rule 5 |
| Rentals | FleetMaintenance | OHS,PL → ACL | Bike Withdrawn; withdraw/make available; reserve/unlock | expert R2 Q2/Q3 |
| Pricing | Settlement | OHS → ACL | price for session | integration rule 4 |
| Settlement | DebtRecovery | OHS,PL → ACL | Rental Fee Unpaid; collect | integration rules 4–5 |
| FleetMaintenance | CustomerSupport | OHS → ACL | report faulty bike | expert Q5 ("support tells maintenance") |

All internal links are Customer/Supplier in team terms. CML forbids OHS in C/S, so they're written as U/D, and the CML header says so.

### Cycle-breaking attempts (original indirect cycle Accounts → Rentals → Settlement → Accounts)
- Flip Accounts → Rentals: rejected. "Is the account active?" is "a general question anyone could ask", so Accounts is upstream.
- Flip Rentals → Settlement: rejected. Settlement must understand Rental Session Ended, and Rentals knows nothing about settling.
- Flip Settlement → Accounts: **accepted**. Accounts offers "block / unblock with a reason" and "whoever knows the reason calls it".
- Side effect: if Payments also called that block API, Payments ↔ Accounts would become a direct cycle. Resolved by having Payments publish Payment Method Invalidated, which Accounts' ACL translates (expert: "Yes, that works").

### Validator output
Original:
```
RULE conformist-check x8 (listed above)
RULE no-acl: Rentals is downstream of Accounts without an ACL.
RULE no-acl: FleetMaintenance is downstream of CustomerSupport without an ACL.
RULE ud-cycle-indirect: Accounts -> Rentals -> Settlement -> Accounts.
VALID: 10 bounded contexts, 11 relationships, 0 errors
```
Proposed (last run):
```
RELATIONSHIPS:
  PaymentProcessor [] -> Payments [ACL] : PaymentProcessing
  City [] -> Pricing [ACL] : ZoneMap
  Payments [OHS, PL] -> Accounts [ACL] : PaymentMethodVerification
  Payments [OHS, PL] -> Settlement [ACL] : Charging
  Accounts [OHS] -> Rentals [ACL] : AccountStatus
  Accounts [OHS] -> Settlement [ACL] : AccountBlocking
  Rentals [PL] -> Settlement [ACL] : RentalSessionEnded
  Rentals [OHS, PL] -> FleetMaintenance [ACL] : BikeAvailability
  Pricing [OHS] -> Settlement [ACL] : PriceCalculation
  Settlement [OHS, PL] -> DebtRecovery [ACL] : DebtCollection
  FleetMaintenance [OHS] -> CustomerSupport [ACL] : FaultReporting
VALID: 10 bounded contexts, 11 relationships, 0 errors
```
No RULE lines. Every original RULE line was fixed: the CFs became ACLs, ACLs were added, and the cycle was flipped.

## Q&A with the domain expert
### Round 1
**Facilitator:**

Thanks. Since you wrote this as the trainer's reference solution, I'll treat this as a **validation** (review only). I won't touch `src/main/cml/VeloCity-Context-Map.cml`. Any corrections go into a separate proposal file and the report.

**What I ran:** the CML parses cleanly (10 contexts, 11 relationships, 0 errors). The rule checker raised 11 `RULE` lines: 8 `conformist-check`, 2 `no-acl` and 1 `ud-cycle-indirect`.

**How I read the narrative the map has to support** (taken from `velocity-vision.md` and `velocity-context-map.md`; please confirm):
- P1 **Onboarding:** register → ... → payment method verified (Payments, via the processor) → account activated (Accounts).
- P2 **Rent a bike:** location validated → reserve → unlock. Rentals checks with Accounts that the account is active. Then the session starts.
- P3 **Return and settle:** a station or drop-zone return, or the max-time forced end → Rental Session Ended → Settlement asks Pricing for the total (using the City zone map) → Settlement charges through Payments, with retries → the ride is settled, or Rental Fee Unpaid → Accounts blocks and Debt Recovery starts.
- P4 **Debt recovery:** reminders, payment plans, pause/resume. Debt Recovery asks Settlement to collect → Outstanding Debt Cleared → Accounts unblocks.
- P5 **Invalid card:** Payments → Payment Method Invalidated → Accounts blocks.
- P6 **Broken bike and maintenance:** fault detected → Rentals withdraws the bike → Fleet Maintenance; the technician reserves and unlocks through Rentals → maintenance → fixed → Fleet Maintenance makes the bike available.
- P7 **Incident:** Customer Support reports → types → assigns → accepts/rejects → resolves. A fleet issue goes to Fleet Maintenance (marked unconfirmed in the map). A billing dispute pauses Debt Recovery (deferred in the doc).

**Preliminary findings** (not final yet; the critic review comes after your answers):
- **An indirect U/D cycle:** Accounts → Rentals → Settlement → Accounts. `velocity-context-map.md` says "No cycles", so the map and the doc disagree here. The candidate flip is DebtBlocking: Accounts offers an OHS ("block/unblock for reason X"), which would make Accounts upstream of Settlement.
- **8 Conformists between internal contexts.** Our rule allows CF only towards a well-established language, and none of them is justified in the file. The ACL / Customer-Supplier default doesn't appear anywhere.
- **No ACL downstream** on Accounts → Rentals (no patterns at all) and on CustomerSupport → FleetMaintenance.
- **An unconfirmed relationship:** CustomerSupport → FleetMaintenance (`_Unconfirmed`).
- **Possible H3 overlap:** both Payments and Settlement list "Charging" as a responsibility.

**Questions** (each one decides a relationship or a boundary):
1. Are P1–P7 the full set of processes the map must support? Anything missing or wrong? *(This is the test bench for every relationship.)*
2. **Account blocking language:** when a rider owes money, does Accounts have to understand Settlement's events (Rental Fee Unpaid / Outstanding Debt Cleared)? Or does Accounts define its own "block/unblock with reason" API that Settlement (and Payments) call? *(This decides whether the Accounts → Rentals → Settlement → Accounts cycle can be broken.)*
3. **The Conformists:** are all eight teams internal and working towards the same product goal? Did you mean any CF deliberately? For example, is Payments' language just the processor's standard, or is the City zone map an established format? If not, should they be ACL downstream (and Customer/Supplier)? *(This decides 8 relationship patterns.)*
4. **Accounts → Rentals (AccountStatus):** is "is the account active?" an API Accounts offers to anyone (OHS), with Rentals translating it into its own eligibility check (ACL)?
5. **Fleet-issue incidents:** when Support classifies an incident as a fleet issue, who decides the bike is broken? And how does that reach Fleet Maintenance: Support calls a Fleet Maintenance API, or Fleet Maintenance reacts to a Support event? Should billing disputes → pause Debt Recovery be on the map? *(This confirms, changes or removes the unconfirmed relationship, and decides whether Customer Support is isolated.)*
6. **Charging:** does Payments have any rules of its own about charging (when, how often, retries), or does it just execute what Settlement tells it? *(H3: who owns the charging decision.)*
7. Do you want the corrected proposal as a new file, `src/main/cml/VeloCity-Context-Map-validated.cml`, alongside your original?

**Expert:**

**1. Processes.** I want the map checked against four processes:
(1) Onboarding and activation, (2) Rent and return a bike, (3) Settle a ride and recover debt, (4) Bike fault to maintenance.
Your P1–P6 fit inside those: P1 = (1); P2 plus the return half of P3 = (2); the settlement half of P3, P4 and P5 = (3); P6 = (4). The incident flow (P7) isn't one of the processes I'm asking you to check against. Both of its links are still open questions in my doc.
Corrections to the narrative:
- Onboarding is strictly sequential. The steps are register → email confirmed → phone verified → personal data → payment method added → verified (small pre-authorization) → agreement signed → activated. A rider who drops out stays incomplete and can resume later.
- Before a session starts, the reservation can expire, or the unlock can fail. In both cases the reservation is released and the bike goes back to the pool.
- A lock can fail at a station return and also at a drop-zone return. When it fails, the session ends immediately, the rider is told why, no extra charge is added, and the bike counts as broken.
- Every return ends the ride. The location only decides the fee: drop zone fee or unauthorized zone return fee. When the max time is hit, the session ends automatically and a penalty is added. The rider always pays the prices that were valid when the ride started.
- Faults come from three places: a rider reporting one at a station return, the bike or station signalling one itself (a failed lock or a mechanical fault), or a rider reporting it to support. If a technician's reservation expires or their unlock fails, the bike stays broken and is never offered to riders. A technician's unlock starts maintenance, never a rental session. Maintenance is never billed.
- Every block sends a block notification to the rider.

**2. Account blocking.** Accounts doesn't need to know anything about rides, fees or retries. To the account, a block is just "blocked, reason: outstanding debt" or "blocked, reason: invalid card". It is lifted when that reason is cleared. So I pick your second option: Accounts has its own "block / unblock with a reason", and whoever knows the reason calls it. For debt that's whoever knows the debt is paid. For a card it's whoever knows the card is invalid.

**3. The eight teams.** All VeloCity teams are in one company and work towards the same product. They can talk to each other and negotiate. None of the internal ones was a deliberate choice on my part.
Only two parties are outside our control:
- **The payment processor** has its own terms. VeloCity only cares whether a payment method is verified or not, and whether a charge succeeded or failed.
- **The City** defines the zone map: stations, drop zones, and everything else as unauthorized zone. We take it as the City gives it and can't change it.
For the internal ones, go with your default.

**4. "Is the account active?"** Yes. It's a general question anyone could ask: is this account activated and not blocked? Today only renting asks it. What renting actually needs to know is "may this rider reserve and unlock?", and that applies only to riders. Technicians don't go through onboarding.

**5. Fleet-issue incidents.**
- **Who decides it's broken:** the support agent. When the agent accepts an incident as a fleet issue, the bike counts as broken right away. No technician confirms it first, the same as any other fault report.
- **How it reaches the technicians:** support hands it over to them. That's an escalation to a specialized team. Of your two options, I pick the first: support tells maintenance, rather than maintenance watching support.
- **Billing disputes:** a dispute under review is a legitimate reason to pause debt recovery, and recovery resumes once the dispute is settled. I deliberately left it out of the reference. Keep it deferred, and list it as open in your report.

**6. Charging.** The payment side has no rules of its own about when to charge or how often. It verifies a payment method with a pre-authorization, and it runs a charge it's told to run, which either succeeds or fails. The "try again a few times shortly after the ride" rule belongs to settling the ride. So does turning the unpaid amount into outstanding debt.

**7. File.** Yes. Write it as `src/main/cml/VeloCity-Context-Map-validated.cml` next to the original, and leave `VeloCity-Context-Map.cml` untouched. No canvases, please.

### Round 2
**Facilitator:**

Thanks, that settles most of it. Here's what your answers changed:
- **Cycle broken:** Accounts now offers "block / unblock with a reason" (OHS). Settlement calls it for outstanding debt, so **Accounts is upstream of Settlement**. That removes Accounts → Rentals → Settlement → Accounts.
- **All eight internal CFs become ACLs** (internal teams, no well-established language). PaymentProcessor → Payments stays ACL. City → Pricing becomes ACL too, because "we can't change it" is a power fact, not a well-established language, so Pricing should protect itself from it.
- **Accounts → Rentals:** Accounts OHS ("is the account active?"), and Rentals ACL translating it into "may this rider reserve and unlock?".
- **Payments/Settlement:** no overlap. Payments executes; Settlement owns the retries and the debt. I'll rename Payments' responsibility to "Executing charges" so it doesn't read like a duplicate.
- **Fleet-issue incident:** Support tells Maintenance, so Fleet Maintenance offers a fault-reporting OHS and **Customer Support is downstream** of it (ACL). That's the reverse of the unconfirmed arrow in your file. Billing disputes stay deferred and listed as open.

Three follow-ups, each needed to avoid a new cycle or a guessed hand-off:
1. **Invalid card:** Accounts already calls Payments to verify the card (Payments upstream). If Payments also *called* Accounts' block API, the two would be upstream of each other, which is a direct cycle. Is it OK for Payments to *publish* "Payment Method Invalidated", and for Accounts to translate it in its ACL into "block, reason: invalid card"? Accounts still owns the block either way. *(This decides the Payments ↔ Accounts direction.)*
2. **Faults the bike or station signals itself** (a failed lock, a mechanical fault): do they reach Rentals first, since it owns locks and stations, so that Rentals withdraws the bike and publishes the fault to Fleet Maintenance? Or do they go straight to Fleet Maintenance? *(Your reference lists this as unconfirmed.)*
3. **When Support accepts a fleet issue**, does Fleet Maintenance then tell Rentals to withdraw that bike (through Rentals' generic "withdraw"), or does Support do that? *(This decides whether Customer Support needs a link to Rentals.)*

**Expert:**

Answers to your three follow-ups: invalid card is announced, self-signalled faults go to renting first, and maintenance withdraws a bike that support reports.

1. Invalid card: yes, that works. The payment side is the first to find out that a card is no longer usable: it expired, was cancelled or was reported lost. It announces that. Placing the block and sending the block notification stay with the account. To the account, it's simply "blocked, reason: invalid card", lifted once the rider has a usable card again.

2. Faults the bike or station signals itself: they go to renting first. A failed lock or a fault detected at the station happens while a bike is being locked or docked, and renting runs locking and stations. The bike has to stop being offered to riders immediately, so renting withdraws it right there. If a ride is in progress, renting also ends it, with no extra charge. Then the technicians are told the bike is broken.

3. Fleet issue accepted by support: maintenance withdraws it, not support. Support's job ends with "this bike is faulty, over to the technicians". The technicians' side decides what happens to the bike: it takes the bike out of circulation and later puts it back once it's fixed. Support never deals with whether a bike is available.

### Round 3
**Facilitator:**

Here's the corrected proposal: `src/main/cml/VeloCity-Context-Map-validated.cml`. Your original is untouched. It's valid, with 0 errors, 11 relationships and **no RULE lines left**. An independent critic walked your four processes through it and agreed.

| Upstream | Downstream | Patterns | What flows | Why |
|---|---|---|---|---|
| PaymentProcessor | Payments | U → ACL | verify, charge | external, shield VeloCity |
| City | Pricing | U → ACL | zone map | imposed, not a well-established language (was CF) |
| Payments | Accounts | OHS,PL → ACL | verify; Payment Method Invalidated → block (invalid card) | your Q1 answer; avoids a Payments ↔ Accounts cycle (was CF) |
| Payments | Settlement | OHS,PL → ACL | execute charge → succeeded/failed | internal, no CF (was CF) |
| Accounts | Rentals | OHS → ACL | is the account active? → may this rider reserve/unlock? | your answer 4 (was plain U/D) |
| Accounts | Settlement | OHS → ACL | block / unblock, reason: outstanding debt | **flipped**: breaks the Accounts → Rentals → Settlement → Accounts cycle |
| Rentals | Settlement | PL → ACL | Rental Session Ended | (was CF) |
| Rentals | FleetMaintenance | OHS,PL → ACL | Bike Withdrawn; withdraw / make available; reserve / unlock | (was CF) |
| Pricing | Settlement | OHS → ACL | price for session | (was CF) |
| Settlement | DebtRecovery | OHS,PL → ACL | Rental Fee Unpaid; collect | (was CF) |
| FleetMaintenance | CustomerSupport | OHS → ACL | report faulty bike | **reversed and confirmed** (was `_Unconfirmed`) |

All internal links are Customer/Supplier in team terms. They're written as U/D because CML doesn't allow OHS in C/S, and the file's header says so.

Cycle flips I tried: Accounts → Rentals (rejected: it's a general question anyone asks); Rentals → Settlement (rejected: Settlement depends on Rentals' language); Settlement → Accounts (accepted: your blocking answer).

**For your workshop doc:** `velocity-context-map.md` says "No cycles". The original CML has one, and it uses CF everywhere, which the rules only allow towards a well-established language.

Still open: billing disputes → pause Debt Recovery (deferred, as you asked).

1. One critic question: is the **service area** that Rentals checks at reservation VeloCity's own, or part of the City's zone map? *(If it's the City's, Rentals also needs a City → Rentals ACL link.)*
2. Do you approve this proposed map?

**Expert:**

The service area belongs to VeloCity, not to the City, and I approve the proposed map.

1. Service area: VeloCity decides it. It's the overall region where we operate, and it decides where a rider may pick up a bike. The City's map is only about where bikes may come back: stations, drop zones, and everything else counts as an unauthorized zone. That map matters when a ride ends, and only for the fee. When a rider reserves a bike, we only check that they're inside our service area and standing near a bike.

2. Approved. Every hand-off in it matches how the business works, and it covers all four processes. Keep billing disputes, the pausing of debt recovery, listed as open. I'll fix the "No cycles" claim in my workshop doc myself.

## Open questions and assumptions
- **Billing disputes → pause / resume Debt Recovery** (from Customer Support): deliberately deferred by the expert. Not modelled; noted in a CML comment.
- No unconfirmed relationships remain in the proposed map.

## Suggested board changes
- Add the Payments events Payment Method Verified, Payment Method Invalidated, Charge Succeeded and Charge Failed, and the Settlement events Rental Settled and Rental Fee Unpaid.
- Add Bike Withdrawn (Rentals) and Faulty Bike Reported (support → maintenance).
- `velocity-context-map.md`: the "No cycles" claim is false for the original CML. The expert will fix it.

## Next steps
- Canvases: declined by the user for now ("No canvases, please.").
- Optionally, the trainer can sync `velocity-context-map.md` with the validated proposal, or adopt it as the new reference.
