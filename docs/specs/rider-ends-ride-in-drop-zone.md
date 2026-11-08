---
name: rider-ends-ride-in-drop-zone
intent: rider-ends-ride-in-drop-zone
system: velocity
status: approved
date: 2026-11-08
---

# Spec: A rider wants to end the ride where they are, outside a station

Concrete rules, examples and contract for [the intent](../intents/rider-ends-ride-in-drop-zone.md).

## Vocabulary

| Term | Meaning |
|------|---------|
| Free-floating return | Ending the ride at the rider's current position instead of at a station (vision "Returns"). |
| Drop zone | A city-approved area where a bike may be left without a station. |
| Unauthorized zone | Anywhere outside stations and drop zones; ending a ride there is possible but costs more (vision). |
| End reason | Why a rental session ended. For this intent: *free-floating return* or *lock failure*. |
| Withdrawn | No longer offered to riders; the vision calls such a bike "broken". |

## Rules

### R1: The ride ends where the rider is, in a drop zone or anywhere else (vision)

- **E1.1** Given Anna's rental session on B-17 started at 10:01, When Anna ends the ride at position (52.2297, 21.0122), inside a drop zone, at 10:30 and the lock closes, Then the session ended at 10:30 at (52.2297, 21.0122) with end reason *free-floating return* (Rental Session Ended) and B-17 is available there.
- **E1.2** Given Anna's rental session on B-17 started at 10:01, When Anna ends the ride at (52.2500, 21.1000), in an unauthorized zone, at 10:30 and the lock closes, Then the session ended at 10:30 at (52.2500, 21.1000) with end reason *free-floating return* (Rental Session Ended); the zone only decides what Anna pays (see `finance-prices-free-floating-return-by-zone`).

### R2: When the lock fails, the ride ends at once and the bike is withdrawn, wherever it is (vision, DE-23)

- **E2.1** Given Anna's rental session on B-17 started at 10:01, When Anna ends the ride at (52.2297, 21.0122) at 10:30 and the lock doesn't close, Then the session ended at 10:30 at (52.2297, 21.0122) with end reason *lock failure* (Rental Session Ended), Anna is told the ride ended because the lock failed, and B-17 is withdrawn with reason *lock failed* (Bike Withdrawn).

### R3: Only the rider whose ride is running can end it

- **E3.1** Given Anna's rental session on B-17 is running, When rider Ben ends the ride on B-17 at (52.2297, 21.0122), Then it is rejected and Anna's session keeps running.
- **E3.2** Given Anna has no running rental session, When Anna ends a ride on B-22 at (52.2297, 21.0122), Then it is rejected.

## Contract

### Commands

| Command | Issued by | Input | On success | Rejected when |
|---------|-----------|-------|------------|---------------|
| End Ride Here | Rider | rider, bike, rider's position | Rental Session Ended, Bike Withdrawn | R3 |

### Queries

| Query | Asked by | Input | Result |
|-------|----------|-------|--------|
| None. | | | |

### Events

| Event | When | Data |
|-------|------|------|
| Rental Session Ended | a running ride ended | rental session, rider, bike, started at, ended at, end reason, position |
| Bike Withdrawn | the lock failed at the free-floating return | bike, withdrawal reason (*lock failed*), position, withdrawn at |

## Data

| Shape | Field | Type | Constraint |
|-------|-------|------|------------|
| Free-floating return | rider's position | Position (latitude, longitude) | required; any position, trusted as reported (DE-22) |
| Ended ride | end reason | one of {free-floating return, lock failure} | lock failure exactly when the lock didn't close (R2) |
| Ended ride | ended at | Timestamp | the moment the rider ended the ride, also when the lock failed |

## Out of scope

- What the ride and the return cost: drop zone fee or unauthorized zone return fee, and no return fee after a lock failure (`finance-prices-free-floating-return-by-zone`, DE-23).
- Deciding whether a position is in a drop zone: that only matters for the price.
- Returning at a station (`rider-returns-bike-at-station`).
- A ride ended automatically (`fleet-operations-ends-overdue-rental`).
- A fault the bike itself signalled during the ride (`fleet-operations-withdraws-bike-signalling-fault`).

## Open questions

- **Q1** Can a rider report a fault when ending the ride outside a station, as they can at a station (DE-24)? (Contract: End Ride Here input) Doesn't block R1-R3.
- ~~**Q2** What happens when the lock fails at a drop-zone return?~~ Exactly what happens at a station: the session ends at that moment, the bike is withdrawn and no return fee applies, wherever the bike is (role-played domain expert, DE-23, 2026-11-08)

## Notes for the plan

- None.
