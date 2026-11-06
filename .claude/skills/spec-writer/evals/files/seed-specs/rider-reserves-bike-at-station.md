---
name: rider-reserves-bike-at-station
intent: rider-reserves-bike-at-station
system: velocity
status: draft
date: 2026-11-05
---

# Spec: A rider wants to reserve a bike at a bike station

Concrete rules, examples and contract for [the intent](../intents/rider-reserves-bike-at-station.md).

## Vocabulary

| Term | Meaning |
|------|---------|
| Reservation | A rider's hold on one specific available bike at a station, for the reservation period. |
| Reservation period | How long a reservation holds the bike. Length: see Q1. |

## Rules

### R1: A rider can reserve an available bike at a station

- **E1.1** Given bike B-17 is available at station Central and rider Anna has no reservation, When Anna reserves B-17 at 10:00, Then B-17 is reserved for Anna until 10:00 + ? (Q1) (Bike Reserved).

### R2: A rider holds at most one active reservation

- **E2.1** Given Anna holds a reservation for B-17, When Anna reserves B-22 at Central, Then the reservation is rejected and B-22 stays available.

### R3: A reserved bike can't be reserved or rented by anyone else

- **E3.1** Given B-17 is reserved for Anna, When rider Ben reserves B-17, Then the reservation is rejected.
- **E3.2** Given B-17 is reserved for Anna, When Ben rents B-17, Then the rental is rejected.

### R4: An unused reservation expires at the end of the reservation period

- **E4.1** Given Anna reserved B-17 at 10:00, When the reservation period (? Q1) passes without Anna renting B-17, Then the reservation ends and B-17 is available again (Reservation Expired).

## Contract

### Commands

| Command | Issued by | Input | On success | Rejected when |
|---------|-----------|-------|------------|---------------|
| Reserve Bike | Rider | rider, bike, station | Bike Reserved | R2, R3 |

### Queries

| Query | Asked by | Input | Result |
|-------|----------|-------|--------|
| None. | | | |

### Events

| Event | When | Data |
|-------|------|------|
| Bike Reserved | a reservation is accepted | rider, bike, station, reserved at, expires at |
| Reservation Expired | the reservation period passed without a rental | rider, bike, station, expired at |

## Data

| Shape | Field | Type | Constraint |
|-------|-------|------|------------|
| Reservation | expires at | Timestamp | reserved at + reservation period (Q1) |

## Out of scope

- Cancelling a reservation (`rider-cancels-reservation`, candidate).
- Unlocking the bike (`rider-rents-bike-from-station`).

## Open questions

- **Q1** How long is the reservation period? (R1, R4, Data) The vision only says "short-lived".
- **Q2** Does the rider who holds the reservation have to rent exactly the reserved bike, or any bike at that station? (R3)

## Notes for the plan

- None.
