# Worked example: VeloCity discovery session (condensed)

This condenses `docs/domains/velocity/bounded-context-discovery-session.md`, a real session between the trainer (as domain expert) and Claude. Read it to see the **rhythm**:
- echo back how you read the board
- apply one heuristic
- ask numbered questions
- let each answer move a boundary
- admit gaps

Don't use it as an answer key. VeloCity is also a workshop exercise. If a participant is discovering VeloCity, their answers decide the outcome, and leaking these results spoils the exercise.

## Round 0: input and reading
The input was Miro screenshots of an event-storming board (`docs/domains/velocity/velocity-processes.jpg`). Before applying anything, Claude summarised its reading of the board:
- the main timeline, from onboarding through pre-rental checks, reservation and unlock
- a ride that splits into hourly billing, station return and zone return
- two disconnected clusters: debt recovery and incident handling

This lets the expert correct a misreading cheaply.

## Round 1: H1, candidate pivots and questions
Candidates: *User Account Activated*, *Bike Reserved* vs *Rental Session Started*, the return events, *Rental Total Fee Calculated*, and the triggers of the two disconnected clusters. Questions covered:
- activated vs validated
- what the session start means, and whether reservation is a phase of its own
- what the return triggers
- where the faults lead
- duplicated stickies

Answers:
- Activation happens once, when a payment method is added. Validation runs before every rental.
- Session start means the ride is billable. Reservation is just a convenience.
- The real charge happens at the return. The hourly fees are only estimates.
- Faults probably lead to incidents.
- The duplicates are the same events.

## Round 2: confirmed pivots, then H2 swimlanes
The confirmed pivots gave four phases: onboarding, rental preparation, ride, settlement. Claude also named the missing events (a forced end, Payment Charged/Failed) and the misleading "Fee Applied" stickies.

Swimlane candidates were price estimation, maintenance, incidents, debt recovery, and station vs zone returns.

Answers:
- Same pricing rules, plus an unauthorized-zone fee.
- Maintenance is independent.
- Debt blocks rentals.
- Incidents come from riders and from detection, with no feedback into other flows.

Results:
- Station vs zone are **variants, not lanes**.
- Maintenance, incidents and debt recovery are confirmed lanes.
- Price estimation is a separate lane, but see H3.

## Round 3: H3 single responsibility, first draft
- The estimate and the final price answer the same question, so they form **one** Pricing context.
- Payment, failure, blocking and recovery all answered "does this rider owe us money?" and merged.
- The draft had six contexts.

Open questions:
- Blocking also appears in onboarding.
- Is preparation separate from the ride?
- Is Pricing really separate?
- Where does a fault reported on return go?

## Round 4: answers move boundaries
- **Preparation has no rules of its own**, so it merges into Rental. The pivot was a phase change, not a boundary.
- **Pricing evolves independently** as a simple calculator anyone can use, so it stays separate.
- **Rental publishes the fault event, and Incident Handling subscribes**, so the boundary is clean.
- **Blocking happens for two reasons in two contexts**, which violates H3. Merging Onboarding with Debt would break the *Account Activated* pivot. Instead, ownership moves: **Rider Accounts** owns the whole account lifecycle, including every block reason, and the others publish facts it reacts to.

## Round 5: rule ownership splits contexts
- Expert: "Payment knows nothing about any rules." Payments becomes purely mechanical.
- Debt recovery "has its own rules regarding what actions to do, and the schedule". It becomes **its own context**.
- Seven contexts, each with one question.

## Round 6: the gap the expert caught
The expert asked: "So, who actually makes the settlement here?" Nobody did:
- Pricing calculates.
- Payments executes.
- Rental ends at the pivot.

The confirmed pivot (session ended) had nothing behind it. Settlement has its own (simple, schedule-based) rules, so it becomes a context. Simple rules make it supporting, not absent.

**Lesson:** run the cross-cutting "is every step owned?" check yourself before presenting. Each side of every confirmed pivot needs an owner.

## Outcome
Eight contexts, each answering one question:

| Context | Question |
|---|---|
| Rider Accounts | Can this rider use the service? |
| Rental | Who has which bike, and when? |
| Pricing | What does this cost? |
| Settlement | What does the rider owe for this rental, and is it settled? |
| Payments | Did the charge go through? |
| Debt Recovery | How do we recover what's owed? |
| Fleet Maintenance | Is this bike in working order? |
| Incident Handling | What went wrong, and is it resolved? |

Board changes suggested:
- add Rental Session Ended (including a forced end)
- add Payment Succeeded/Failed
- add Rental Settled/Unsettled
- rename "Fee Applied" to "Fee Estimated"
