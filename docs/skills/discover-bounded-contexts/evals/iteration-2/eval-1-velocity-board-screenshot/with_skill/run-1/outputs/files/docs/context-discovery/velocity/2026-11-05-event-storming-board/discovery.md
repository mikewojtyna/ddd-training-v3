# Bounded context discovery: VeloCity (event-storming board)

- **Date:** 2026-11-05
- **Mode:** discover
- **Status:** contexts approved (context map declined for now; canvases deferred to the next session)
- **Input:** `velocity/velocity-processes.jpg` (event-storming board, "Bike Rental Processes")
- **Context map:** not requested ("No context map for now. Let's stop at the contexts today.")
- **Participants:** the trainer as domain expert; Claude as facilitator; independent critic agents per context.

## Reading of the input

The board has seven labelled flows.

**Main timeline**
- *User Registration and Account Setup:* User Registered → Email Confirmed → Phone Number Verified → Basic Data Provided → Payment Method Added → Payment Method Verified → Agreement Signed → User Account Activated.
- *Bike Rental:* User Location Validated (plus "bike broken" as an input) → Bike Reserved → Bike Unlocked | Bike Unlock Failed → Bike Reservation Expired | Bike Reservation Expired. After Bike Unlocked: Rental Session Started → bike returned, and Maintenance Started → bike fixed → bike returned.
- *Pricing:* Rental Session Started → First Free Period Started/Ended → hourly billing periods, each with "Hour Fee Applied" → Fourth Hour And Subsequent Billing Period Started → Hourly Fee Applied → Rental Total Fee Calculated | Max Rental Time Exceeded → Penalty Applied.

**Branches (ride endings)**
- *Bike Station Return Handling:* Bike Returned To Station → Bike Locked In Station → Bike Fault Detected → Bike Fault Reported | Bike Locking Failed → Customer Support Contacted / Bike Fault Reported.
- *Bike Zone Return Handling:* Bike Returned to Zone → Return To Drop Zone Accepted → Drop Zone Fee Applied | Return To Unauthorized Zone Detected → Unauthorized Zone Return Fee Applied.

**Disconnected clusters**
- *Account Block and Debt Recovery:* Account Blocked Due To Invalid Card / Due To Debt → Account Block Notification Sent → Debt Recovery Process Started → Failed / Succeeded / Paused → Resumed → Succeeded / Failed.
- *Customer Support and Incident Handling:* Incident Reported → Incident Type Detected → Assigned To Agent → Accepted (→ Escalated → Resolved | Resolved) / Rejected / Rejected → Reopened → Resolved.

**Ambiguities raised:** the meaning of "bike broken" and the maintenance branch inside Bike Rental; duplicated stickies (Rental Session Started, bike returned); no link from return to the total fee or from the fee to a payment; no Rental Session Ended; no trigger for Account Blocked Due To Invalid Card.

**Expert's correction:** "Mostly right. Keep in mind that the board was drawn quickly, so some arrows show 'this can happen around here' rather than strict causation." Duplicates are the same events (incl. Rental Session Started and Account Blocked). "User Location Validated" stands for the per-rental account check. The maintenance branch isn't part of the ride.

## Heuristic trail

### H1: Pivotal events
| Event | Status | Before | After | Expert's answer |
|---|---|---|---|---|
| User Account Activated | confirmed | onboarding: who the user is | renting | "activated once, when the user has added a payment method. Then, before every rental, we check the account again" |
| Bike Reserved | rejected | – | – | "Reservation is not a phase of its own. It only lets you hold a bike before renting" |
| Rental Session Started | confirmed as phase change, **not a boundary** (H3) | validate, reserve, unlock | billable ride, price calculation starts | "validation, reservation and unlock have no extra rules of their own" |
| Rental Session Ended (missing sticky) | confirmed | ride (station/zone return, forced end) | the real charge | "The real payment happens when the rider returns the bike or is forced to end the session" |
| Rental Unsettled (missing sticky) | derived from answers | Settlement's scheduled retries | Debt Recovery; account block | "The rental counts as unsettled only when those attempts have run out. Everything after that belongs to debt recovery" |
| Account Blocked / Incident Reported | triggers of side lanes | – | – | blocks come from an unverified card or an unsettled payment; incidents from riders and automatic detection |

### H2: Swimlanes
| Flow | Verdict | Why |
|---|---|---|
| Price estimation during the ride | lane (folded into Pricing by H3) | repeats hourly on its own clock; "only an estimate for the rider's convenience" |
| Station vs zone return | **variant** | "same rules, apart from the extra fee for an unauthorized zone" |
| Maintenance | lane | "runs independently of rentals", on the crew's schedule; follows the bike, not a rental |
| Incident handling | lane | triggered from several places, own loops (escalate, reject, reopen); "Nothing flows back" |
| Debt recovery | lane | own rules and schedule, pauses/resumes, "days to weeks" |

### H3: Single responsibility
| Duplicated responsibility | Where it appeared | Resolution |
|---|---|---|
| Price estimate vs final price | Pricing lane (hourly fees) and Rental Total Fee Calculated | Same rules → one **Pricing** context |
| Preparation vs ride | Bike Rental before/after Rental Session Started | Preparation has no rules → merged into **Rental**; pivot kept as a phase change |
| Account blocking | onboarding (card not verified in time) and debt cluster | "A block works the same way whatever the reason" → **Rider Accounts** is the single owner; Settlement publishes facts it reacts to |
| Payment step vs charging rules vs recovery | debt cluster, implied payment | "Payment knows nothing about any rules" → mechanical **Payments**; schedule/retries → **Settlement**; actions/schedule after failure → **Debt Recovery** |
| Who acts after Rental Session Ended (empty side) | Pricing calculates, Payments executes, Rental ends at the pivot | **Settlement** owns when to charge and settled/unsettled ("Settling the rental has its own rules") |
| Payment Method Verified | Rider Accounts and Payments (critic, Payments r1) | Rider Accounts owns the event and the deadline; Payments only runs the card check |
| Customer Support Contacted | Rental (station return) vs Incident Handling (critic, Rental r1) | It is Incident Reported, owned by **Incident Handling**; Rental publishes Bike Locking Failed |
| "This bike can't be rented right now" | Rental, Fleet Maintenance, Incident Handling (critics, Rental r1 and Fleet Maintenance r1) | Expert: "A bike comes out of service automatically as soon as it's reported broken… The maintenance crew is the only one who puts it back" → **Fleet Maintenance** owns service status; Rental only consumes it |
| Retrying a failed charge | Settlement vs Debt Recovery | Split by the Rental Unsettled pivot: Settlement retries before, Debt Recovery acts after |
| Max rental time | Pricing lane vs Rental's forced end | Expert: tariff owners set it → Pricing owns the limit and the penalty; Rental owns detecting it and forcing the end |
| "Is this rental paid?" after debt recovery | Debt Recovery vs Settlement | "When the debt is recovered, the rental counts as paid" → Settlement marks it settled on Debt Recovered |

## Bounded contexts
| Context | Question it answers | Owns (events, decisions, rules) | Heuristic justification | Classification |
|---|---|---|---|---|
| Rider Accounts | Can this rider use the service? | User Registered … Agreement Signed, Payment Method Verified + verification deadline, User Account Activated; Account Blocked (card not verified in time / rental unsettled), Account Banned (staff, abuse/fraud, never lifts on its own), Account Unblocked, Account Block Notification Sent; answers "can this account rent?" | H1: User Account Activated pivot. H3: single owner of block/ban/unblock; others publish facts | not yet classified |
| Rental | Who has which bike, and when? | Rider Eligibility Checked (asks Rider Accounts), Bike Reserved, Bike Unlocked/Unlock Failed, Bike Reservation Expired, Rental Session Started, Bike Returned To Station/Zone, Bike Locked/Locking Failed, drop-zone acceptance / unauthorized zone detected, Max Rental Time Exceeded → forced end, Rental Session Ended, Bike Fault Detected On Return; rule: only in-service bikes can be reserved | H1: between Activation and Rental Session Ended; H3 merged preparation; H2 station/zone are variants | not yet classified |
| Pricing | What does this ride cost? | Price list (free period, hourly rates, max rental time, penalty, drop-zone and unauthorized-zone fees), Fee Estimated, Rental Total Fee Calculated; stateless calculator | H2 estimate lane; H3 estimate = final price; changes independently, "usable by anyone" | supporting (critic suggestion, unconfirmed) |
| Settlement | Has this rental been paid for, and when do we collect? | Reacts to Rental Session Ended; charge schedule and retries; Rental Settled / Rental Unsettled; marks the rental settled on Debt Recovered | H1: far side of Rental Session Ended; simple but own rules | supporting (simple, schedule rules) |
| Payments | Did the charge go through? | Executes charges and card checks against the payment processor; Payment Charged/Failed, Card Check Succeeded/Failed; no business rules | H3 rule ownership: "Payment knows nothing about any rules" | generic (critic suggestion, unconfirmed) |
| Debt Recovery | How do we recover what's owed? | Debt Recovery Process Started/Paused/Resumed/Succeeded (Debt Recovered)/Failed; reminders, card re-charge via Payments, hand-over to external collector, schedule over days–weeks | H2 own lane and clock; H3 own rules; starts at Rental Unsettled | not yet classified |
| Fleet Maintenance | Is this bike fit for service, and when is it back? | Bike Taken Out Of Service (automatic on any broken report), Maintenance Started, periodic inspections, urgent repairs first, Bike Repaired (crew sign-off), Bike Returned To Service | H2 own lane following the bike; H3 single owner of bike service status | supporting ("rules are simple") |
| Incident Handling | What went wrong, and is it resolved? | Incident Reported (riders, incl. contacting support on a locking failure; automatic detection), Incident Type Detected, Assigned To Agent, Accepted/Rejected/Escalated/Reopened/Resolved; publishes bike faults; nothing flows back on resolution | H2 own lane with own loops, many triggers | not yet classified |

**External parties:** payment processor; external debt collector.

## Critic log
| Subject | Round | Verdict | Blocking points / resolution |
|---|---|---|---|
| Rider Accounts | 1 | AGREE | Notes: ban vs block?; split Payment Method Verified ownership; add Account Unblocked |
| Rental | 1 | DISAGREE | (1) "can this bike be rented?" owned by Rental and Fleet Maintenance; (2) Customer Support Contacted is incident intake. Fix: (2) applied directly; (1) put to the expert (round 3, Q1) |
| Pricing | 1 | AGREE | Questions on max rental time owner and who asks for the final price → asked in round 3 |
| Settlement | 1 | AGREE | Questions on retry ownership and the final-price path → asked in round 3; reworded question to avoid overlap with Pricing |
| Payments | 1 | DISAGREE | Payment Method Verified had two owners. Fix applied: Rider Accounts owns event and deadline, Payments executes the check |
| Debt Recovery | 1 | AGREE | Questions: does it charge via Payments? does a recovered debt settle the rental? → asked in round 3 |
| Fleet Maintenance | 1 | DISAGREE | Bike service status had three owners. Resolved by expert round 3, Q1: Fleet Maintenance is the single owner |
| Incident Handling | 1 | AGREE | Questions on who decides out-of-service and on Customer Support Contacted → both resolved |
| Rider Accounts | 2 | AGREE | A block must track open reasons; unblock never lifts a ban |
| Rental | 2 | AGREE | Both blockers resolved; fault on return has two consumers |
| Pricing | 2 | AGREE | Pricing owns the max-time value; Rental owns enforcement |
| Settlement | 2 | AGREE | Retry ownership split at Rental Unsettled; warns of a Settlement ↔ Debt Recovery cycle if a map is drawn |
| Payments | 2 | AGREE | Rule-free; outcome events need a caller reference |
| Debt Recovery | 2 | AGREE | Settled state stays in Settlement |
| Fleet Maintenance | 2 | AGREE | Own rules confirmed; taking out of service must be idempotent |
| Incident Handling | 2 | AGREE | Choose one out-of-service path or make it idempotent |

## Context map
Not requested in this session.

## Q&A with the domain expert

### Round 1 (reading + H1)
Questions:
1. Is my reading right? Anything I misread or that's missing from the board?
2. What's the difference between *User Account Activated* and *User Location Validated*? Is activation a one-time event, and does something get checked before every rental?
3. What does *Rental Session Started* mean in business terms? Is *Bike Reserved* a phase with its own rules (time limits, fees), or just a convenience before renting?
4. What happens when the bike is returned: does the return end the session and trigger the real charge? Are the hourly "Fee Applied" stickies actual charges or estimates?
5. Who or what triggers *Account Blocked Due To Invalid Card* and *Due To Debt*? Where does the debt come from?
6. Where does *Bike Fault Reported* (on return) lead? And what do "bike broken" and "Maintenance Started → bike fixed" in Bike Rental mean?
7. Rental Session Started and "bike returned" appear more than once. Are the duplicates the same events?

Answers (verbatim):
1. Mostly right. Keep in mind that the board was drawn quickly, so some arrows show "this can happen around here" rather than strict causation. You already found the real gaps.
2. The account is activated once, when the user has added a payment method. Then, before every rental, we check the account again so that blocked or banned users can't rent. That check is what User Location Validated stands for on the board.
3. Rental Session Started means the renter has rented the bike and the ride has begun, so price calculation can start. Reservation is not a phase of its own. It only lets you hold a bike before renting, and validation, reservation and unlock have no extra rules of their own.
4. Yes. The real payment happens when the rider returns the bike or is forced to end the session. The hourly "Fee Applied" stickies during the ride are only an estimate for the rider's convenience. The estimate and the final charge use the same pricing rules.
5. An account is blocked for two reasons: the payment method wasn't verified in time, or a payment is unsettled. The debt is a rental charge that couldn't be collected.
6. A fault reported on return is checked when the bike is returned and then goes to incident handling. Broken bikes and faults belong with incidents. Maintenance runs independently of rentals, so "Maintenance Started → bike fixed" isn't really part of the ride. "Bike broken" before Bike Reserved just means a broken bike can't be reserved [improvised].
7. Yes, the duplicates are the same events shown in different flows. That includes Rental Session Started and Account Blocked. Station and zone returns follow the same rules, except for the extra fee when a bike is left in an unauthorized zone.

### Round 2 (confirmed pivots + H2)
Questions (abridged; full text in the session transcript): 1 confirm the three pivots and station/zone as variants; 2 who decides what is owed after Rental Session Ended, and does it have rules; 3 does payment have rules; 4 does debt recovery have its own rules and timescale; 5 who owns blocking/unblocking; 6 maintenance triggers and feedback; 7 incident sources and feedback; 8 does pricing change independently; 9 what forces a session to end.

Answers (verbatim):
1. Yes, I confirm all three. Rental Session Ended covers both a normal return and a forced end. Station and zone returns follow the same rules, apart from the extra fee for an unauthorized zone, so treat them as variants of the same ending.
2. Settling the rental has its own rules. They're not very complex and are mostly about the schedule, meaning when we charge. Those rules don't depend on the rental itself. If the payment stays unsettled, the account gets blocked.
3. No. Payment knows nothing about any rules. It runs the charge and reports whether it succeeded or failed.
4. Yes. Debt recovery has its own rules about which actions to take and on what schedule, and that's why it pauses and resumes. It runs over days to weeks, much longer than a ride [improvised].
5. A block works the same way whatever the reason: before every rental we only check whether the account is blocked. The reason for the block comes from wherever the problem arose, either the card not being verified in time or an unsettled payment. The block is lifted once that problem is fixed, for example when the debt is recovered or the card is verified [improvised].
6. Maintenance runs independently of rentals. The maintenance crew starts it on their own schedule or when a bike is known to be broken. While a bike is in maintenance, it isn't available to rent [improvised]. A bike can be reported broken when it's returned to a station or a zone. That report is checked during the return and goes to incident handling, and incident handling may then send the bike to maintenance [improvised].
7. Riders report incidents, and some are detected automatically. Nothing flows back to the other flows when an incident is resolved.
8. Yes. Pricing changes independently. It's just a simple calculator that any other part of the business, or any person, can use. The ride uses it for both the estimate and the final price.
9. Yes. Max Rental Time Exceeded forces the session to end, and Penalty Applied is part of the price for that ride [improvised].

### Round 3 (H3 + critic questions)
Questions (abridged): 1 who takes a broken bike out of service and who puts it back; 2 does maintenance have its own rules; 3 does settlement retry before "unsettled"; 4 how debt recovery collects and whether a recovered debt settles the rental; 5 does the session end on a locking failure; 6 who sets the maximum rental time; 7 is a ban different from a block.

Answers (verbatim):
1. A bike comes out of service automatically as soon as it's reported broken, so nobody can reserve it while we look into it. The maintenance crew is the only one who puts it back, once they've fixed it [improvised].
2. Maintenance runs on its own, independently of rentals. It has its own routine: periodic inspections, urgent repairs first, and a bike counts as fixed only when the crew signs it off. It's more than a to-do list, but the rules are simple [improvised].
3. Settlement's rules are mostly about the schedule, so it retries the charge a few times on that schedule. The rental counts as unsettled only when those attempts have run out. Everything after that belongs to debt recovery [improvised].
4. Debt recovery has its own rules about which actions to take and when. It sends reminders and charges the card again, and in the end it can hand the case to an external collector. When the debt is recovered, the rental counts as paid [improvised].
5. The session still ends and gets charged. The locking problem is handled separately as an incident, and nothing flows back to the rental [improvised, consistent with "no connections back when resolved"].
6. The people who own the tariff set it. The maximum time and the penalty are part of the price list [improvised].
7. No. For renting, a ban works the same way as a block: the account can't rent. The only difference is that a person decides a ban, for abuse or fraud, and it doesn't lift on its own [improvised].

### Round 4 (approval)
1. Do you approve these eight contexts? — "Yes, I approve. The list matches what I told you, so we'll build on these eight."
2. Do you want a context map in Context Mapper (.cml) format? — "No context map for now. Let's stop at the contexts today. We'll do the canvases in the next session too, not today."

## Open questions and assumptions
- **Assumption:** a fault found on return reaches Fleet Maintenance directly from Rental and possibly again via Incident Handling; taking a bike out of service is idempotent. Pick one path when drawing the map.
- Who reports the abuse that leads to a ban? Can a person lift a ban later (Account Unbanned)?
- What pauses and resumes debt recovery (promise to pay, dispute, timer)? If a dispute/incident pauses it, that contradicts "nothing flows back".
- Is a debt per rental or per rider?
- After Debt Recovery Process Failed or a hand-over to the collector: is the rental written off, does it stay unsettled, does the account get banned?
- What happens to an active reservation when its bike goes out of service?
- For a future map: Settlement ↔ Debt Recovery exchange events both ways (Rental Unsettled, Debt Recovered); decide the model dependency to avoid a U/D cycle.
- Strategic classification (core/supporting/generic) not confirmed by the expert.

## Suggested board changes
- **Add:** Rental Session Ended (normal and forced), Payment Charged / Payment Failed, Card Check Succeeded / Failed, Rental Settled / Rental Unsettled, Account Unblocked, Account Banned, Payment Method Verification Deadline Passed, Bike Taken Out Of Service.
- **Rename:** "Hour Fee Applied"/"Hourly Fee Applied" → Fee Estimated; "User Location Validated" → Rider Eligibility Checked; "bike fixed" → Bike Repaired; second "bike returned" → Bike Returned To Service; "Bike Fault Reported" (station return) → Bike Fault Detected On Return; "Debt Recovery Process Succeeded" → Debt Recovered. Optionally "…Fee Applied"/"Penalty Applied" → "…Included In Price".
- **Move/merge:** Max Rental Time Exceeded into the rental flow; hang Account Blocked Due To Debt and Debt Recovery Process Started off Rental Unsettled; merge the two Incident Rejected stickies; treat "Customer Support Contacted" as Incident Reported; mark the billing-period stickies as Pricing breakdown lines.

## Next steps
- Next session (expert's choice): decide whether to build the context map (`src/main/cml/VeloCity-Context-Map.cml` already exists as the workshop reference, so a new file such as `VeloCity-Context-Map-event-storming-board.cml` would be proposed), then hand each context to `bounded-context-scaffolder` for its canvas at `docs/bounded-contexts/velocity/<context-package>/canvas.md`.
- Resume from this file: Status is `contexts approved`.
