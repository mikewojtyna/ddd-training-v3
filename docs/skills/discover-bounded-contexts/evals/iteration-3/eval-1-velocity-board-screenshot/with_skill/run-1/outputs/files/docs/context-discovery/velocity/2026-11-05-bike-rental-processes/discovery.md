# Bounded context discovery: VeloCity (bike rental processes board)

- **Date:** 2026-11-05
- **Mode:** discover
- **Status:** contexts approved
- **Input:** `velocity/velocity-processes.jpg` (event-storming board, "Bike Rental Processes")
- **Context map:** not requested (expert: "No context map for now. Let's stop at the contexts today.")

The user is the trainer, so the reference solution was not treated as a spoiler, but it was not used as an answer key either: every boundary below comes from the expert's answers in this session.

## Reading of the input
- **Main timeline:** User Registration And Account Setup: User Registered → Email Confirmed → Phone Number Verified → Basic Data Provided → Payment Method Added → Payment Method Verified → Agreement Signed → **User Account Activated**. Then Bike Rental: User Location Validated → Bike Reserved → Bike Unlocked → **Rental Session Started** → bike returned; branches Bike Unlock Failed → Bike Reservation Expired, and Bike Reservation Expired.
- **Pricing lane:** from Rental Session Started: free period started/ended, 1st-3rd hour billing periods with "Hour Fee Applied", 4th-and-subsequent period → Hourly Fee Applied → Rental Total Fee Calculated, or Max Rental Time Exceeded → Penalty Applied.
- **Return lanes:** Station (Locked In Station → Fault Detected → Fault Reported; Locking Failed → Customer Support Contacted / Fault Reported) and Zone (Drop Zone Accepted → Drop Zone Fee Applied; Unauthorized Zone Detected → Unauthorized Zone Return Fee Applied).
- **Disconnected clusters:** Account Block And Debt Recovery; Customer Support And Incident Handling.
- **Ambiguous:** "bike broken" feeding Bike Reserved; "maintenance started → bike fixed → bike returned" after Bike Unlocked; duplicated Rental Session Started / bike returned / Account Blocked stickies; nothing connects the return to a payment, or a payment to the debt lane.
- **Expert corrections:** duplicated stickies are the same events (R1.5). "User Location Validated" is the per-rental "is this rider still allowed to rent?" check, not a location check (R3.5). The "bike broken" branch "is messy": a broken bike is never reserved or unlocked; it goes to maintenance (R3.3).

## Heuristic trail
### H1: Pivotal events
| Event | Status | Before | After | Expert's answer |
|---|---|---|---|---|
| User Account Activated | confirmed | onboarding (one-off) | renting (per-rental eligibility check) | R1.1: activated once a payment method is added; validation runs before every rental |
| Bike Reserved | rejected | - | - | R1.2: "Reservation isn't a phase of its own" |
| Rental Session Started | confirmed (phase change, **not a boundary**) | validation, reservation, unlock | billable ride | R1.2: ride begun, price calculation starts; "Validation, reservation and unlock have no rules, limits or fees of their own" |
| Rental Session Ended (missing; return or forced end) | confirmed | ride | settlement (real charge) | R1.3: charged on return or forced end; R2.1 settlement has its own rules |
| Rental Total Fee Calculated | rejected as pivot | - | - | R1.3: estimate and final charge use the same pricing rules |
| Rental Unsettled (missing) | confirmed hand-off | settlement | debt recovery + account block | R1.5, R3.4: the unsettled payment starts recovery and is the block reason |

Empty side found: after Rental Session Ended, Pricing calculates and Payments executes, but nobody decided when to charge/retry/give up. The expert confirmed those rules exist (R2.1), so **Settlement** owns the far side.

### H2: Swimlanes
| Flow | Verdict | Why |
|---|---|---|
| Price estimation during the ride | lane, owned by **Rental** (clock) calling **Pricing** (calculation) | Repeats on its own clock; R3.1: "The rental side keeps the time. It asks the pricing calculator…" |
| Station vs zone return | variant | R1.3: same rules, extra fee for an unauthorized zone |
| Maintenance | lane | R1.4, R2.5: runs on its own, own schedule; follows the bike |
| Incident handling | lane | own loops (escalate/reject/reopen); R2.5 nothing flows back |
| Debt recovery | lane | long-running, pause/resume, own rules (R2.3) |

### H3: Single responsibility
| Duplicated responsibility | Where it appeared | Resolution |
|---|---|---|
| Estimate vs final price | Pricing lane (hourly fees) and Rental Total Fee Calculated | One **Pricing** context (R1.3, same rules); stays separate because it "changes independently… a simple calculator that any other part of the business can use" (R2.5) |
| Preparation vs ride | Bike Rental lane, both sides of Rental Session Started | Merged into **Rental**: preparation has no rules (R1.2) |
| Account blocking | onboarding (unverified payment method) and debt lane (unsettled payment) | **Rider Accounts** is the single owner of block/unblock (R2.4); others publish facts |
| Payment vs settlement vs recovery | "Account Block And Debt Recovery" lane, implicit charge | Payments has no rules (R2.2) → mechanical; Settlement owns charge/retry/give-up (R2.1); Debt Recovery owns recovery actions/schedule (R2.3) |
| Ride clock / estimate lane | Pricing lane vs "stateless calculator" | Rental owns the clock and estimate trigger; Pricing has no clock (R3.1). Billing-period stickies are tariff detail inside Pricing's calculation, not Rental events |
| Max rental time | Pricing lane (penalty) and Rental (forced end) | "One limit, used by both sides" (R3.2): **Rental** owns the limit and the forced end; Pricing receives "exceeded" as an input and owns only the penalty amount (no copy of the limit) |
| Bike serviceability | "bike broken" (Bike Rental lane), Bike Fault Detected (Rental), bike fixed (maintenance) | **Fleet Maintenance** decides out of service / back in service (R3.3); Rental only rents available bikes and publishes faults to Incident Handling |
| "Is this rental paid?" after recovery | Settlement and Debt Recovery | **Settlement** stays the single owner; it marks the rental settled when recovery succeeds (R3.4) |

## Bounded contexts
| Context | Question it answers | Owns (events, decisions, rules) | Heuristic justification | Classification |
|---|---|---|---|---|
| Rider Accounts | Can this rider use the service? | User Registered … User Account Activated; Account Blocked (reasons: payment method not verified in time, rental unsettled); Account Unblocked; Account Block Notification Sent; answers the per-rental eligibility query (Rider Eligibility Confirmed) | H1 activation pivot; H3 single owner of blocking (R2.4) | unknown |
| Rental | Who has which bike, and when? | eligibility check (asks Rider Accounts); Bike Reserved / Reservation Expired; Bike Unlocked / Unlock Failed; Rental Session Started; ride clock and Fee Estimated (via Pricing); max-rental-time limit, Max Rental Time Exceeded, forced end; Bike Returned To Station / To Zone; Locked / Locking Failed; Drop Zone Accepted / Unauthorized Zone Detected; Bike Fault Detected / Reported (published); Rental Session Ended | H1: session start is a phase change but not a boundary (no preparation rules, R1.2); H2 returns are variants; owns the clock (R3.1) and the limit (R3.2) | unknown |
| Pricing | What does this cost? | free period, hourly fees, overtime penalty amount, unauthorized-zone fee (R4.1), drop-zone fee (unconfirmed); serves running estimates and the final total (Rental Total Fee Calculated) with the same rules; no clock | H3 one pricing question (R1.3); changes independently, usable by anyone (R2.5) | unknown |
| Settlement | Is this rental paid for? | reacts to Rental Session Ended; gets the total from Pricing; orders charges from Payments; charge/retry/give-up schedule; Rental Settled / Rental Unsettled; marks settled after successful debt recovery | H1 empty side of the session-ended pivot; own simple schedule rules (R2.1) | supporting (simple rules) |
| Payments | Did the charge go through? | executes charges and payment-method verification against the external payment processor; Payment Succeeded / Failed | H3 rule-ownership test: "payment has no rules" (R2.2) → mechanical | likely generic |
| Debt Recovery | How do we recover what's owed? | starts on Rental Unsettled; Debt Recovery Process Started / Paused / Resumed / Succeeded / Failed; which actions, schedule, pause/resume | H2 own long-running lane; H3 own rules (R2.3) | unknown |
| Incident Handling | What went wrong, and is it resolved? | Incident Reported / Type Detected / Assigned To Agent / Accepted / Rejected / Escalated / Reopened / Resolved; Customer Support Contacted; subscribes to Bike Fault Reported | H2 lane with own loops; nothing flows back (R2.5) | unknown |
| Fleet Maintenance | Is this bike fit to ride, and when is it serviced? | Bike Taken Out Of Service ("bike broken"), Bike Maintenance Started, Bike Fixed, Bike Returned To Service; own schedule | H2 follows the bike, own schedule (R1.4, R2.5); H3 single owner of serviceability (R3.3) | supporting |

External parties (not contexts): the payment processor; the city's zone map (assumption, not confirmed).

## Critic log
| Subject | Round | Verdict | Blocking points / resolution |
|---|---|---|---|
| Rider Accounts | 1 | AGREE | none; notes: what unblocks a rider (answered R3.4), is a ban just a block reason |
| Rental | 1 | AGREE | none; notes: what forces an end (answered R3.2), unowned "bike broken" (answered R3.3), "User Location Validated" naming (answered R3.5) |
| Pricing | 1 | DISAGREE | "Stateless calculator" contradicted owning the ticking estimate lane; max-time ownership unclear. Resolved by asking the expert (R3.1, R3.2) |
| Settlement | 1 | AGREE | none; note: does successful recovery settle the rental (answered R3.4) |
| Payments | 1 | AGREE | none; note: add Payment Succeeded/Failed |
| Debt Recovery | 1 | AGREE | none; notes: trigger (answered R3.4), what follows Debt Recovery Process Failed (open) |
| Incident Handling | 1 | AGREE | none; note: is Customer Support Contacted the same as Incident Reported (open) |
| Fleet Maintenance | 1 | DISAGREE | Serviceability decision had no single owner; "bike broken" unowned. Resolved by asking the expert (R3.3) |
| Pricing | 2 | AGREE | blocker resolved; Pricing must not keep its own copy of the limit |
| Fleet Maintenance | 2 | AGREE | blocker resolved; open: how maintenance learns a bike is broken |
| Rental | 2 | AGREE | re-reviewed because its ownership grew; billing-period stickies must stay tariff detail in Pricing |

## Context map
Not requested in this session (R4.2).

## Q&A with the domain expert

### Round 1

**Facilitator:**

> Thanks. Since you're the trainer, I won't treat the reference map as off-limits, but your answers still decide the boundaries. I've started a discovery report at `docs/context-discovery/velocity/2026-11-05-bike-rental-processes/discovery.md`.
> 
> **How I read the board** (correct me before we build on it):
> - **Main timeline:** User Registration (email, phone, basic data, payment method added/verified, agreement signed) → **User Account Activated**. Then Bike Rental: User Location Validated → Bike Reserved → Bike Unlocked → **Rental Session Started** → bike returned. Branches: Bike Unlock Failed / Bike Reservation Expired.
> - **Pricing lane:** starts again from Rental Session Started; free period, then hourly billing periods with "Hour Fee Applied", ending in Rental Total Fee Calculated, or Max Rental Time Exceeded → Penalty Applied.
> - **Returns:** two lanes. Station (locked, or locking failed → Customer Support Contacted, with Bike Fault Detected/Reported) and Zone (drop zone accepted → fee, or unauthorized zone → fee).
> - **Disconnected clusters:** Account Block & Debt Recovery (blocked for invalid card or for debt → notification → recovery started/paused/resumed/succeeded/failed), and Incident Handling (reported → typed → assigned → accepted/rejected/escalated/reopened → resolved).
> - **Ambiguous:** "bike broken" feeding Bike Reserved; a "maintenance started → bike fixed → bike returned" branch after Bike Unlocked; Rental Session Started and "bike returned" appear twice; nothing links the return to an actual payment, or a payment to the debt lane.
> 
> **H1: candidate pivotal events.** User Account Activated, Bike Reserved vs Rental Session Started, the return events, Rental Total Fee Calculated, and the triggers of the two disconnected clusters.
> 
> Questions:
> 1. What does **User Account Activated** mean, and how does it differ from **User Location Validated**? (Decides whether onboarding and renting are separate phases.)
> 2. Which is the real phase change: **Bike Reserved** or **Rental Session Started**? Does reservation have its own rules, limits or fees? (Decides whether reservation is its own phase.)
> 3. What happens at the return? Are the "Hour Fee Applied" stickies actual charges, or estimates, and when is the rider really charged? (Decides where the ride ends and what follows it.)
> 4. Where do faults lead: "bike broken", "maintenance started → bike fixed", Bike Fault Reported? Into incidents, into a maintenance flow, or somewhere else? (Decides whether the fault branches belong to renting.)
> 5. What starts the debt lane, and is it connected to a failed payment? And are the duplicated Rental Session Started / bike returned stickies the same events? (Decides the trigger, and the owner, of the disconnected clusters.)

**Expert:**

> 1. The account is activated once the user has added a payment method. User Location Validated is a different thing. Before every rental we check the user is still allowed to rent, so a blocked or banned user can't rent even though the account was activated earlier.
> 
> 2. Rental Session Started. It means the renter has rented the bike, the ride has begun and we can start calculating the price. Reservation isn't a phase of its own. It just lets you hold a bike before renting it. Validation, reservation and unlock have no rules, limits or fees of their own.
> 
> 3. The "Hour Fee Applied" stickies are only an estimate we show the rider during the ride. The rider is actually charged when they return the bike, or when the session is forcibly ended. The estimate and the final charge use the same pricing rules. Station and zone returns follow the same rules, except there's an extra fee if the bike is left in an unauthorized zone.
> 
> 4. Broken bikes and faults most likely go to incidents. A rider can report a bike broken when returning it, at a station or in a zone. Maintenance runs on its own, separately from rentals.
> 
> 5. An account gets blocked in two cases: a payment wasn't settled, or the payment method wasn't verified in time. So yes, an unsettled payment can start the debt lane. And yes, the duplicated stickies (Rental Session Started twice, Account Blocked twice) are the same events. They just show up in different flows.

### Round 2

**Facilitator:**

> Thanks, that moves a lot.
> 
> **Confirmed pivots**
> - **User Account Activated** (once, after a payment method is added): onboarding before, renting after. The per-rental check (User Location Validated) sits on the renting side.
> - **Rental Session Started**: the ride becomes billable. Validation, reservation and unlock have no rules of their own, so they're steps of renting, not a separate phase. (H3 will likely fold them into the ride context.)
> - **The return / forced end**: the ride ends and the real charge happens. The board has no event for this, so I suggest adding **Rental Session Ended** (with a forced-end variant), plus **Payment Succeeded / Payment Failed**.
> - Board rename: the "Hour Fee Applied" stickies are estimates, so **"Hour Fee Estimated"**.
> 
> **H2: swimlanes**
> - **Price estimation** ticks on its own clock during the ride. It's a lane, but it shares rules with the final charge (one pricing question; to check in H3).
> - **Station vs zone returns**: same rules plus one extra fee, so they're **variants of one return**, not lanes.
> - **Maintenance**: runs on its own, so a lane.
> - **Incidents**: their own loops (escalate, reject, reopen), so a lane.
> - **Debt recovery**: long-running, with pause/resume, so a lane.
> 
> The ended-session pivot now has an empty side: after the ride ends, I can see who prices it, but not who turns that into a charge and decides it's settled or not.
> 
> Questions:
> 1. After a session ends, who decides what the rider owes for that rental and whether it's settled: when to charge, whether to retry, when to give up? Are there rules there, and whose are they? (Decides whether something owns the step between the ride and the money.)
> 2. Does the payment step itself have any rules, or does it just do what it's told and report success or failure? (Decides whether Payments is mechanical, and where rule-driven work goes.)
> 3. Does debt recovery have its own rules, like which actions to take, a schedule, when to pause or resume? (Decides whether debt recovery is its own context or part of payments.)
> 4. An account is blocked for two reasons (unsettled payment, unverified payment method). Who decides "this rider can't use the service right now": one owner whatever the reason, or each department its own block? (Decides who owns blocking.)
> 5. Do pricing rules change independently of renting, maintained by different people or used elsewhere? And when an incident is resolved, does anything flow back (a refund, a maintenance order, an unblock), and does a reported fault feed maintenance? (Decides whether Pricing stands alone, and how Incidents and Maintenance connect.)

**Expert:**

> Domain expert answers (round 2):
> 
> 1. Yes, settlement has rules of its own. They're not very complex and are mostly about the schedule: when to charge, when to retry, when to give up. They don't depend on the rental itself.
> 
> 2. No, payment has no rules. It does what it's told and reports success or failure.
> 
> 3. Yes, debt recovery has its own rules: which actions to take, and the schedule for them, including when to pause and resume.
> 
> 4. [improvised] There's one place that blocks and unblocks the rider, whatever the reason. The reasons come from different departments: an unsettled payment comes from the money side, and an unverified payment method comes from onboarding.
> 
> 5. Yes, pricing changes independently of renting. It's just a simple calculator that any other part of the business can use. When an incident is resolved, nothing flows back to the other flows. On faults: when a rider reports a fault at the return, the rental side checks it and publishes an event, incident handling picks that up, and the rental side knows nothing more about it. [improvised] A reported fault doesn't feed maintenance directly. Maintenance runs on its own schedule.

### Round 3

**Facilitator:**

> Thanks. Your answers gave me an 8-context draft, and I had an independent critic review each context before showing it to you. Six held up. Two need facts only you have, and they're below.
> 
> **Draft (one question each)**
> 1. **Rider Accounts:** Can this rider use the service? Onboarding through activation, plus the single owner of block/unblock, whatever the reason.
> 2. **Rental:** Who has which bike, and when? Validation, reservation, unlock, session, returns (station and zone are variants), fault reports.
> 3. **Pricing:** What does this cost? One set of rules for the estimate and the final total.
> 4. **Settlement:** Is this rental paid for? It turns an ended session into a charge, with its own charge/retry/give-up schedule. This fills the empty side of the session-ended pivot.
> 5. **Payments:** Did the charge go through? Mechanical, no rules, wraps the external processor.
> 6. **Debt Recovery:** How do we recover what's owed? Own actions and schedule.
> 7. **Incident Handling:** What went wrong, and is it resolved? It subscribes to fault reports, and nothing flows back.
> 8. **Fleet Maintenance:** Is this bike in working order? Own schedule.
> 
> **What the critics found**
> - **Pricing:** you called it "a simple calculator anyone can use", but the board's Pricing lane ticks along during the ride (billing periods, hourly estimates). A calculator can't keep a clock, so someone else must own that lane.
> - **Fleet Maintenance:** nobody owns the decision that takes a bike out of service or puts it back, and the "bike broken" sticky belongs to no context.
> 
> Questions:
> 1. During a ride, who keeps track of time and decides when to show a new estimate: the pricing side itself, or the rental side asking the pricing calculator "what would this ride cost now"? (Decides who owns the estimate lane.)
> 2. Is the maximum rental time a rental rule (when we force the ride to end), a pricing rule (when the penalty starts), or one limit used by both? Is there anything else that can force a session to end? (Decides who owns the limit and the forced end.)
> 3. On the board, "bike broken" leads to Bike Reserved → Bike Unlocked → maintenance started → bike fixed. What does that branch mean? And who decides that a bike can't be rented, and that it can be rented again after a fix? (Decides who owns a bike's serviceability.)
> 4. What starts debt recovery: the unsettled rental itself, or the account block? And when recovery succeeds, is the rider unblocked automatically, and does the rental count as settled? (Decides the hand-offs between Settlement, Debt Recovery and Rider Accounts.)
> 5. Is "User Location Validated" really a location check (is the rider near the bike?), or is it the "is this rider still allowed to rent?" check? (Decides whether Rental checks it itself or asks Rider Accounts.)

**Expert:**

> Domain expert answers (round 3):
> 
> 1. [improvised] The rental side keeps the time. It asks the pricing calculator "what would this ride cost now?" and shows the rider the result. The pricing side doesn't keep any clock. It just calculates.
> 
> 2. [improvised] It's one limit, used by both sides. When the ride passes it, the rental side forces the session to end, and pricing adds the penalty using its own rules. I can't think of anything else that forces a session to end.
> 
> 3. [improvised] That branch on the board is messy. A broken bike shouldn't be reserved or unlocked at all. It goes to maintenance. Maintenance decides when a bike is out of service and when it can be rented again after a fix. The rental side only rents out bikes that are available.
> 
> 4. [improvised] The unsettled payment starts it. That same unsettled payment is also why the account gets blocked. When recovery succeeds, the rental counts as paid and the rider is unblocked automatically.
> 
> 5. It's the "is this rider still allowed to rent?" check. We check the account before every rental, so a blocked or banned rider can't rent.

### Round 4

**Facilitator:**

> Your answers settled both open points, and the critics now agree with all eight contexts.
> 
> **What moved**
> - **Pricing** stays a stateless calculator. **Rental** owns the ride clock, asks Pricing "what would this cost now?", owns the max-time limit and forces the end. Pricing only owns the penalty amount, so the limit has one owner.
> - **Fleet Maintenance** now owns a bike's serviceability, including "bike broken". Rental only rents available bikes.
> - **Settlement** marks a rental paid when debt recovery succeeds. **Rider Accounts** unblocks the rider in reaction.
> 
> **The contexts**
> 
> | Context | Question | Owns | Why it's separate |
> |---|---|---|---|
> | Rider Accounts | Can this rider use the service? | onboarding → Account Activated; block/unblock for every reason; the eligibility answer | Activation pivot; H3 single owner of blocking |
> | Rental | Who has which bike, and when? | eligibility check (asks Accounts), reserve, unlock, session, ride clock and estimate trigger, max-time limit and forced end, station/zone returns, fault reports | Session-start pivot isn't a boundary (no preparation rules); returns are variants |
> | Pricing | What does this cost? | free period, hourly fees, overtime penalty, zone fee; estimate and total by the same rules | Changes independently; usable by anyone |
> | Settlement | Is this rental paid for? | charge/retry/give-up schedule; Rental Settled/Unsettled | Fills the empty side of the session-ended pivot; its own rules |
> | Payments | Did the charge go through? | executing charges and payment-method checks via the external processor | Mechanical, no rules |
> | Debt Recovery | How do we recover what's owed? | recovery actions and schedule, pause/resume | Own lane, own long clock, own rules |
> | Incident Handling | What went wrong, and is it resolved? | incident lifecycle; subscribes to fault reports | Own lane with loops; nothing flows back |
> | Fleet Maintenance | Is this bike fit to ride, and when is it serviced? | out of service / fixed / back in service | Follows the bike, not the rental; own schedule |
> 
> External parties: the payment processor, and the city's zone map (an assumption).
> 
> **Open assumptions**: some answers were improvised on the spot (the clock, the limit, the maintenance branch, recovery → unblock). I've flagged them. Also: is the drop-zone fee real? And how does maintenance learn a bike is broken, if fault reports don't feed it?
> 
> **Suggested board changes**
> - Add Rental Session Ended (incl. forced), Payment Succeeded/Failed, Rental Settled/Unsettled, Account Unblocked.
> - Rename "… Fee Applied" to "Fee Estimated"; rename "User Location Validated" to "Rider Eligibility Confirmed".
> - Fold the billing-period stickies into Fee Estimated. They're tariff detail, not events.
> - Move "bike broken → … → bike fixed" out of Bike Rental into a Fleet Maintenance lane (Bike Taken Out Of Service → Maintenance Started → Fixed → Returned To Service).
> 
> Questions:
> 1. Do you approve these eight contexts?
> 2. Do you want a context map in Context Mapper (.cml) format?

**Expert:**

> Domain expert answers (round 4):
> 
> 1. Yes, I approve the eight contexts. On your open point: the zone fee is real. A bike returned in an unauthorized zone costs extra.
> 
> 2. No context map for now. Let's stop at the contexts today. We'll do canvases in the next session too.

## Open questions and assumptions
- The expert marked these answers as improvised on the spot; confirm them before they become code: R2.4 (one place blocks/unblocks), R2.5 second half (faults don't feed maintenance), R3.1 (Rental keeps the ride clock), R3.2 (one max-time limit, Rental forces the end), R3.3 (maintenance owns serviceability), R3.4 (unsettled rental starts recovery; success settles the rental and unblocks the rider).
- Is the drop-zone fee real? The expert confirmed only the extra fee for an unauthorized zone (R1.3, R4.1).
- How does Fleet Maintenance learn that a bike is broken, if fault reports don't feed it directly?
- What follows Debt Recovery Process Failed (write-off, external collector, permanent ban)?
- Is Customer Support Contacted (after Bike Locking Failed) the same as Incident Reported?
- Is a "ban" just another block reason in Rider Accounts?
- The city's zone map as an external party is an assumption.

## Suggested board changes
- Add **Rental Session Ended** (including a forced-end variant), **Payment Succeeded / Payment Failed**, **Rental Settled / Rental Unsettled**, **Account Unblocked**.
- Rename "First/Second/Third Hour Fee Applied" and "Hourly Fee Applied" to **Fee Estimated**; fold the free-period and billing-period stickies into it (tariff detail, not events).
- Rename "Penalty Applied" / "… Zone Fee Applied" into lines of the calculated total: nothing is applied until Settlement charges.
- Rename "User Location Validated" to **Rider Eligibility Confirmed**.
- Move "bike broken → … → maintenance started → bike fixed → bike returned" out of the Bike Rental lane into a Fleet Maintenance lane: **Bike Taken Out Of Service → Bike Maintenance Started → Bike Fixed → Bike Returned To Service**.
- Split the "Account Block And Debt Recovery" lane: the block events belong to Rider Accounts, the recovery events to Debt Recovery.

## Next steps
- Context map: offered and declined for today (R4.2). Ask again next session.
- Canvases: deferred to the next session at the expert's request (R4.2). Then hand each context to `bounded-context-scaffolder` for `docs/bounded-contexts/velocity/<context-package>/canvas.md` (canvases only).
