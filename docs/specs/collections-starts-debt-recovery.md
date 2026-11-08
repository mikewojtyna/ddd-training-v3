---
name: collections-starts-debt-recovery
intent: collections-starts-debt-recovery
system: velocity
status: approved
date: 2026-11-08
---

# Spec: Collections wants debt recovery to start as soon as a rental fee stays unpaid

Concrete rules, examples and contract for [the intent](../intents/collections-starts-debt-recovery.md).

## Vocabulary

| Term | Meaning |
|------|---------|
| Rental fee unpaid | All charge attempts for a ride failed; the fee became part of the rider's outstanding debt. |
| Outstanding debt | The sum a rider owes VeloCity from unpaid rides. |
| Debt recovery | The active pursuit of one rider's whole outstanding debt. A rider has at most one open recovery. |
| Open recovery | A recovery that is running or paused, i.e. has neither succeeded nor been exhausted. |
| Collection attempt | A payment reminder to the rider plus a try to collect the whole outstanding debt (DE-40). |
| Failed attempts | How many collection attempts of this recovery have failed so far. |

## Rules

### R1: An unpaid rental fee starts a debt recovery, with the first collection attempt due at once

- **E1.1** Given rider Anna has no debt recovery and no outstanding debt, When her rental fee of PLN 5.00 for ride S-1 is unpaid at 10:50 on 2026-11-08 (Rental Fee Unpaid, outstanding debt PLN 5.00), Then a debt recovery starts for Anna with 0 failed attempts and the first collection attempt due at 10:50 on 2026-11-08 (Recovery Started).

### R2: A further unpaid fee joins the rider's open recovery, running or paused; the attempt count isn't reset

- **E2.1** Given Anna's recovery is running with 2 failed attempts, her outstanding debt is PLN 5.00 and her next attempt is due at 10:50 on 2026-11-14, When her rental fee of PLN 43.00 for ride S-7 is unpaid at 18:05 on 2026-11-12 (Rental Fee Unpaid, outstanding debt PLN 48.00), Then no new recovery starts and Anna's recovery keeps 2 failed attempts, its next attempt still due at 10:50 on 2026-11-14, and now pursues PLN 48.00.
- **E2.2** Given Anna's recovery was paused for a *dispute* with 1 failed attempt, When another rental fee of PLN 3.00 is unpaid, Then no new recovery starts; the fee joins Anna's recovery, which stays paused for *dispute* with 1 failed attempt (DE-88).

### R3: Once a rider's recovery has ended, a new unpaid fee starts a new recovery (DE-88)

- **E3.1** Given Anna's recovery succeeded on 2026-11-11 and she has no outstanding debt, When her rental fee of PLN 7.00 for ride S-9 is unpaid at 09:30 on 2026-11-20, Then a new debt recovery starts for Anna with 0 failed attempts and the first attempt due at 09:30 on 2026-11-20 (Recovery Started).
- **E3.2** Given Anna's recovery was exhausted on 2026-11-20 with PLN 5.00 still outstanding, When her rental fee of PLN 3.00 is unpaid at 12:00 on 2026-11-25 (outstanding debt PLN 8.00), Then a new debt recovery starts for Anna for her whole outstanding debt of PLN 8.00, with 0 failed attempts and the first attempt due at 12:00 on 2026-11-25 (Recovery Started) (DE-88).

## Contract

### Commands

| Command | Issued by | Input | On success | Rejected when |
|---------|-----------|-------|------------|---------------|
| Start Debt Recovery | Collections, whenever a rental fee is unpaid | rider, ride, unpaid amount, outstanding debt, unpaid at | Recovery Started | none (with a running recovery the fee joins it instead, R2) |

### Queries

| Query | Asked by | Input | Result |
|-------|----------|-------|--------|
| None. | | | |

### Events

| Event | When | Data |
|-------|------|------|
| Rental Fee Unpaid | all charge attempts for a ride failed (heard from `finance-retries-failed-charge`) | rider, ride, unpaid amount, outstanding debt, unpaid at |
| Recovery Started | a debt recovery starts for a rider | rider, outstanding debt, started at, first attempt due at |

## Data

| Shape | Field | Type | Constraint |
|-------|-------|------|------------|
| Debt recovery | rider | Identifier (Rider) | required; at most one open recovery per rider |
| Debt recovery | state | one of {running, paused, succeeded, exhausted} | running when started |
| Debt recovery | failed attempts | Number | 0 when started; 0..5 (DE-41) |
| Debt recovery | next attempt due at | Timestamp | = started at, when started (DE-40) |
| Debt recovery | outstanding debt | Money | > PLN 0.00; the rider's whole outstanding debt |

## Out of scope

- The charge retries before the fee becomes unpaid, and blocking the account (`finance-retries-failed-charge`).
- Making collection attempts and ending recovery (`collections-attempts-debt-collection`).
- Pausing and resuming (`collections-agent-pauses-debt-recovery`, `collections-agent-resumes-debt-recovery`).
- The rider paying the debt in the app (`rider-pays-off-outstanding-debt`).
- Handing debt to an external agency (DE-41: out of scope).

## Open questions

- ~~**Q1** When does recovery start, and when is the first attempt?~~ When a rental fee is unpaid; the first attempt when recovery starts (role-played domain expert, DE-40, 2026-11-08)
- ~~**Q2** Does a second unpaid fee start a second recovery?~~ No, it joins the running recovery; the debt grows and the attempt count isn't reset (role-played domain expert, DE-44, 2026-11-08)
- ~~**Q3** Does a further unpaid fee join a *paused* recovery, or only a running one?~~ It joins any open recovery, running or paused, and a paused one stays paused (role-played domain expert, DE-88, 2026-11-08)
- ~~**Q4** After success or exhaustion, does a new unpaid fee start a new recovery?~~ Yes, a new recovery starts; collection attempts pursue the whole outstanding debt (role-played domain expert, DE-88, DE-40, 2026-11-08)

## Notes for the plan

- None.
