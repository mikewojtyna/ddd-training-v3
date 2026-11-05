# Transcript: discover-bounded-contexts, VeloCity board (eval-1, with_skill, run-1)

## User (first message)
"Here's our VeloCity event storming board: velocity/velocity-processes.jpg. Find the bounded contexts on it. I'm the trainer, by the way, so you can't spoil anything for me."

## Facilitator → user, message 1

Thanks. Since you're the trainer, I won't hide anything, but I'll still let your answers decide the boundaries rather than the reference map. Before applying any heuristic, here is how I read the board, so you can correct me cheaply.

**Main timeline**
- *User Registration and Account Setup*: User Registered → Email Confirmed → Phone Number Verified → Basic Data Provided → Payment Method Added → Payment Method Verified → Agreement Signed → User Account Activated.
- *Bike Rental*: User Location Validated (and, as a second input, "bike broken") → Bike Reserved → then three branches: Bike Unlocked; Bike Unlock Failed → Bike Reservation Expired; Bike Reservation Expired directly. After Bike Unlocked: Rental Session Started → bike returned, and a second branch Maintenance Started → bike fixed → bike returned.
- *Pricing*: Rental Session Started → First Free Period Started/Ended → First/Second/Third hour billing periods each followed by an "Hour Fee Applied" → Fourth Hour And Subsequent Billing Period Started → either Hourly Fee Applied → Rental Total Fee Calculated, or Max Rental Time Exceeded → Penalty Applied.

**Branches (ride endings)**
- *Bike Station Return Handling*: Bike Returned To Station → Bike Locked In Station → Bike Fault Detected → Bike Fault Reported; or Bike Locking Failed → Customer Support Contacted / Bike Fault Reported.
- *Bike Zone Return Handling*: Bike Returned to Zone → Return To Drop Zone Accepted → Drop Zone Fee Applied; or Return To Unauthorized Zone Detected → Unauthorized Zone Return Fee Applied.

**Disconnected clusters**
- *Account Block and Debt Recovery*: Account Blocked Due To Invalid Card / Due To Debt → Account Block Notification Sent → Debt Recovery Process Started → Failed / Succeeded / Paused → Resumed → Succeeded / Failed.
- *Customer Support and Incident Handling*: Incident Reported → Incident Type Detected → Assigned To Agent → Accepted (→ Escalated → Resolved, or Resolved) / Rejected / Rejected → Reopened → Resolved.

**Ambiguities I noticed**
- "bike broken" feeds into Bike Reserved like a precondition, and "Maintenance Started → bike fixed → bike returned" hangs off Bike Unlocked. I'm not sure what those arrows mean.
- Rental Session Started appears in both Bike Rental and Pricing; "bike returned" appears twice in Bike Rental, and again as "Returned To Station/Zone".
- Nothing connects the return to Rental Total Fee Calculated, or the fee to any payment. There's no Payment Charged/Failed and no "Rental Session Ended" sticky.
- Account Blocked Due To Invalid Card has no trigger on the board.

**H1: candidate pivotal events** (candidates until you confirm them)
- **User Account Activated**: before it, everything is about who the user is; after it, about bikes and rides.
- **Bike Reserved vs Rental Session Started**: one of them is where renting really starts.
- **Bike Returned (station/zone)**: the end of the ride, and maybe the start of charging.
- **Rental Total Fee Calculated**: the hand-off from riding to paying.
- **Account Blocked (card / debt)** and **Incident Reported**: triggers of the two disconnected clusters.

**Questions**
1. Is my reading right? Anything I misread or that's missing from the board? *(fixes the input before any boundary is drawn)*
2. What's the difference between *User Account Activated* and *User Location Validated*? Is activation a one-time event, and does something get checked before every rental? *(decides whether Activation is the pivot between account setup and renting)*
3. What does *Rental Session Started* mean in business terms? Is *Bike Reserved* a phase with its own rules (time limits, fees), or just a convenience before renting? *(decides whether reservation is its own phase)*
4. What happens when the bike is returned: does the return end the session and trigger the real charge? Are the hourly "Fee Applied" stickies actual charges or estimates? *(decides where the ride ends and whether charging is a separate phase)*
5. Who or what triggers *Account Blocked Due To Invalid Card* and *Due To Debt*? Where does the debt come from? *(decides what connects the debt cluster to the main timeline)*
6. Where does *Bike Fault Reported* (on return) lead: to maintenance, to an incident, or both? And what do "bike broken" and "Maintenance Started → bike fixed" in Bike Rental mean? *(decides whether faults/maintenance belong to the ride)*
7. Rental Session Started and "bike returned" appear more than once. Are the duplicates the same events? *(decides whether Pricing and Rental share a timeline)*

## Expert reply 1

1. Mostly right. Keep in mind that the board was drawn quickly, so some arrows show "this can happen around here" rather than strict causation. You already found the real gaps.

2. The account is activated once, when the user has added a payment method. Then, before every rental, we check the account again so that blocked or banned users can't rent. That check is what User Location Validated stands for on the board.

3. Rental Session Started means the renter has rented the bike and the ride has begun, so price calculation can start. Reservation is not a phase of its own. It only lets you hold a bike before renting, and validation, reservation and unlock have no extra rules of their own.

4. Yes. The real payment happens when the rider returns the bike or is forced to end the session. The hourly "Fee Applied" stickies during the ride are only an estimate for the rider's convenience. The estimate and the final charge use the same pricing rules.

5. An account is blocked for two reasons: the payment method wasn't verified in time, or a payment is unsettled. The debt is a rental charge that couldn't be collected.

6. A fault reported on return is checked when the bike is returned and then goes to incident handling. Broken bikes and faults belong with incidents. Maintenance runs independently of rentals, so "Maintenance Started → bike fixed" isn't really part of the ride. "Bike broken" before Bike Reserved just means a broken bike can't be reserved [improvised].

7. Yes, the duplicates are the same events shown in different flows. That includes Rental Session Started and Account Blocked. Station and zone returns follow the same rules, except for the extra fee when a bike is left in an unauthorized zone.

## Facilitator → user, message 2

Thanks, that moves a lot. What changed:

**H1: pivots, after your answers**
- **User Account Activated: confirmed.** One-time, after the payment method is added. Before it: onboarding (who the user is). After it: renting. The per-rental check ("User Location Validated") is not part of onboarding; it's a gate in front of every rental.
- **Bike Reserved: rejected as a pivot.** Reservation is a convenience with no rules of its own.
- **Rental Session Started: confirmed as a phase change** (the ride becomes billable, price calculation starts). But since validation, reservation and unlock have no rules of their own, I expect H3 to keep preparation and the ride in **one** context. The pivot marks a phase, not a boundary.
- **Rental Session Ended: confirmed, but missing from the board.** It's the real pivot of the return (station, zone or forced end) and triggers the real charge. "Bike Returned To Station/Zone" are its two variants.
- **Rental Total Fee Calculated** is the real price; the hourly "Fee Applied" stickies are **estimates** (same rules).

The far side of *Rental Session Ended* is still thin on the board: a price is calculated, but nothing says who decides what the rider owes, who charges, and what happens when the charge fails. I don't want to leave that side without an owner.

**H2: swimlane candidates**
| Flow | Why it looks like a lane |
|---|---|
| Price estimation during the ride | repeats on its own clock (hourly) while the ride carries on |
| Station vs zone return | looks like two flows, but you said same rules except one fee, so I treat them as **variants** of one ride ending, not lanes |
| Maintenance | follows a bike's condition, not a rental; you said it runs independently |
| Incident handling | triggered from several places (rider reports, fault detection at return, locking failure), with its own loops (escalate, reject, reopen) |
| Account block and debt recovery | much longer timeline, pause/resume |

**Questions**
1. Do you confirm the three pivots: *User Account Activated*, *Rental Session Started*, and a new *Rental Session Ended* sticky (covering normal return and forced end)? And that station vs zone return are variants of the same ending? *(fixes the phases)*
2. After *Rental Session Ended*: who decides what the rider owes for the rental and when it's charged? Does that step have its own rules (when to charge, what counts as settled/unsettled)? *(decides whether there's a context on the far side of the pivot, or whether it's part of the ride or of payments)*
3. Does the payment step itself have business rules, or does it just execute a charge against the card provider and report success or failure? *(decides whether payments is a mechanical context, and where rule-driven work lives)*
4. Debt recovery: does it have its own rules, such as which actions to take, a schedule, why it pauses and resumes? Over what time scale does it run? *(decides whether it's a lane of its own or part of payments)*
5. Blocking has two reasons (card not verified in time, unsettled payment). Who decides that a rider can't rent: one owner whatever the reason, or does each department decide its own block? And who unblocks when the debt is recovered? *(H3: one owner per decision)*
6. Maintenance: who or what starts it? When a bike goes into maintenance or is fixed, does anything flow back into renting (e.g. bike unavailable)? Do incidents ever trigger maintenance? *(decides the maintenance lane and its hand-offs)*
7. Incidents: who reports them, and when one is resolved does anything flow back (a refund, a maintenance order, an unblock)? *(decides whether incident handling is self-contained)*
8. Pricing: does it change independently of renting (different people, different schedule)? Is anything besides the ride using it? *(decides whether pricing stands on its own)*
9. What forces a session to end: is it *Max Rental Time Exceeded*, and is *Penalty Applied* part of the price? *(decides who owns the forced end)*

## Expert reply 2

1. Yes, I confirm all three. Rental Session Ended covers both a normal return and a forced end. Station and zone returns follow the same rules, apart from the extra fee for an unauthorized zone, so treat them as variants of the same ending.

2. Settling the rental has its own rules. They're not very complex and are mostly about the schedule, meaning when we charge. Those rules don't depend on the rental itself. If the payment stays unsettled, the account gets blocked.

3. No. Payment knows nothing about any rules. It runs the charge and reports whether it succeeded or failed.

4. Yes. Debt recovery has its own rules about which actions to take and on what schedule, and that's why it pauses and resumes. It runs over days to weeks, much longer than a ride [improvised].

5. A block works the same way whatever the reason: before every rental we only check whether the account is blocked. The reason for the block comes from wherever the problem arose, either the card not being verified in time or an unsettled payment. The block is lifted once that problem is fixed, for example when the debt is recovered or the card is verified [improvised].

6. Maintenance runs independently of rentals. The maintenance crew starts it on their own schedule or when a bike is known to be broken. While a bike is in maintenance, it isn't available to rent [improvised]. A bike can be reported broken when it's returned to a station or a zone. That report is checked during the return and goes to incident handling, and incident handling may then send the bike to maintenance [improvised].

7. Riders report incidents, and some are detected automatically. Nothing flows back to the other flows when an incident is resolved.

8. Yes. Pricing changes independently. It's just a simple calculator that any other part of the business, or any person, can use. The ride uses it for both the estimate and the final price.

9. Yes. Max Rental Time Exceeded forces the session to end, and Penalty Applied is part of the price for that ride [improvised].

## Facilitator → user, message 3

**Round 3: H3, single responsibility.** Your answers gave me a working draft of eight candidates. I had an independent critic try to break each one before showing it to you. Five passed; three raised points I could fix myself or that only you can settle.

**Working draft (not yet for approval)**
| Candidate | The one question it answers |
|---|---|
| Rider Accounts | Can this rider use the service? |
| Rental | Who has which bike, and when? |
| Pricing | What does this ride cost? |
| Settlement | Has this rental been paid for, and when do we collect? |
| Payments | Did the charge go through? |
| Debt Recovery | How do we recover what's owed? |
| Fleet Maintenance | Is this bike in working order? |
| Incident Handling | What went wrong, and is it resolved? |

**How H3 moved things**
- **Preparation merges into Rental.** Validation, reservation and unlock have no rules of their own (your round-1 answer), so *Rental Session Started* stays a phase change inside Rental, not a boundary.
- **Estimate and final price are one Pricing context.** Same rules, and it changes independently as a calculator anyone can use.
- **Blocking has one owner: Rider Accounts.** Both block reasons (card not verified in time, unsettled payment) end in the same block, and the rental check only asks "is it blocked?". So Settlement publishes *Rental Unsettled*, Debt Recovery publishes *Debt Recovered*, and Rider Accounts blocks and unblocks. Otherwise blocking would have two owners.
- **Settlement fills the empty side of *Rental Session Ended*.** Pricing calculates, Payments executes, Rental stops at the pivot. Settlement owns "when we charge" and settled/unsettled, which you said are its own rules.
- **Payments is purely mechanical; Debt Recovery is its own context** (its own actions and schedule over days to weeks).

**Critic fixes I applied**
- *Payment Method Verified* was listed under both Rider Accounts and Payments. Now Rider Accounts owns the event and the verification deadline. Payments only runs the card check against the processor and reports the result.
- *Customer Support Contacted* (after a locking failure) was under Rental. You said riders report incidents and faults belong with incidents, so it's now *Incident Reported*, owned by Incident Handling. Rental only publishes *Bike Locking Failed* / *Bike Fault Detected On Return*.

**Open: one decision has three would-be owners.** "This bike can't be rented right now" shows up in Rental (a broken bike can't be reserved), Fleet Maintenance (a bike in maintenance isn't available) and Incident Handling (broken bikes belong with incidents). H3 says one owner.

**Questions**
1. When a bike is reported broken (on return, by a rider, or detected automatically), who takes it out of service so it can't be reserved: incident handling when it accepts the report, the maintenance crew, or does it happen automatically on the report? And who puts it back into service? *(decides the single owner of the bike's service status: Fleet Maintenance, Incident Handling, or Rental)*
2. Does maintenance have rules of its own, such as an inspection schedule, priorities, or what counts as "fixed"? Or is it just the crew's to-do list? *(decides whether Fleet Maintenance owns rules or only status; supporting vs generic)*
3. When a charge fails after a ride, does settlement retry on its own schedule before declaring the rental unsettled, or is the first failure already "unsettled", with every further attempt done by debt recovery? *(decides who owns the "try again" rule: Settlement or Debt Recovery)*
4. When debt recovery tries to collect, does it charge the card again through payments, or does it use other means (reminders, transfer, an external collector)? And when a debt is recovered, does the original rental count as settled? *(decides the Debt Recovery hand-offs and whether "is it settled?" has one owner)*
5. When a bike fails to lock at a station, does the rental session still end (and get charged), or does it stay open until support sorts it out? *(decides whether anything flows back from Incident Handling to Rental)*
6. Who sets the maximum rental time: the people who own the tariff, or the people who run rentals? *(decides whether Pricing or Rental owns the limit that forces the end)*
7. You mentioned "blocked or banned" users. Is a ban different from a block (permanent, decided by a person, other reasons)? *(decides whether there's another decision Rider Accounts must own)*

## Expert reply 3

1. A bike comes out of service automatically as soon as it's reported broken, so nobody can reserve it while we look into it. The maintenance crew is the only one who puts it back, once they've fixed it [improvised].

2. Maintenance runs on its own, independently of rentals. It has its own routine: periodic inspections, urgent repairs first, and a bike counts as fixed only when the crew signs it off. It's more than a to-do list, but the rules are simple [improvised].

3. Settlement's rules are mostly about the schedule, so it retries the charge a few times on that schedule. The rental counts as unsettled only when those attempts have run out. Everything after that belongs to debt recovery [improvised].

4. Debt recovery has its own rules about which actions to take and when. It sends reminders and charges the card again, and in the end it can hand the case to an external collector. When the debt is recovered, the rental counts as paid [improvised].

5. The session still ends and gets charged. The locking problem is handled separately as an incident, and nothing flows back to the rental [improvised, consistent with "no connections back when resolved"].

6. The people who own the tariff set it. The maximum time and the penalty are part of the price list [improvised].

7. No. For renting, a ban works the same way as a block: the account can't rent. The only difference is that a person decides a ban, for abuse or fraud, and it doesn't lift on its own [improvised].

## Facilitator → user, message 4

Your round-3 answers settled the last ownership conflict, and all eight candidates now pass the critic (round 2 for every one of them; three had disagreed in round 1). Here's the list for your approval.

**1. Rider Accounts: _Can this rider use the service?_**
- Owns: registration and verification steps, *Payment Method Verified* and its deadline, *User Account Activated*; *Account Blocked* (card not verified in time, rental unsettled), *Account Banned* (staff decision for abuse/fraud, never lifts on its own), *Account Unblocked*, block notification.
- Why: *User Account Activated* is the confirmed onboarding pivot. H3 made it the **single owner of blocking**: you said a block works the same whatever the reason, so others only publish facts (*Rental Unsettled/Settled*, card check result) it reacts to.
- Critic note: a block must track its open reasons, so settling one rental doesn't unblock an account with another unsettled one, and an unblock never lifts a ban.

**2. Rental: _Who has which bike, and when?_**
- Owns: the per-rental eligibility check (asks Rider Accounts), reservation, unlock, *Rental Session Started*, station/zone returns, locking, forced end at the maximum time, *Rental Session Ended*, the fault check on return.
- Why: preparation has no rules of its own, so *Rental Session Started* is a phase change inside Rental, not a boundary. Station and zone returns are variants (same rules, one extra fee). It only *consumes* bike service status and account status.

**3. Pricing: _What does this ride cost?_**
- Owns: the price list (free period, hourly rates, maximum rental time, penalty, zone fees), *Fee Estimated*, *Rental Total Fee Calculated*.
- Why: estimate and final price are one calculation (same rules). It changes independently, as a calculator anyone can use. Stateless: the billing periods are lines in its breakdown, not its own timeline.

**4. Settlement: _Has this rental been paid for, and when do we collect?_**
- Owns: reacting to *Rental Session Ended*, the charge schedule and retries, *Rental Settled* / *Rental Unsettled*, and marking the rental paid when a debt is recovered.
- Why: it fills the far side of the *Rental Session Ended* pivot. Its rules are simple (schedule) but its own, which makes it supporting, not absent.

**5. Payments: _Did the charge go through?_**
- Owns: executing charges and card checks against the external processor; *Payment Charged/Failed*, *Card Check Succeeded/Failed*.
- Why: you said "Payment knows nothing about any rules". All rule-driven work (retries, recovery) was moved out to Settlement and Debt Recovery.

**6. Debt Recovery: _How do we recover what's owed?_**
- Owns: Started, Paused, Resumed, Succeeded (*Debt Recovered*), Failed; reminders, re-charging, hand-over to an external collector, and their schedule.
- Why: its own lane on a days-to-weeks clock with its own rules. It begins exactly where Settlement's retries run out (*Rental Unsettled*), so each "try again" rule has one owner.

**7. Fleet Maintenance: _Is this bike fit for service, and when is it back?_**
- Owns: the bike's service status (*Bike Taken Out Of Service* automatically on any broken report, *Bike Returned To Service* only on crew sign-off), inspections, urgent-repairs-first, *Maintenance Started*, *Bike Repaired*.
- Why: its own lane following the bike, not a rental. Critic round 1 found "this bike can't be rented" had three owners; your answer made Fleet Maintenance the only one.

**8. Incident Handling: _What went wrong, and is it resolved?_**
- Owns: *Incident Reported* (riders, incl. contacting support on a locking failure, or automatic detection), type detection, assignment, accept/reject/escalate/reopen/resolve.
- Why: its own lane with its own loops, triggered from several places, and nothing flows back when it's resolved. It reports bike faults but doesn't own bike status.

**External parties (not designed by us):** payment processor, external debt collector.

**Open assumptions** (non-blocking, they refine relationships, not boundaries)
- A fault found on return reaches Fleet Maintenance directly from Rental, and may also come through Incident Handling. Taking a bike out of service is assumed to be idempotent.
- Unknown: who reports abuse leading to a ban, what pauses debt recovery, whether a debt is per rental or per rider, what happens after a recovery fails or goes to the collector, and what happens to an active reservation when its bike goes out of service.

**Suggested board changes**
- Add: *Rental Session Ended* (normal and forced), *Payment Charged/Failed*, *Card Check Succeeded/Failed*, *Rental Settled/Unsettled*, *Account Unblocked*, *Account Banned*, *Payment Method Verification Deadline Passed*, *Bike Taken Out Of Service*.
- Rename: "Hour Fee Applied"/"Hourly Fee Applied" → *Fee Estimated*; "User Location Validated" → *Rider Eligibility Checked*; "bike fixed" → *Bike Repaired*; the second "bike returned" → *Bike Returned To Service*; "Bike Fault Reported" (station return) → *Bike Fault Detected On Return*; "Debt Recovery Process Succeeded" → *Debt Recovered*.
- Move *Max Rental Time Exceeded* into the rental flow (Rental enforces it; Pricing owns the limit). Hang *Account Blocked Due To Debt* and *Debt Recovery Process Started* off *Rental Unsettled*. Merge the two *Incident Rejected* stickies. Treat "Customer Support Contacted" as *Incident Reported*.

**Questions**
1. Do you approve these eight contexts as the list to build on?
2. Do you want a context map in Context Mapper (.cml) format?

## Expert reply 4

1. Yes, I approve. The list matches what I told you, so we'll build on these eight.

2. No context map for now. Let's stop at the contexts today. We'll do the canvases in the next session too, not today.

## Critic verdicts (one line each, in order received)

Round 1 critics ran after expert reply 2 (before message 3); round 2 critics ran after expert reply 3 (before message 4).

- Rider Accounts | round 1 | AGREE | single block owner is correct H3 resolution; notes: ban vs block?, can Settlement lift a block?, split Payment Method Verified (Accounts owns event/deadline, Payments executes), add Account Unblocked sticky
- Payments | round 1 | DISAGREE | blocking: Payment Method Verified had two owners; fix: Rider Accounts owns the event + deadline, Payments only executes a card check and reports the result. Question: does debt recovery charge via Payments?
- Debt Recovery | round 1 | AGREE | own question, own clock (days-weeks), own rules; questions: charges via Payments?, does a recovered debt settle the rental?, what after Failed?, per rental or per rider?
- Pricing | round 1 | AGREE | one question, estimate+final one calculation, changes independently; questions: who owns max rental time?, who asks for the final price (Rental or Settlement)?; note: Pricing is a stateless calculator, billing periods are its breakdown
- Settlement | round 1 | AGREE | owns far side of Rental Session Ended; questions: does Settlement retry before declaring unsettled?, who asks Pricing for final price?; note: reword question to avoid overlap with Pricing
- Incident Handling | round 1 | AGREE | own lane with own loops; questions: who decides a reported-broken bike is out of service?, is Customer Support Contacted = Incident Reported?
- Fleet Maintenance | round 1 | DISAGREE | blocking: "bike can't be rented" (service status) had no single owner across Fleet Maintenance / Incident Handling / Rental; expert question: who takes a broken bike out of service and puts it back?; also: does maintenance have its own rules?
- Rental | round 1 | DISAGREE | blocking: (1) "can this bike be rented?" owned by both Rental and Fleet Maintenance; (2) Customer Support Contacted (locking failure) is incident intake, belongs to Incident Handling; questions: does the session end on locking failure?, who marks a reported bike unrentable?, max rental time: tariff or rental policy?
- Payments | round 2 | AGREE | blocker resolved; rule-free, serves Settlement, Debt Recovery, Rider Accounts; outcome events need caller reference
- Fleet Maintenance | round 2 | AGREE | blocker resolved: single owner of bike service status; own rules (inspections, urgent first, sign-off); out-of-service must be idempotent
- Pricing | round 2 | AGREE | Pricing owns max-time value and penalty; Rental owns detecting/forcing the end; stateless calculator, billing periods are breakdown lines
- Settlement | round 2 | AGREE | retries before Unsettled are Settlement's, after are Debt Recovery's; single owner of "settled"; warns of Settlement<->Debt Recovery cycle for the map
- Rental | round 2 | AGREE | both blockers resolved; Fault Detected On Return has two consumers (Fleet Maintenance, Incident Handling); edge case: reservation when bike goes out of service
- Rider Accounts | round 2 | AGREE | block/ban/unblock one owner; block must track open reasons; Payments card-check results are a source; who reports abuse for bans?
- Debt Recovery | round 2 | AGREE | retry ownership split by Rental Unsettled pivot; settled state stays in Settlement; external collector is external party
- Incident Handling | round 2 | AGREE | no bike-status ownership; Customer Support Contacted = Incident Reported; pick one out-of-service path or make it idempotent

## Facilitator → user, final message

Approved, and I've stopped at the contexts as you asked. There's no context map and no canvases this session.

**Report:** `docs/context-discovery/velocity/2026-11-05-event-storming-board/discovery.md` (Status: contexts approved). It holds the heuristic trail, all Q&A verbatim, the critic log and the suggested board changes. Start the next session from it.

**Result:** 8 bounded contexts, 0 relationships (no map yet), and 2 external parties: the payment processor and the external debt collector. The contexts are Rider Accounts, Rental, Pricing, Settlement, Payments, Debt Recovery, Fleet Maintenance and Incident Handling.

**Critic rounds that changed something** (all eight contexts were approved in round 2):
- Payments, round 1: *Payment Method Verified* had two owners. Rider Accounts now owns it, and Payments only runs the card check.
- Rental, round 1: *Customer Support Contacted* moved to Incident Handling as *Incident Reported*.
- Rental and Fleet Maintenance, round 1: "this bike can't be rented" had three owners. Your answer made Fleet Maintenance the single owner of the bike's service status.

**Still open:**
- Who reports the abuse behind a ban, and whether a ban can be lifted.
- What pauses debt recovery.
- Whether a debt is tracked per rental or per rider.
- What happens after recovery fails or the case goes to the collector.
- What happens to a reservation when its bike goes out of service.
- Whether a bike reported broken on return reaches Fleet Maintenance directly, through Incident Handling, or both. For now I've assumed it can come through both and that taking the bike out of service twice changes nothing.
- No core, supporting or generic classification has been agreed yet.
- For the map: Settlement and Debt Recovery send events to each other, so the map needs a decision on which one is upstream.

**Next session:**
- The context map. `src/main/cml/VeloCity-Context-Map.cml` is the workshop reference, so I'll propose a new file instead of overwriting it.
- One canvas per context via `bounded-context-scaffolder`.
