# Bounded context discovery: VeloCity (bike rental processes board)

- **Date:** 2026-11-05
- **Mode:** discover
- **Status:** in progress: heuristics canvases handed off
- **Input:** velocity/velocity-processes.jpg (Miro board "Bike Rental Processes")
- **Intention:** create a new system (VeloCity)
- **Context map:** requested (src/main/cml/VeloCity-Context-Map-<slug>.cml; the existing VeloCity-Context-Map.cml is the workshop reference and is not overwritten)

## Reading of the input
Seven lanes on the board:
- **User Registration And Account Setup:** User Registered → User Email Confirmed → User Phone Number Verified → User Basic Data Provided → User Payment Method Added → User Payment Method Verified → User Agreement Signed → User Account Activated.
- **Bike Rental:** (User Location Validated | bike broken) → Bike Reserved → Bike Unlocked → Rental Session Started → bike returned; Bike Unlocked → maintenance started → bike fixed → bike returned; Bike Reserved → Bike Unlock Failed → Bike Reservation Expired; Bike Reserved → Bike Reservation Expired.
- **Pricing:** Rental Session Started → First Free Period Started/Ended → First/Second/Third Hour Billing Period Started → … Fee Applied → Fourth Hour And Subsequent Billing Period Started → Hourly Fee Applied → Rental Total Fee Calculated | Max Rental Time Exceeded → Penalty Applied.
- **Bike Station Return Handling:** Bike Returned To Station → Bike Locked In Station → Bike Fault Detected → Bike Fault Reported; Bike Locking Failed → Customer Support Contacted | Bike Fault Reported.
- **Bike Zone Return Handling:** Bike Returned to Zone → Bike Return To Drop Zone Accepted → Drop Zone Fee Applied; Bike Return To Unauthorized Zone Detected → Unauthorized Zone Return Fee Applied.
- **Account Block And Debt Recovery (disconnected):** Account Blocked Due To Invalid Card → Account Block Notification Sent; Account Blocked Due To Debt → Account Block Notification Sent → Debt Recovery Process Started → Failed | Succeeded | Paused → Resumed → Succeeded | Failed.
- **Customer Support And Incident Handling (disconnected):** Incident Reported → Incident Type Detected → Incident Assigned To Agent → Accepted (→ Escalated → Resolved, or Resolved) | Rejected | Rejected → Reopened → Resolved.

Ambiguities noticed:
- "bike broken" runs in parallel with User Location Validated, before Bike Reserved.
- A maintenance branch (maintenance started → bike fixed → bike returned) hangs off Bike Unlocked.
- "bike returned" appears twice in the rental lane, and returns also have their own two lanes.
- Nothing follows Rental Total Fee Calculated: no payment event.
- Debt recovery and incident handling don't connect to the main timeline.
- Account Blocked and Incident Rejected each appear twice.

## Heuristic trail
### H1: Pivotal events
| Event | Status | Before | After | Expert's answer |
|---|---|---|---|---|
| User Account Activated | confirmed | onboarding (account validated with a first payment) | renting | "User activated is when user account is validated using payment for the first time." Location check is per rental, so it belongs on the rental side. |
| Rental Session Started | confirmed | preparing a rental (location check, reservation, unlock) | the ride | "Let's start with Rental Session Started event which initiates the ride." Reservation is a step with a reservation-window rule, not a phase. |
| Bike returned (end of ride) | confirmed (implied) | the ride | payment | "Payment happens after end of the ride." |
| Rental Total Fee Calculated | not a pivot | | | The hourly "Fee Applied" events are only estimates. |
| Account Blocked Due To Debt | not a pivot | | | Blocking is a generic capability; the reason comes from the caller. |

### H2: Swimlanes
| Flow | Verdict (lane/variant) | Why |
|---|---|---|
| Station vs zone returns | variant | "The same rules" (except zone fees, which belong to Pricing) |
| Price estimation during the ride | lane, but same rules as the final fee | "The same rules" |
| Fleet maintenance | lane | "It's totally separate"; bike/station only report broken |
| Debt recovery | lane | Own actions and schedule ("Yes") |
| Incident handling | lane | Disconnected, own loops (escalate, reject, reopen) |

### H3: Single responsibility
| Duplicated responsibility | Where it appeared | Resolution |
|---|---|---|
| Cost of a ride | estimate during the ride, final fee after it | One Pricing context; same rules |
| Account blocking | "Blocked Due To Invalid Card", "Blocked Due To Debt" | Generic capability in Rider Accounts; reason supplied by the caller |
| Settling a rental | glossed over between Pricing and Payments | Settlement is its own context ("Settlement is the context on its own. period.") |

## Bounded contexts
| Context | Question it answers | Owns (events, decisions, rules) | Heuristic justification | Classification |
|---|---|---|---|---|
| Rider Accounts | Can this rider use the service? | registration, activation (first payment validation), account status, generic block/unblock (reason from caller), invalid-card block rule, notifications | before the User Account Activated pivot; H3 single owner of blocking | |
| Rental | Who has which bike, and when? | per-rental location/eligibility check, reservation window, unlock, session, station/zone returns, locking, max-time, reporting broken bikes | Rental Session Started is a phase change, but preparation has no rules of its own (H3) | |
| Pricing | What does this ride cost? | free period, hourly tiers, penalty, zone fees; estimate and final fee | H2 own clock; H3 one set of rules | |
| Settlement | What does the rider owe for this rental, and is it settled? | bill after the ride, payment request, unsettled decision | far side of the end-of-ride pivot; expert confirmed | |
| Payments | Did the charge go through, after retries? | payment-method verification, executing charges, charge retries; card processor is an external system behind it | H3: own rules (retries); invalid-card block rule belongs to Rider Accounts | |
| Debt Recovery | How do we recover what's owed? | recovery process, actions, schedule | H2 own long clock; H3 own rules | |
| Fleet Maintenance | Is this bike in working order? | maintenance, repairs, back in circulation | H2 separate lane | |
| Incident Handling | What went wrong, and is it resolved? | incidents and their lifecycle | H2 disconnected lane with own loops | |

## Critic log
| Subject | Round | Verdict | Blocking points / resolution |
|---|---|---|---|
| Rider Accounts | 1 | AGREE | notes: rename block stickies to one generic event with a reason; add Account Unblocked |
| Rental | 1 | AGREE | notes: one Rental Session Ended event (incl. forced end); open: does max time end the ride? |
| Pricing | 1 | AGREE | notes: Rental reports facts, Pricing turns them into fees |
| Settlement | 1 | AGREE | notes: add Rental Settled/Unsettled, Payment Succeeded/Failed |
| Payments | 1 | DISAGREE | own rules? who decides the invalid-card block? is it just the external processor? -> asked the expert |
| Debt Recovery | 1 | AGREE | open: who requests the debt block; what flows back on success |
| Fleet Maintenance | 1 | AGREE | open: who withdraws a broken bike / returns it to circulation |
| Incident Handling | 1 | AGREE | open: do fault reports go through incidents first? |
| Payments | 2 | AGREE | retries owned by Payments; Payment Failed published only after retries |
| Settlement | 2 | AGREE | re-checked because of retries; question stays unique. Open: own re-charge schedule after Payment Failed? |

## Q&A with the domain expert
### Round 1 (reading + H1)
Expert's answers (verbatim):
- Let's start with Rental Session Started event which initiates the rid.
1. User activated is when user account is validated using payment for the first time. User location is required to check whether user is allowed to rent a bike.
2. Reservation doesn't have other limits or flees, but bike reserved has a reservation window which decides whether the bike is locked for this specifci user.
3. Payment happens after end of the ride. These fee applied are only for estimating the costs.
4. Blocking is a generic capability, it doesn't understand the context on it's owne. The reason is only passed from clients.
5. It's totally separate, but the station/system/bike can also decide that bike is broken. But it's all it knows - it doesn't know how to handle the maintenance.
- Note: limited time; keep questions concise and relevant.

### Round 2 (H2 swimlanes)
1. Settlement is the context on its own. period.
2. Yes (debt recovery has its own rules: actions, schedule)
3. The same rules (estimate and final fee)
4. The same rules (station and zone returns)

### Round 3 (H3 draft + critic questions on Payments; approval; map yes/no)
1. Retries are part of it
2. The Rider Accounts by itself if credit card wasn't validated by payment.
3. Yes, looking solid
4. yes

### Round 4 (map: processes and hand-offs)
1. Yes (processes P1-P5)
2. Yes (all teams internal; City zone data read as external - assumption)
3. It retries. After exceeding max results, the rental is unsettled.
4. To incidents first. That would be the maintenance.
5. When debt recovery succeded, then debt recovery confirms that rental is settled by sending a command to the settlement. Then, settlement lifts the account block.
- Note: be short, not much time.

### Round 5 (map approval)
- "looking good, go on" (map approved; zone-data and Debt Recovery charging questions left open)

## Context map
CML: src/main/cml/VeloCity-Context-Map-Discovery.cml (new file; the reference VeloCity-Context-Map.cml is untouched).
Validator: VALID, 10 contexts, 13 relationships, 0 errors, no RULE lines (no cycles). 11 deliberate warnings: ACL in Customer/Supplier (trainer's ACL-by-default rule).
Patterns: internal links Customer/Supplier, PL upstream, ACL downstream; PaymentProcessor (OHS, PL) -> Payments (ACL); City -> Rental (ACL, unconfirmed).
Critics: P1, P2, P3, P4+P5 AGREE round 1; final pass over all processes AGREE round 1. Only comment changes applied.

## Open questions and assumptions
- City zone data is external (interpretation of Q2 "Yes"); relationship marked _Unconfirmed.
- Does Debt Recovery charge the card itself (would add Payments -> DebtRecovery)?

## Suggested board changes
- Rename the hourly "... Fee Applied" stickies to "... Fee Estimated": they are estimates, not charges.
- Add a payment event after the end of the ride (e.g. Payment Succeeded / Payment Failed).

## Next steps
- Canvases (canvas only, no plan, no code), pre-filled via bounded-context-scaffolder's canvas template:
  docs/bounded-contexts/velocity/{rideraccounts,rental,pricing,settlement,payments,debtrecovery,incidenthandling}/canvas.md
- FleetMaintenance: existing canvas (completed 2026-11-03, code exists) left untouched; compare it with this discovery before updating.
- Name to settle: Rental (discovery) vs Rentals (existing code package and reference CML).
- Business decisions in the canvases mostly need concrete Given/When/Then values before scaffolding.
