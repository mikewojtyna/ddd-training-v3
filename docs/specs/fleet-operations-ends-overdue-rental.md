---
name: fleet-operations-ends-overdue-rental
intent: fleet-operations-ends-overdue-rental
system: velocity
status: approved
date: 2026-11-08
---

# Spec: Fleet operations wants a ride that runs past the maximum rental time ended

Concrete rules, examples and contract for [the intent](../intents/fleet-operations-ends-overdue-rental.md).

## Vocabulary

| Term | Meaning |
|------|---------|
| Maximum rental time | 12 hours from the start of the rental session (DE-20). A ride of exactly 12:00:00 is still a ride. |
| Overdue ride | A rental session that has run longer than the maximum rental time without being returned. |
| End reason | Why a rental session ended. For this intent: *maximum rental time exceeded*. |
| Not returned | Withdrawal reason of a bike whose ride was ended automatically; nobody knows where it is (DE-21). |

## Rules

### R1: A ride running longer than 12 hours is ended automatically, at the 12-hour mark (vision, DE-20, DE-82)

- **E1.1** Given Anna's rental session on B-17 started at 08:00 and is still running, When the rides are checked at 20:00:01, Then the session ended at 20:00 with end reason *maximum rental time exceeded* and no known place (Rental Session Ended).
- **E1.2** Given Anna's rental session on B-17 started at 08:00 and is still running, When the rides are checked at exactly 20:00:00, Then the session is still running.
- **E1.3** Given Anna's rental session on B-17 started at 08:00 and is still running, When the rides are checked only at 22:15, Then the session ended at 20:00 with end reason *maximum rental time exceeded* (Rental Session Ended) (DE-82).

### R2: The bike of an automatically ended ride is withdrawn as not returned (DE-21)

- **E2.1** Given Anna's rental session on B-17 started at 08:00, When the rides are checked at 20:00:01, Then B-17 is withdrawn with reason *not returned* (Bike Withdrawn) and isn't offered to riders.

### R3: A ride that ended in time is left alone

- **E3.1** Given Anna's rental session on B-17 started at 08:00 and she returned B-17 at station Old Town at 19:59, When the rides are checked at 20:00:01, Then nothing happens: the ride stays ended at Old Town at 19:59 and B-17 stays available.

## Contract

### Commands

| Command | Issued by | Input | On success | Rejected when |
|---------|-----------|-------|------------|---------------|
| End Overdue Rental Sessions | Fleet Operations | the current time | Rental Session Ended, Bike Withdrawn | |

### Queries

| Query | Asked by | Input | Result |
|-------|----------|-------|--------|
| None. | | | |

### Events

| Event | When | Data |
|-------|------|------|
| Rental Session Ended | an overdue ride was ended; one per overdue ride | rental session, rider, bike, started at, ended at (started at + 12 hours), end reason *maximum rental time exceeded*, no place |
| Bike Withdrawn | the bike of an automatically ended ride wasn't returned | bike, withdrawal reason *not returned*, withdrawn at |

## Data

| Shape | Field | Type | Constraint |
|-------|-------|------|------------|
| Ended ride | ended at | Timestamp | started at + 12 hours (R1) |
| Ended ride | end reason | one of {maximum rental time exceeded} | for this intent |
| Ended ride | place | none | the bike wasn't returned anywhere (DE-21) |

## Out of scope

- The penalty and the hourly fees for the full 12 hours (`finance-penalizes-overdue-rental`, DE-33).
- Finding and recovering the bike (the technician's job, not modelled, DE-26).
- Warning the rider before the limit (not requested).
- How often the rides are checked: the examples only say what a check at a given time finds.

## Open questions

- ~~**Q1** When the check happens late, does the ride end at the 12-hour mark (20:00) or at the moment of the check (22:15)?~~ At the 12-hour mark (started + 12 h), whenever it is noticed (role-played domain expert, DE-82, 2026-11-08)
- **Q2** Should the rider be warned before the maximum rental time is reached? Doesn't change R1-R3.
- ~~**Q3** What is the maximum rental time?~~ 12 hours; at 12:00:00 of riding it is still a ride, it is ended once it runs longer (role-played domain expert, DE-20, 2026-11-08)
- ~~**Q4** Where does an automatically ended session end?~~ Nowhere known: the bike wasn't returned, it is withdrawn as not returned and treated as broken (role-played domain expert, DE-21, 2026-11-08)

## Notes for the plan

- None.
