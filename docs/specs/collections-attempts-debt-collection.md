---
name: collections-attempts-debt-collection
intent: collections-attempts-debt-collection
system: velocity
status: approved
date: 2026-11-08
---

# Spec: Collections wants to remind riders and try to collect their debt on a schedule

Concrete rules, examples and contract for [the intent](../intents/collections-attempts-debt-collection.md).

## Vocabulary

| Term | Meaning |
|------|---------|
| Debt recovery | The active pursuit of one rider's whole outstanding debt; running, paused, succeeded or exhausted. |
| Collection attempt | A payment reminder to the rider plus a try to collect the whole outstanding debt at once from their current card (DE-40). |
| Due | The time of the recovery's next collection attempt has come: at once when recovery starts, then 3 days after each failed attempt (DE-40). |
| Failed attempts | How many collection attempts of this recovery failed so far. |
| Succeeded | The rider's outstanding debt was cleared; the recovery ends. |
| Exhausted | The 5th collection attempt failed; the recovery ends, the rider stays blocked and the debt stays outstanding (DE-41). |

## Rules

### R1: When a recovery's collection attempt is due, the rider is reminded and the whole outstanding debt is asked for

- **E1.1** Given Anna's recovery started at 10:50 on 2026-11-08 with outstanding debt PLN 5.00 and its first attempt due at once, When due collections are attempted at 10:50 on 2026-11-08, Then Anna gets a payment reminder and collection of PLN 5.00 is tried (Collection Attempted, attempt 1).
- **E1.2** Given Anna's recovery is running with 1 failed attempt and its next attempt due at 10:50 on 2026-11-11, When due collections are attempted at 10:49 on 2026-11-11, Then no attempt is made for Anna (72 hours to the minute, DE-90).

### R2: A collection attempt that collects the debt ends the recovery as succeeded

- **E2.1** Given Anna's recovery has 1 failed attempt and its next attempt is due at 10:50 on 2026-11-11, When the attempt at 10:50 on 2026-11-11 collects PLN 5.00 (Outstanding Debt Cleared), Then Anna was reminded, the recovery succeeded (Collection Attempted, Recovery Succeeded) and no further attempts are due.

### R3: A failed collection attempt counts, and the next attempt is due 3 days later

- **E3.1** Given Anna's recovery has 0 failed attempts, When the attempt at 10:50 on 2026-11-08 fails, Then Anna's recovery has 1 failed attempt and the next attempt is due at 10:50 on 2026-11-11 (Collection Attempted).

### R4: The 5th failed collection attempt exhausts the recovery

- **E4.1** Given Anna's recovery has 4 failed attempts (2026-11-08, 11-11, 11-14, 11-17) and PLN 5.00 outstanding, When the attempt at 10:50 on 2026-11-20 fails, Then the recovery is exhausted with 5 failed attempts (Collection Attempted, Recovery Exhausted); Anna stays blocked and still owes PLN 5.00.
- **E4.2** Given Anna's recovery was exhausted at 10:50 on 2026-11-20, When due collections are attempted at 10:50 on 2026-11-23, Then no attempt is made for Anna and she gets no reminder.

### R5: A cleared debt ends the recovery as succeeded, even when paused or exhausted; a second clearance changes nothing

- **E5.1** Given Anna's recovery is paused for a *dispute* with 2 failed attempts, When Anna pays her outstanding PLN 5.00 in the app at 16:00 on 2026-11-15 (Outstanding Debt Cleared), Then the recovery succeeded (Recovery Succeeded).
- **E5.2** Given Anna's recovery was exhausted on 2026-11-20, When Anna pays her outstanding PLN 5.00 in the app on 2026-11-28 (Outstanding Debt Cleared), Then the recovery is recorded as succeeded (Recovery Succeeded) (DE-45).
- **E5.3** Given Anna's recovery succeeded on 2026-11-28, When Outstanding Debt Cleared for Anna is heard a second time, Then nothing changes and no second Recovery Succeeded happens (DE-45).

## Contract

### Commands

| Command | Issued by | Input | On success | Rejected when |
|---------|-----------|-------|------------|---------------|
| Attempt Due Collections | Collections, on its regular schedule | now | Collection Attempted, Recovery Succeeded, Recovery Exhausted | none (recoveries not due, paused or ended are skipped) |
| Record Debt Cleared | Collections, whenever a rider's outstanding debt is cleared | rider, cleared at | Recovery Succeeded | none (an already succeeded recovery is left as it is, E5.3) |

### Queries

| Query | Asked by | Input | Result |
|-------|----------|-------|--------|
| None. | | | |

### Events

| Event | When | Data |
|-------|------|------|
| Outstanding Debt Cleared | the rider's outstanding debt went down to PLN 0.00, by a collection or by the rider paying (heard from `rider-pays-off-outstanding-debt`) | rider, cleared at |
| Collection Attempted | a due collection attempt was made | rider, attempt number, amount asked, collected (yes/no), attempted at, next attempt due at (none if collected or exhausted) |
| Recovery Succeeded | the rider's outstanding debt was cleared while a recovery was open or exhausted | rider, succeeded at, failed attempts |
| Recovery Exhausted | the 5th collection attempt failed | rider, exhausted at, outstanding debt |

## Data

| Shape | Field | Type | Constraint |
|-------|-------|------|------------|
| Collection attempt | attempt number | Number | 1..5 (DE-41) |
| Collection attempt | amount asked | Money | the rider's whole outstanding debt at the time, > PLN 0.00 (DE-40) |
| Collection attempt | next attempt due at | Timestamp | attempted at + 72 hours, only after a failed attempt 1..4 (DE-40, DE-90) |

## Out of scope

- Starting a recovery (`collections-starts-debt-recovery`).
- Pausing and resuming, and the rule that a paused recovery makes no attempts (`collections-agent-pauses-debt-recovery`, `collections-agent-resumes-debt-recovery`).
- Charging the card, clearing the debt and unblocking the account (`rider-pays-off-outstanding-debt`, `finance-retries-failed-charge`).
- Handing exhausted debt to an external agency (DE-41) and payment-plan instalments (DE-43): out of scope.
- The wording and channel of the payment reminder.

## Open questions

- ~~**Q1** How often is a collection attempted?~~ First attempt when recovery starts, then every 3 days; each attempt reminds the rider and tries to collect the whole outstanding debt (role-played domain expert, DE-40, 2026-11-08)
- ~~**Q2** When is recovery exhausted?~~ After 5 failed collection attempts; the rider stays blocked and the debt stays outstanding (role-played domain expert, DE-41, 2026-11-08)
- ~~**Q3** Is "every 3 days" counted to the minute or by calendar day?~~ 72 hours, to the minute (role-played domain expert, DE-90, 2026-11-08)
- ~~**Q4** Does a clearance after exhaustion count, and what about a duplicate clearance?~~ It counts, the recovery is recorded as succeeded; a second clearance changes nothing (role-played domain expert, DE-45, 2026-11-08)

## Notes for the plan

- None.
