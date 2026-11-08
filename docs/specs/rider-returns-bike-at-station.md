---
name: rider-returns-bike-at-station
intent: rider-returns-bike-at-station
system: velocity
status: approved
date: 2026-11-08
---

# Spec: A rider wants to return the bike at a station and end the ride

Concrete rules, examples and contract for [the intent](../intents/rider-returns-bike-at-station.md).

## Vocabulary

| Term | Meaning |
|------|---------|
| Rental session | A rider's ride, from unlock until the ride ends (vision "Domain Vocabulary"). |
| Station return | The rider brings the bike to an official station and the bike is locked there (vision "Returns"). |
| End reason | Why a rental session ended. For this intent: *station return* or *lock failure*. |
| Withdrawn | No longer offered to riders; the vision calls such a bike "broken". Only a technician can reserve it (DE-16). |
| Withdrawal reason | Why a bike was withdrawn. For this intent: *lock failed* or *fault reported at return*. |

## Rules

### R1: A station return with a lock that closes ends the ride at that station

- **E1.1** Given Anna's rental session on B-17 started at 10:01, When Anna returns B-17 at station Old Town at 10:40 and the lock closes, Then the session ended at 10:40 at Old Town with end reason *station return* (Rental Session Ended) and B-17 is available at Old Town.

### R2: When the lock fails, the ride ends at once and the bike is withdrawn (vision)

- **E2.1** Given Anna's rental session on B-17 started at 10:01, When Anna returns B-17 at Old Town at 10:40 and the lock doesn't close, Then the session ended at 10:40 at Old Town with end reason *lock failure* (Rental Session Ended), Anna is told the ride ended because the lock failed, and B-17 is withdrawn with reason *lock failed* (Bike Withdrawn).

### R3: A fault reported at a station return withdraws the bike, but the ride ends normally (vision, DE-24)

- **E3.1** Given Anna's rental session on B-17 started at 10:01, When Anna returns B-17 at Old Town at 10:40 reporting "chain slips" and the lock closes, Then the session ended at 10:40 at Old Town with end reason *station return* (Rental Session Ended) and B-17 is withdrawn with reason *fault reported at return* (Bike Withdrawn).

### R4: The ride ends at the station the rider names (DE-22)

- **E4.1** Given Anna's rental session on B-17 started at 10:01, When Anna returns B-17 at 10:40 naming station Central and the lock closes, Then the session ended at Central, without checking Anna's position (Rental Session Ended).
- **E4.2** Given Anna's rental session on B-17 is running, When Anna returns B-17 naming "Harbour Café", which is not a station, Then the return is rejected and the session keeps running.

### R5: Only the rider whose ride is running can return the bike

- **E5.1** Given Anna's rental session on B-17 is running, When rider Ben returns B-17 at Old Town, Then the return is rejected and Anna's session keeps running.
- **E5.2** Given Anna has no running rental session, When Anna returns B-22 at Old Town, Then the return is rejected.

## Contract

### Commands

| Command | Issued by | Input | On success | Rejected when |
|---------|-----------|-------|------------|---------------|
| Return Bike at Station | Rider | rider, bike, station, fault description (optional) | Rental Session Ended, Bike Withdrawn | R4, R5 |

### Queries

| Query | Asked by | Input | Result |
|-------|----------|-------|--------|
| None. | | | |

### Events

| Event | When | Data |
|-------|------|------|
| Rental Session Ended | a running ride ended | rental session, rider, bike, started at, ended at, end reason, station |
| Bike Withdrawn | the lock failed, or the rider reported a fault, at the return | bike, withdrawal reason, fault description (if reported), withdrawn at |

## Data

| Shape | Field | Type | Constraint |
|-------|-------|------|------------|
| Station return | station | Identifier (Station) | required; one of the city's stations (R4) |
| Station return | fault description | Text | optional; when given, the bike is withdrawn (R3) |
| Ended ride | end reason | one of {station return, lock failure} | lock failure exactly when the lock didn't close (R2) |
| Ended ride | ended at | Timestamp | the moment of the return, also when the lock failed |
| Withdrawal | withdrawal reason | one of {lock failed, fault reported at return} | |

## Out of scope

- What the ride costs: the hourly fees and "the rider is not charged extra because our lock failed" (`finance-prices-ride-by-tiered-tariff`).
- Ending the ride outside a station (`rider-ends-ride-in-drop-zone`).
- A ride ended automatically (`fleet-operations-ends-overdue-rental`).
- A fault the bike itself signalled during the ride (`fleet-operations-withdraws-bike-signalling-fault`).
- Repairing the bike (`technician-reserves-broken-bike` and following).

## Open questions

- ~~**Q1** Does a station return need anything from the rider?~~ The rider names the station they're at, and that is trusted (role-played domain expert, DE-22, 2026-11-08)
- ~~**Q2** A rider reports a fault when returning at a station: how does the ride end?~~ Normally, as a station return with normal fees, and the bike is withdrawn as faulty (role-played domain expert, DE-24, 2026-11-08)

## Notes for the plan

- None.
