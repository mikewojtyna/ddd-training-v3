---
name: rider-cancels-reservation
intent: rider-cancels-reservation
system: velocity
status: approved
date: 2026-11-10
---

# Spec: A rider wants to cancel a reservation they no longer need

Concrete rules, examples and contract for [the intent](../intents/rider-cancels-reservation.md).

## Vocabulary

| Term | Meaning |
|------|---------|
| Reservation | A brief, exclusive hold on one specific bike for one rider, between choosing it and unlocking it (see `rider-reserves-bike`). |
| Cancellation | The holder of a reservation ending it on purpose, before its period is over. |

## Rules

### R1: A rider can cancel their reservation; the bike is free for anyone at once

- **E1.1** Given B-17 is reserved for Anna until 10:02, When Anna cancels her reservation at 10:01, Then the reservation is cancelled at 10:01 (Reservation Cancelled) and Ben, standing 10 m from B-17, can reserve it at 10:01:30.

### R2: Only the rider holding the reservation can cancel it

- **E2.1** Given B-17 is reserved for Anna until 10:02, When Ben cancels a reservation of B-17 at 10:01, Then the cancellation is rejected and B-17 stays reserved for Anna.

### R3: A bike whose ride has started can't have its reservation cancelled

- **E3.1** Given Anna unlocked B-17 at 10:01 and is riding it, When Anna cancels her reservation of B-17 at 10:05, Then the cancellation is rejected and Anna's ride goes on.

## Contract

### Commands

| Command | Issued by | Input | On success | Rejected when |
|---------|-----------|-------|------------|---------------|
| Cancel Reservation | Rider | rider, bike | Reservation Cancelled | R2, R3 |

### Queries

| Query | Asked by | Input | Result |
|-------|----------|-------|--------|
| Rider's Active Rental | Rider | rider | none after a cancellation |

### Events

| Event | When | Data |
|-------|------|------|
| Reservation Cancelled | a rider cancelled their reservation | rider, bike, cancelled at |

## Data

| Shape | Field | Type | Constraint |
|-------|-------|------|------------|
| Cancellation | rider | Identifier (Rider) | required; holds the reservation (R2) |
| Cancellation | bike | Identifier (Bike) | required; reserved, not ridden (R3) |

## Out of scope

- A technician cancelling the reservation of a broken bike.
- Any fee: reservations are free (DE-17).

## Open questions

- None.

## Notes for the plan

- None.
