# Transcript

## Message to user 1

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

## Expert reply 1

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

## Critic verdicts (round A)

- Payments (context), round 1: AGREE. The Charging overlap is only wording; Payments has no rules. Note: rename the responsibilities; asked who marks a card invalid (answered by expert: Payments).

## Message to user 2

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

## Expert reply 2 (delivered first via message)

Answers to your three follow-ups: invalid card is announced, self-signalled faults go to renting first, and maintenance withdraws a bike that support reports.

1. Invalid card: yes, that works. The payment side is the first to find out that a card is no longer usable: it expired, was cancelled or was reported lost. It announces that. Placing the block and sending the block notification stay with the account. To the account, it's simply "blocked, reason: invalid card", lifted once the rider has a usable card again.

2. Faults the bike or station signals itself: they go to renting first. A failed lock or a fault detected at the station happens while a bike is being locked or docked, and renting runs locking and stations. The bike has to stop being offered to riders immediately, so renting withdraws it right there. If a ride is in progress, renting also ends it, with no extra charge. Then the technicians are told the bike is broken.

3. Fleet issue accepted by support: maintenance withdraws it, not support. Support's job ends with "this bike is faulty, over to the technicians". The technicians' side decides what happens to the bike: it takes the bike out of circulation and later puts it back once it's fixed. Support never deals with whether a bike is available.

## Expert reply 2 (same answer, re-delivered as hand-back)

**1. Invalid card.** Yes, that's fine. The payment side is the first to find out a card can't be used any more: it expired, was cancelled or was reported lost. It announces that. The account side still places the block ("blocked, reason: invalid card") and sends the rider the block notification. The block is lifted once the rider has a usable card again.

**2. Faults the bike or station signals itself.** They go to renting first. A failed lock, or a fault detected at the station, happens while the bike is being locked or docked, and renting runs locking and stations. The bike must stop being offered to riders immediately, so renting takes it out of circulation right there. If a ride is in progress, renting also ends it, with no extra charge. Then the technicians are told the bike is broken.

**3. A fleet issue accepted by support.** Maintenance takes the bike out of circulation, not support. Support's part ends with "this bike is faulty, over to the technicians". The technicians' side decides what happens to the bike: it takes the bike out of circulation and puts it back once it's fixed. Support never deals with whether a bike is available.

## Critic verdicts (round B)

- Map final pass over all 4 processes, round 1: AGREE. DAG, all RULE lines resolved, cycle flips adequate. Non-blocking: drop PL on AccountBlocking, reword FleetMaintenance, asked whether the service area is the City's (sent to expert).

## Message to user 3

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

## Expert reply 3 (delivered first via message)

The service area belongs to VeloCity, not to the City, and I approve the proposed map.

1. Service area: VeloCity decides it. It's the overall region where we operate, and it decides where a rider may pick up a bike. The City's map is only about where bikes may come back: stations, drop zones, and everything else counts as an unauthorized zone. That map matters when a ride ends, and only for the fee. When a rider reserves a bike, we only check that they're inside our service area and standing near a bike.

2. Approved. Every hand-off in it matches how the business works, and it covers all four processes. Keep billing disputes, the pausing of debt recovery, listed as open. I'll fix the "No cycles" claim in my workshop doc myself.

## Expert reply 3 (same answer, re-delivered as hand-back)

**1. Service area.** VeloCity decides it, not the City. It's the overall region where we operate, and it decides where a rider may pick up a bike. The City's map is only about where bikes may come back: stations, drop zones, and everything else counts as an unauthorized zone. That map matters when a ride ends, and only for the fee. When a rider reserves a bike, we only check that they're inside our service area and standing near a bike.

**2. Approval.** Approved. Every hand-off in your proposal matches how the business works, and it covers all four processes. Keep billing disputes (pausing debt recovery) listed as open. I'll fix the "No cycles" claim in my workshop doc myself.

## Final message to user

Done. Your reference map parses cleanly, but it breaks three of our context-mapping rules. Your original `src/main/cml/VeloCity-Context-Map.cml` is untouched.

**What the original breaks:**
- **An indirect U/D cycle:** Accounts → Rentals → Settlement → Accounts. That also contradicts the "No cycles" line in `velocity-context-map.md`, which you said you'll fix.
- **8 Conformists that aren't justified**, all of them between internal teams with no well-established language.
- **2 downstreams with no ACL:** Accounts → Rentals, and CustomerSupport → FleetMaintenance.
- **1 unconfirmed relationship.** It turns out to point the wrong way: Fleet Maintenance offers fault reporting, and Customer Support uses it.

**Proposed fix** (a new file; it validates with 0 errors and no rule warnings):
- `src/main/cml/VeloCity-Context-Map-validated.cml`: 8 contexts, 2 external parties, 11 relationships. Every downstream now uses an ACL.
- Report: `docs/context-discovery/velocity/2026-11-05-validate-reference-map/discovery.md`

**The changes that mattered:**
- **Cycle broken.** Accounts now offers "block / unblock with a reason", and Settlement calls it. To keep Payments from forming a new cycle with Accounts, Payments publishes "Payment Method Invalidated" and Accounts translates it into a block.
- **Payments / Settlement "Charging" overlap:** a critic agreed it's only wording, since Payments has no rules of its own. I renamed the responsibilities.
- **Final map critic:** it agreed. Its notes tidied the CML, and its service-area question confirmed that Rentals needs no link to the City.

**Still open:** billing disputes pausing Debt Recovery. You deferred this on purpose, so it's only a comment in the CML.

No canvases, as you asked.
