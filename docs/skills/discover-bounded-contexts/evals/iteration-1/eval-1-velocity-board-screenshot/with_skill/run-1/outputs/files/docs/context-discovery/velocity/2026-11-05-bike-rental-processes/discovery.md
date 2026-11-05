# Bounded context discovery: VeloCity (Bike Rental Processes board)

- **Date:** 2026-11-05
- **Mode:** discover
- **Status:** contexts approved
- **Input:** `velocity/velocity-processes.jpg` (event-storming board "Bike Rental Processes")
- **Context map:** not requested this session (deferred to the next session)

## Reading of the input
Main timeline (left column):
- *User Registration and Account Setup*: User Registered → User Email Confirmed → User Phone Number Verified → User Basic Data Provided → User Payment Method Added → User Payment Method Verified → User Agreement Signed → User Account Activated.
- *Bike Rental*: (User Location Validated | bike broken) → Bike Reserved → Bike Unlocked → Rental Session Started → bike returned; Bike Unlocked → maintenance started → bike fixed → bike returned; Bike Unlock Failed → Bike Reservation Expired; Bike Reservation Expired.
- *Pricing*: Rental Session Started → First Free Period Started/Ended → First/Second/Third Hour Billing Period Started → First/Second/Third Hour Fee Applied → Fourth Hour And Subsequent Billing Period Started → Hourly Fee Applied → Rental Total Fee Calculated | Max Rental Time Exceeded → Penalty Applied.
- *Bike Station Return Handling*: Bike Returned To Station → Bike Locked In Station → Bike Fault Detected → Bike Fault Reported; Bike Locking Failed → Customer Support Contacted / Bike Fault Reported.

Branch: *Bike Zone Return Handling*: Bike Returned to Zone → Bike Return To Drop Zone Accepted → Drop Zone Fee Applied | Bike Return To Unauthorized Zone Detected → Unauthorized Zone Return Fee Applied.

Disconnected clusters:
- *Account Block and Debt Recovery*: Account Blocked Due To Invalid Card → Account Block Notification Sent; Account Blocked Due To Debt → Account Block Notification Sent → Debt Recovery Process Started → Failed | Succeeded | Paused → Resumed → Succeeded | Failed.
- *Customer Support and Incident Handling*: Incident Reported → Incident Type Detected → Incident Assigned To Agent → Accepted (→ Escalated → Resolved | Resolved) | Rejected | Rejected → Reopened → Resolved.

Ambiguities raised, and the expert's corrections:
- "bike broken" is a **guard** (a broken bike can't be reserved), not a trigger.
- The lowercase "maintenance started → bike fixed → bike returned" branch is a **mechanic's** work, not part of a rider's rental; the mechanic's unlock is a separate maintenance action that opens no rental session.
- Duplicated stickies (Bike Reservation Expired, Account Block Notification Sent, Debt Recovery Process Succeeded/Failed) are the **same events**, appearing in two flows.
- Nothing on the board charged the rider or linked the debt cluster to the timeline; the expert filled these gaps (see Q&A).
- Onboarding order: the expert says activation happens once, when the payment method is added, and the payment method must be verified "in time" afterwards. The board puts verification and agreement before activation. Suggested reorder below.

The expert called the reading "mostly right".

## Heuristic trail
### H1: Pivotal events
| Event | Status | Before | After | Expert's answer |
|---|---|---|---|---|
| User Account Activated | confirmed | who the user is: identity, contacts, payment method, agreement | bikes and rides | R1.1: activated once, when a payment method is added; validated again before every rental |
| Bike Reserved | rejected | location check | unlock, ride | R1.2: reservation isn't a phase of its own; preparation has no extra rules |
| Rental Session Started | confirmed (phase change, **not a boundary**, see H3) | preparation (validate, reserve, unlock) | the ride, price calculation | R1.2: renter has a bike, ride begun, price calculation can start |
| Rental Session Ended (missing on board; return to station/zone or forced end) | confirmed | the ride | the real charge | R1.3/R1.4: the real payment happens at the return or forced end; R3.5: max time exceeded forces the end |
| Rental Total Fee Calculated | rejected as pivot | | | only a calculation; the far side (charging) needed an owner, which became Settlement |
| Rental Unsettled (missing on board) | confirmed hand-off | settlement's retry schedule | debt recovery, account block | R1.5: Blocked Due To Debt comes from an unsettled payment; R3.3/R3.4 |
| Bike Fault Reported | confirmed hand-off | the return (Rental) | incident handling (rider side) and maintenance (bike side) | R1.6, R3.1 |

### H2: Swimlanes
| Flow | Verdict | Why |
|---|---|---|
| Price estimation during the ride | lane (merged with the final price under H3) | ticks on its own clock; R1.3 estimates only, same rules as the final charge |
| Station return vs zone return | variant | R1.4: same rules, extra fee for an unauthorized zone |
| Maintenance (mechanic) | lane | R1.7, R2.1: own flow, follows the bike, independent of rentals |
| Incident handling | lane | R1.6, R2.7: several triggers, own loops and rules, support agents, nothing flows back |
| Debt recovery | lane | disconnected, runs over days, pauses/resumes; R2.2 own rules (actions, schedule) |
| Settlement (charge + retries) | lane after the ride | R2.5, R3.4: own schedule rules, independent of the rental |

### H3: Single responsibility
| Duplicated responsibility | Where it appeared | Resolution |
|---|---|---|
| Account blocking | onboarding (invalid card) and debt recovery (debt) | Merging the two would break the *User Account Activated* pivot, so ownership moved: Rider Accounts is the single owner of every block/unblock; others publish reasons (R2.4) |
| Price | in-ride estimate and final total | one question, same rules (R1.3) → one Pricing context; changes independently and usable by anyone (R2.6) → separate from Rental |
| Preparation vs ride | Rental Session Started pivot | preparation has no rules of its own (R1.2) → one Rental context |
| Payment vs debt recovery | "Debt Recovery" next to payment | payment has no rules (R2.3), debt recovery has its own (R2.2) → two contexts |
| Who settles? | nobody: Pricing calculates, Payments executes, Rental ends at the pivot | empty side of *Rental Session Ended* → Settlement, own simple schedule rules (R2.5) |
| Bike out of service | Fleet Maintenance vs Incident Handling (critic, round 1) | R3.1: Fleet Maintenance decides; both hear *Bike Fault Reported* directly from Rental |
| Unlock | rider unlock vs mechanic unlock | R3.2: separate maintenance action, no rental session |
| Retrying a failed charge | Settlement vs Debt Recovery | R3.4: Settlement retries within its schedule; *Rental Unsettled* hands over to Debt Recovery |
| Max rental time | Pricing lane vs Rental | R3.5: forced end → Rental owns the limit and the forced end; Pricing owns the penalty amount |

## Bounded contexts
| Context | Question it answers | Owns (events, decisions, rules) | Heuristic justification | Classification |
|---|---|---|---|---|
| Rider Accounts | Can this rider use the service right now? | User Registered … User Account Activated; payment-method verification deadline; Account Blocked (any reason), Account Unblocked, Account Block Notification Sent; eligibility answer before each rental | H1 *User Account Activated*; H3 single owner of blocking (R2.4) | not discussed |
| Rental | Who has which bike, and when? | User Location Validated (combines eligibility + service status), Bike Reserved, reservation expiry, Bike Unlocked / Unlock Failed, Rental Session Started, station/zone returns (locked, locking failed, drop zone accepted, unauthorized zone detected), max rental time + forced end, Rental Session Ended, Bike Fault Detected/Reported, Customer Support Contacted | H1 between *User Account Activated* and *Rental Session Ended*; H3 preparation has no own rules; H2 station/zone are variants | not discussed |
| Pricing | What does this cost? | Tariff: free period, hourly fees, 4th-hour+ fee, max-time penalty, drop-zone and unauthorized-zone fees; in-ride estimate and final total | H2 own clock; H3 one question; changes independently (R2.6) | not discussed (critic: supporting/generic) |
| Settlement | What does the rider owe for this rental, and is it settled? | reacts to Rental Session Ended; gets the total; requests the charge; charge/retry schedule; Rental Settled / Rental Unsettled | H1 far side of *Rental Session Ended*; H3 own schedule rules (R2.5, R3.4) | supporting (simple rules) |
| Payments | Did the charge go through? | executes charges and payment-method verifications against the processor; Payment Succeeded / Payment Failed; no business rules | H3 "Payment knows nothing about any rules" (R2.3, R3.6) | not discussed (critic: generic) |
| Debt Recovery | How do we recover what's owed? | Debt Recovery Process Started / Paused / Resumed / Succeeded / Failed; which actions, on what schedule; started by Rental Unsettled | H2 disconnected, long timeline; H3 own rules (R2.2, R3.3) | not discussed |
| Fleet Maintenance | Is this bike in working order? | the bike's service status (Bike Taken Out Of Service, Bike Back In Service); reacts to Bike Fault Reported; Maintenance Started, mechanic's own unlock/return, Bike Fixed | H2 follows the bike, own clock (R2.1); H3 single owner of "out of service" (R3.1) | not discussed |
| Incident Handling | What went wrong, and is it resolved? | Incident Reported, Type Detected, Assigned To Agent, Accepted, Rejected, Escalated, Reopened, Resolved; support agents' rules; reacts to Bike Fault Reported and Customer Support Contacted; nothing flows back | H2 several triggers, own loops (R1.6, R2.7); H3 own people and rules | not discussed |

External parties (not designed by us): **Payment Processor**; **City** providing the zone map (assumption, not confirmed).

## Critic log
| Subject | Round | Verdict | Blocking points / resolution |
|---|---|---|---|
| Rider Accounts | 1 | AGREE | none; notes: confirm onboarding order, add Account Unblocked and a verification-deadline event |
| Rental | 1 | AGREE | none; questions on max time (answered R3.5) and fault consumers (answered R3.1) |
| Pricing | 1 | AGREE | none; notes: Rental Total Fee Calculated better recorded by Settlement; who drives the estimate clock (map question) |
| Settlement | 1 | AGREE | none; question on who owns retries (answered R3.4) |
| Payments | 1 | AGREE | none; question on payment-method verification (answered R3.6) |
| Debt Recovery | 1 | AGREE | none; question on what starts recovery (answered R3.3) |
| Fleet Maintenance | 1 | DISAGREE | "bike out of service" had no confirmed single owner and nothing started maintenance → put to the expert (R3.1, R3.2); added *Bike Taken Out Of Service*, separated the mechanic's unlock |
| Incident Handling | 1 | AGREE | none; question on who decides out of service (answered R3.1) |
| Fleet Maintenance | 2 | AGREE | resolved by R3.1/R3.2 |
| Incident Handling | 2 | AGREE | re-checked because the Fleet Maintenance fix touched it; still holds |

Rounds that changed something: Fleet Maintenance round 1 (new event, ownership of service status, separate maintenance unlock).

## Context map
Not requested this session. The expert deferred it to the next session.

Prepared for that session (questions already asked, answers deferred):
- Processes to test the map against: Register and activate an account; Rent a bike (validate, reserve, unlock, ride, return to station/zone or forced end); Settle a rental (price, charge, retries, settled/unsettled); Recover a debt; Handle an incident; Maintain a bike.
- Team setup: one organisation with internal teams, or bought/outsourced parts?
- Is the payment processor's API a well-established standard (Conformist candidate) or do we translate (ACL)?
- Is the zone map provided by the city?
- File name: `src/main/cml/VeloCity-Context-Map.cml` exists (the reference solution) and must not be overwritten; proposed `src/main/cml/VeloCity-Context-Map-board-discovery.cml`.
- Critic hint for the map: Rental → Fleet Maintenance (Bike Fault Reported) and Fleet Maintenance → Rental (service status) are both real dependencies on different models; check for a U/D cycle and justify or break it.

## Q&A with the domain expert
### Round 1 (reading + H1)
Questions: (1) Account Activated vs Location Validated; (2) meaning of Rental Session Started, is reservation a phase; (3) are the hourly fees charges or estimates, when is the real charge; (4) what a return triggers, what happens with Rental Total Fee Calculated; (5) what triggers the two account blocks; (6) where Bike Fault Reported / Customer Support Contacted lead; (7) "bike broken" and the maintenance branch; (8) duplicated stickies.

Answers:
1. "An account is activated once, when the user adds a payment method. Before every rental the account is validated again, so blocked or banned users can't rent."
2. "Rental Session Started means the renter has a bike, the ride has begun and price calculation can start. Reservation isn't a phase of its own. It only lets you reserve before the rental, and preparing a rental (validation, reservation, unlock) has no extra rules."
3. "The hourly fee stickies are only an estimate shown to the rider for convenience. The estimate and the final charge use the same pricing rules. The real payment happens when the rider returns the bike or is forced to end the session."
4. "Yes, the return ends the session, and that is when the rider actually pays. Station and zone returns follow the same rules, except there are extra fees for returning to an unauthorized zone."
5. "There are two causes. 'Due To Debt' comes from an unsettled payment. 'Due To Invalid Card' means the payment method wasn't verified in time."
6. "Faults lead into incident handling. On return, the rental side checks for a fault and publishes an event, and incident handling picks it up; rental doesn't know what happens next. Incidents are reported by riders or detected automatically, and nothing flows back once they're resolved. Customer Support Contacted also goes into incident handling."
7. "Maintenance is its own flow and runs separately from rentals. The only link is that a rider can report a bike broken when returning it to a station or zone. The 'bike broken' sticky is a guard: a broken bike can't be reserved. The maintenance branch is a mechanic's work, not part of the rider's rental."
8. "Yes, each duplicate is the same event. It appears twice because it's part of two different flows."

### Round 2 (H2)
Questions: (1) maintenance independence; (2) debt recovery rules; (3) payment rules; (4) who blocks; (5) who settles; (6) pricing independence; (7) incidents' own people and rules.

Answers:
1. "Maintenance runs on its own, independently of rentals. The only link is that a bike can be reported broken when it's returned to a station or zone. Maintenance decides when a bike is fixed and back in service. Only the bike's availability goes back to renting."
2. "Debt recovery has its own rules: which actions to take, and on what schedule."
3. "No. Payment knows nothing about any rules."
4. "There's one owner, the account side. It decides whether a rider can use the service right now. The other areas only report the reason, like an unsettled payment or a payment method not verified in time."
5. "Settlement has its own rules. They aren't very complex, mostly about the schedule, and they're independent of the rental itself."
6. "Pricing changes independently of the rental. It's just a simple calculator, and any other context or actor can use it."
7. "Yes. Incidents are reported by riders or detected automatically, and handled with their own rules (assignment, escalation, rejection, reopening). Nothing flows back to other flows once an incident is resolved. Support agents do this work, not rental or maintenance staff."

### Round 3 (H3 draft + critic's Fleet Maintenance questions)
Questions: (1) who decides out of service, how maintenance hears about faults; (2) mechanic's unlock vs rider's unlock; (3) what starts debt recovery; (4) who retries a failed charge; (5) max rental time; (6) payment-method verification via Payments.

Answers:
1. "Maintenance decides that a bike is out of service. Rental only checks for the fault on return and publishes the event, without knowing who listens. Incident handling and maintenance both pick it up: support deals with the rider's side, maintenance with the bike. So maintenance hears about it straight from the return, not through an incident."
2. "It's a separate maintenance action. A mechanic's unlock doesn't open a rental session, and maintenance handles its own unlock and return."
3. "The unsettled rental starts debt recovery. The account is blocked because of the same unsettled payment, but recovery doesn't wait for the block."
4. "Settlement retries within its own schedule, which is mostly what its rules are about. Once that runs out, the rental is unsettled and debt recovery takes over with its own actions and schedule."
5. "The session ends by force when the maximum time is exceeded. That's one of the cases where the rider is charged without returning the bike. The penalty is just an extra fee."
6. "Yes, it's the same payment capability. It just executes and reports; it has no rules of its own."

### Round 4 (approval)
Answers: "I approve the eight contexts, but no context map today. … Yes, I approve all eight. The list matches what I told you, Settlement included. No context map for now. Let's stop at the contexts today. … No canvases today either. We'll do canvases in the next session."

## Open questions and assumptions
- **Assumption:** the zone map comes from the City, an external party. Not confirmed.
- Is *Customer Support Contacted* the same business fact as *Incident Reported*? Decides who owns the event name, not a boundary.
- Who drives the in-ride estimate (Rental asks Pricing on a schedule, or the rider's app queries Pricing)? Decides the direction of the Rental/Pricing relationship.
- Who records *Rental Total Fee Calculated*: Settlement after asking the stateless Pricing (critic's suggestion) or Pricing?
- What does *Rental Session Ended* carry (start, end, end reason, zone classification, max time exceeded) so Settlement needn't query Rental?
- Debt Recovery: who pauses/resumes and why; what concrete actions; what *Debt Recovery Failed* leads to (account stays blocked? write-off? external collector?); one recovery per rental or per rider?
- Are there ban reasons beyond invalid card and debt (e.g. fraud)? Would be one more reported reason for Rider Accounts.
- Strategic classification (core/supporting/generic) wasn't discussed.

## Suggested board changes
- Add *Rental Session Ended* (reason: station, zone, forced).
- Add *Payment Succeeded* / *Payment Failed*.
- Add *Rental Settled* / *Rental Unsettled*.
- Add *Bike Taken Out Of Service* / *Bike Back In Service*.
- Add *Account Unblocked* and *Payment Method Verification Deadline Passed*.
- Rename the hourly "… Hour Fee Applied" / "Hourly Fee Applied" stickies to "… Fee Estimated".
- Rename "Drop Zone Fee Applied", "Unauthorized Zone Return Fee Applied", "Penalty Applied" to "… Calculated".
- Rename the mechanic's branch to *Bike Unlocked For Maintenance* / *Bike Returned From Maintenance*, and move it to a maintenance lane.
- Turn "bike broken" into a guard reading "Bike Out Of Service".
- Reorder onboarding: User Account Activated right after User Payment Method Added; verification follows under a deadline.
- Optionally make *Incident Rejected* → *Incident Reopened* loop explicit.

## Next steps
- Next session: context map in CML (processes and team questions above), new file `src/main/cml/VeloCity-Context-Map-board-discovery.cml`, never overwriting the reference `VeloCity-Context-Map.cml`.
- Then canvases via bounded-context-scaffolder at `docs/bounded-contexts/velocity/<context-package>/canvas.md` (deferred by the expert).
