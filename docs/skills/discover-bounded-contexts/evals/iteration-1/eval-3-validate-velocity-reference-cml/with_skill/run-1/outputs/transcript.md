# Transcript

## User (initial task)

"can you sanity-check the VeloCity context map in src/main/cml/VeloCity-Context-Map.cml? I wrote it as the reference solution for the workshop and I want to know if it breaks any of our context mapping rules before I show it to the group."

## Facilitator -> user (message 1)

Validate mode it is. You wrote this as the reference solution, so I'll treat you as the trainer and lean on `velocity/velocity-context-map.md` and the vision freely. I won't touch `src/main/cml/VeloCity-Context-Map.cml`; anything I propose goes into a separate file.

**How I read the map**
- 8 internal contexts (Accounts, Rentals, Pricing, Settlement, Payments, DebtRecovery, CustomerSupport, FleetMaintenance) and 2 external parties (PaymentProcessor, City), 11 relationships, all Upstream/Downstream. No Shared Kernel, no Partnership, no Customer/Supplier.
- The CML parses cleanly (validator: 10 contexts, 11 relationships, 0 errors). It raises 11 `RULE` smells:
  - **1 indirect U/D cycle:** Accounts -> Rentals -> Settlement -> Accounts.
  - **8 Conformists to justify:** Pricing CF City, Accounts CF Payments, Settlement CF Payments, Settlement CF Rentals, FleetMaintenance CF Rentals, Settlement CF Pricing, Accounts CF Settlement, DebtRecovery CF Settlement. Under our rules CF is only OK towards a well-established language; 7 of the 8 are towards internal, young models.
  - **2 downstreams with neither ACL nor CF:** Rentals (from Accounts, `AccountStatus`) and FleetMaintenance (from CustomerSupport, the `_Unconfirmed` one).

**The processes I'll walk the map against** (derived from the vision; please confirm or correct):
- P1 Onboarding: register ... add payment method -> payment method verified (pre-authorization) -> account activated. Plus later: card becomes invalid -> account blocked.
- P2 Rent a bike: eligibility (account active, not blocked) -> reserve -> unlock -> session started -> station/drop-zone return or forced end at max rental time -> Rental Session Ended.
- P3 Settle the ride: Rental Session Ended -> total fee calculated (tariff at session start, penalty, zone fees with the City's zone map) -> charge -> retries -> settled, or Rental Fee Unpaid -> outstanding debt -> account blocked.
- P4 Debt recovery: Rental Fee Unpaid -> recovery started -> reminders / payment plan / pause / resume -> collection through Settlement -> Outstanding Debt Cleared -> account unblocked, or recovery exhausted.
- P5 Broken bike & maintenance: fault detected (lock failed, fault at return, bike signal) -> bike withdrawn -> technician reserve/unlock -> maintenance started -> fixed -> bike made available.
- P6 Incident: reported -> type detected -> assigned -> accepted/rejected -> resolved/escalated/reopened; fleet issues lead to the bike being marked broken.

**Questions** (each one decides a relationship or a rule finding):
1. Are P1-P6 the right test bench? Anything missing or out of scope for this map (for example billing disputes pausing debt recovery, which your md lists as deferred)?
2. *The cycle.* For account blocking, whose language is it? (a) Accounts understands Settlement's facts (`Rental Fee Unpaid`, `Outstanding Debt Cleared`), as the map draws it now, or (b) Accounts offers its own "block / lift block with a reason" service, and Settlement (and Payments for invalid card) call it in Accounts' language? (b) would make Accounts upstream and break the cycle; (a) keeps it.
3. *Teams.* Are the internal contexts owned by different teams of the same organisation? If yes, our rules make Customer/Supplier the default flavour of U/D between them, and the map currently has none.
4. *Conformists towards internal contexts.* Were the CFs a deliberate choice (for example "Payments' language is a generic payments vocabulary everyone can adopt"), or just the default you drew? In particular: should Settlement and FleetMaintenance adopt Rentals' model as-is, and should Accounts adopt Settlement's debt language?
5. *City zone map.* Is it a stable, standard format published by the city (something like GeoJSON) that Pricing can conform to, or the city's own ad-hoc model?
6. *Accounts -> Rentals.* Does Rentals translate "is the account active?" into its own eligibility notion, or use Accounts' status as-is?
7. *CustomerSupport -> FleetMaintenance.* Is the fleet-issue incident route confirmed now? And do faults Rentals detects itself also open an incident in Customer Support, or do they only go to Fleet Maintenance?
8. If I propose a corrected map, is a new file `src/main/cml/VeloCity-Context-Map-validated.cml` fine?

## Expert reply 1

Validate mode works for me. One correction to how you're framing it: the reference map is a default I drew, not a set of deliberate decisions. Wherever I say "default" below, challenge it.

**1. Test bench.** I check the map against four processes, not six:
- **Onboarding and activation.** Your P1. It includes the later "card becomes invalid -> account blocked".
- **Rent and return a bike.** Your P2. It covers station and drop-zone returns, the forced end at max rental time, and a locking failure ending the session.
- **Settle a ride and recover debt.** Your P3 and P4 are one process for me. Retries, Rental Fee Unpaid, block, recovery, Outstanding Debt Cleared and unblock all form one chain.
- **Bike fault to maintenance.** Your P5. It has three entry points: a fault Rentals detects (lock failed, or a fault at return), a signal from the bike itself, and a fleet-issue incident from Customer Support.

The full incident lifecycle (your P6: accept, reject, escalate, reopen) is internal to Customer Support. For the map, only its fleet-issue exit matters, and that belongs to "fault to maintenance". Billing disputes pausing debt recovery are out of scope and stay deferred. Don't add a Customer Support -> Debt Recovery relationship.

**2. The cycle.** It's (b). An account block is Accounts' concept: "blocked, with a reason", and "lift the block for this reason". Accounts also sends the block notification. "The debt is cleared" is Settlement's fact, but deciding to block or unblock is Accounts' business. So Settlement should call Accounts' own "block / lift block (reason: outstanding debt)" service in Accounts' language. That makes Accounts upstream of Settlement and breaks the cycle. My "no cycles" check only looked at direct pairs, so I missed this indirect one.

For the invalid card, don't flip the relationship. Accounts already calls Payments to verify the payment method. If Payments also called Accounts, you'd get a direct two-way relationship. Keep Payments upstream: it publishes Payment Method Invalidated, and Accounts reacts to it by blocking.

**3. Teams.** Yes. All VeloCity contexts are owned by different teams in one company. Only the Payment Processor and the City are external. Customer/Supplier is a fair default between the internal contexts. The current map doesn't have one only because I didn't think about team relationships when I drew it.

**4. Conformists.** Mostly they're the default I drew, not choices.
- **Payments:** this one is deliberate. Payments is a thin, generic payments vocabulary ("payment method verified or not", "charge succeeded or failed") that shields us from the processor. Conforming to it is fine, and I'd defend that as a well-established language.
- **Settlement from Rentals:** no. Settlement doesn't care about reservations, unlocks or locks. It needs "a ride to settle": start, end, return location and end reason. Settlement should translate.
- **Fleet Maintenance from Rentals:** definitely no. Rentals deliberately speaks *available / withdrawn*, never *broken*, while Fleet Maintenance speaks *broken / maintenance / fixed / back in circulation*. That's a language difference by design, so Fleet Maintenance needs an ACL.
- **Accounts from Settlement:** no. With (b) the question disappears anyway, since Settlement uses Accounts' language.
- **Pricing and Settlement:** Pricing is a pure calculator called by Settlement. Settlement uses "the price for this session" in its own terms, so I wouldn't insist on a Conformist there either.

**5. City zone map.** It's the city's own data, and we don't control it. The city decides where stations and drop zones are and publishes the map in whatever form its systems produce. I can't promise it's a stable standard format, and it changes when the city changes its rules or systems. Pricing should translate it into its own notions (station, drop zone, unauthorized zone) rather than conform. If I had to choose: ACL.

**6. Accounts -> Rentals.** Rentals has its own notion: "this rider may rent". That's more than the account status, because Rentals also validates location (inside the service area, near a bike). Rentals asks Accounts whether the account is active and not blocked, and turns the answer into its own eligibility. So it translates (ACL), and Accounts doesn't leak its account states (incomplete, activated, blocked) into Rentals.

**7. Customer Support -> Fleet Maintenance.**
- **Fleet-issue incidents:** confirmed. When an incident is about a faulty bike, it goes to Fleet Maintenance, which marks the bike broken and asks Rentals to withdraw it. Drop the "_Unconfirmed" marker. Customer Support publishes the fleet issue in its own terms (incident, bike, description). Fleet Maintenance turns it into "broken bike", so that's an ACL on Fleet Maintenance's side.
- **Faults Rentals detects** (lock failed, fault at return): these do **not** open an incident in Customer Support. Rentals withdraws the bike, and they go only to Fleet Maintenance. Incidents are things riders report to support agents.
- **Faults the bike signals itself:** confirmed that Rentals receives them, since it owns locks and stations. They follow the same Fleet Maintenance-only route.

**8. New file.** Yes, put the corrected map in `src/main/cml/VeloCity-Context-Map-validated.cml` and leave `VeloCity-Context-Map.cml` untouched. Alongside the CML, write the findings: what changed and why, mapped to the rule each change answers. No canvases needed.

## Critic verdicts (between expert reply 1 and message 2)

- Accounts, round 1, AGREE: one question, blocking owned once; note on lifting an invalid-card block.
- Rentals, round 1, AGREE: covers availability/sessions/returns; suggests explicit Bike Withdrawn / end-reason events.
- Pricing, round 1, AGREE: pure calculator; non-blocking question on live estimate; City should be ACL.
- Settlement, round 1, AGREE: owns far side of Rental Session Ended; flags Accounts should be upstream for blocking.
- Payments, round 1, AGREE: purely mechanical shield; CF justified.
- DebtRecovery, round 1, AGREE: own lane; non-blocking questions on exhausted recovery and payment plans.
- CustomerSupport, round 1, AGREE: own lane; drop `_Unconfirmed`, add ACL.
- FleetMaintenance, round 1, AGREE: own lane; ACL on both inbound relationships.
- Map + Onboarding and activation, round 1, AGREE: every hand-off covered; question on payment-method token.
- Map + Rent and return a bike, round 1, AGREE: no cycle; comment refinements for PriceCalculation inputs.
- Map + Settle a ride and recover debt, round 1, AGREE: chain covered, cycle gone; questions on exhausted recovery and payment plans.
- Map + Bike fault to maintenance, round 1, AGREE: three entry points covered; technician-unlock comment suggested.
- Map + all processes (final pass), round 2, AGREE: no cycle, all RULE/WARNING lines justified; note on zone classification ownership.

## Facilitator -> user (message 2)

Thanks. Your answers settled every finding. Here's the result. Your original `VeloCity-Context-Map.cml` is untouched.

**The contexts hold up.** I ran all 8 internal contexts past an independent critic, and each one got AGREE in round 1. Each answers one question that no other context answers, every confirmed pivot has an owner on both sides, and no decision has two owners.

**What the map breaks, by rule:**
1. **Indirect U/D cycle** (relationship rule 3): Accounts -> Rentals -> Settlement -> Accounts. Per your answer 2(b), blocking is Accounts' language, so Settlement calls Accounts' "block / lift block (reason: outstanding debt)" service. Accounts becomes upstream of Settlement (`AccountBlocking`) and the cycle disappears. Payments stays upstream of Accounts for Payment Method Invalidated, as you said.
2. **Unjustified Conformists** (integration rule 1): 6 of the 8 CFs become ACLs:
   - Pricing from City (the zone map is the city's own, changing format)
   - Settlement from Rentals ("a ride to settle")
   - FleetMaintenance from Rentals ("available/withdrawn" vs "broken")
   - Settlement from Pricing
   - DebtRecovery from Settlement
   - Accounts from Settlement (this one disappears with fix 1)

   The 2 CFs to Payments stay, justified in the CML as a deliberate, thin, generic payments vocabulary.
3. **Downstreams with neither ACL nor CF** (integration rule 3):
   - Rentals from Accounts now has an ACL. Rentals' own "this rider may rent" also includes location.
   - CustomerSupport -> FleetMaintenance is now confirmed: `_Unconfirmed` is dropped and FleetMaintenance gets an ACL.
4. **No Customer/Supplier between internal teams** (relationship rule 5). Rentals -> Settlement, Settlement -> DebtRecovery and CustomerSupport -> FleetMaintenance become C/S. Payments, Accounts, Rentals' availability API and Pricing serve several clients, so they stay U/D with OHS/PL. CML doesn't allow OHS in a C/S relationship.

**Proposed map:** `src/main/cml/VeloCity-Context-Map-validated.cml`

| Upstream | Downstream | Patterns | Messages |
|---|---|---|---|
| PaymentProcessor | Payments | U / D,ACL | charge, pre-authorization |
| City | Pricing | U / D,ACL | zone map |
| Payments | Accounts | U,OHS,PL / D,CF | verify payment method; Payment Method Invalidated |
| Payments | Settlement | U,OHS,PL / D,CF | charge; succeeded/failed |
| Accounts | Rentals | U,OHS,PL / D,ACL | account active and not blocked? |
| Accounts | Settlement | U,OHS,PL / D,ACL | block / lift block (reason: outstanding debt) |
| Rentals | Settlement | U,S,PL / D,C,ACL | Rental Session Ended (start, end, location, end reason) |
| Rentals | FleetMaintenance | U,OHS,PL / D,ACL | Bike Withdrawn, Technician Unlocked; withdraw / make available, reserve / unlock |
| Pricing | Settlement | U,OHS,PL / D,ACL | price for session |
| Settlement | DebtRecovery | U,S,PL / D,C,ACL | Rental Fee Unpaid, Outstanding Debt Cleared; collect |
| CustomerSupport | FleetMaintenance | U,S,PL / D,C,ACL | Fleet Issue Reported |

**Checks on the proposed map:**
- **Validator:** 0 errors, no cycle. Two `conformist-check` lines remain (the Payments CFs, justified). Context Mapper also gives three "ACL in Customer/Supplier" warnings, which I kept on purpose, because our rules make ACL the downstream default.
- **Critic:** each of your four processes got AGREE in round 1, and the final pass over all processes got AGREE in round 2. No round changed a relationship. They only refined comments.

**Open questions.** None of these can reverse an arrow. Answer them if you like, or I'll record them as open:
1. When debt recovery is exhausted, does anything happen outside Debt Recovery (a write-off in Settlement, an external collector, a permanent block)?
2. Does a payment plan change what Settlement records as owed, or does Settlement only see individual "collect" requests?
3. Does the service area (used in Rentals' eligibility) come from the City's zone map? And who classifies an "unauthorized zone" return: Pricing when it charges, or Rentals when it accepts the return?
4. Who keeps the verified payment method (the processor token): Accounts or Payments?

Do you approve the proposed map? When you do, I'll write the findings report to `docs/context-discovery/velocity/2026-11-05-validate-reference-map/discovery.md`. No canvases, as you said.

## Expert reply 2

Approved: the proposed map in VeloCity-Context-Map-validated.cml is accepted, and you can write the findings report to the path you named.

The 11 relationships match my answers. Blocking is now Accounts' language, Payments stays upstream, and Fleet Maintenance translates Rentals' "available / withdrawn" into "broken". The fleet-issue route is confirmed as a Customer/Supplier relationship. Keep the comment explaining why the two Conformists to Payments are justified. In the report, also explain why the three "ACL in Customer/Supplier" warnings were kept on purpose.

Answers to your open questions:

1. **Exhausted recovery:** Nothing happens outside Debt Recovery today. The account stays blocked, and the debt stays on the rider's balance in Settlement as outstanding debt. No write-off, external collector or permanent-block step is in scope yet. Record write-off / external collection as deferred, next to billing disputes.

2. **Payment plans:** The plan and its schedule belong to Debt Recovery. Settlement only sees individual "collect" requests, one per installment. What the rider owes stays the same: Settlement's outstanding debt goes down only when money is actually collected. Once it reaches zero, Settlement publishes Outstanding Debt Cleared.

3. **Service area:** It is VeloCity's own decision about where we operate, not the City's map. The City defines stations and drop zones; everything else is an unauthorized zone. Rentals uses the service area for eligibility. Returns are always accepted, and Rentals only records where the ride ended. Pricing classifies the location against the City's zone map as station, drop zone or unauthorized zone, and applies the fee.

4. **Payment method token:** Payments keeps it. VeloCity never holds card details, and Payments shields us from the processor. Accounts only knows the business fact that the rider has a verified payment method on file, and later reacts to Payment Method Invalidated.

None of these answers changes an arrow, so the map stands as approved.

## Facilitator -> user (message 3, final)

The proposed map is approved and written down, and your original `src/main/cml/VeloCity-Context-Map.cml` is untouched.

**Files**
- `src/main/cml/VeloCity-Context-Map-validated.cml`: the corrected map. Validator: 10 contexts, 11 relationships, 0 errors.
- `docs/context-discovery/velocity/2026-11-05-validate-reference-map/discovery.md`: each finding mapped to the rule it answers, the four process walkthroughs, the critic log, the final validator output with how each line was resolved, and your answers.

**What the reference map breaks**
- **One indirect U/D cycle:** Accounts -> Rentals -> Settlement -> Accounts. Your "no cycles" check looked only at direct pairs. The fix is that Settlement calls Accounts' block / lift-block service, so Accounts is upstream.
- **6 unjustified Conformists:** to City, Rentals (twice), Pricing, Settlement (twice). They become ACLs.
- **2 downstreams with neither ACL nor CF:** Rentals from Accounts, and FleetMaintenance from CustomerSupport. Both get an ACL, and the CustomerSupport one is now confirmed.
- **No Customer/Supplier between internal teams:** three relationships become C/S.
- The 2 Conformists to Payments stay, justified in the CML comments.
- The contexts themselves passed the critic unchanged.

**How the review went**
- Contexts: all 8 got AGREE from the critic in round 1.
- Map: each of the 4 processes got AGREE in round 1, and the final pass got AGREE in round 2. No critic round changed a relationship; they only sharpened comments.

**Kept on purpose:** Context Mapper warns three times about an ACL inside a Customer/Supplier relationship. Our rules make C/S the internal default and an ACL the downstream default, and you want each downstream to keep its own language. The report explains this.

**Still open:**
- Deferred: billing disputes pausing Debt Recovery, and write-off or external collection after recovery is exhausted. Both are recorded as comments in the new CML.
- Not yet discussed: whether the live fee estimate during a ride is shown to riders. It wouldn't reverse any relationship.
- If you adopt the new map as the reference, `velocity/velocity-context-map.md` needs updating to match. That covers its diagram, its "No cycles" decision and its "Debt Recovery -> Accounts" alternative. I haven't changed it.
