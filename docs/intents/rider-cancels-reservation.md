---
name: rider-cancels-reservation
sequence: 4
title: A rider wants to cancel their reservation
system: velocity
actor: Rider
activity: Get a bike
task: Cancel a reservation
status: draft
date: 2026-11-07
supersedes:
superseded_by:
source: velocity/briefs/VELO-142-reserve-bike-from-app.md (product meeting 2026-11-02)
---

# A rider wants to cancel their reservation

## Intent

As a **Rider**, I want to **cancel my reservation** so that **I'm free to reserve another bike when my plans change, and the bike is available to others again**.

## Why

"The rider should be able to cancel if they change their mind" (product meeting). Since a rider may hold only one reservation at a time, a rider who can't cancel would be stuck until the reservation runs out.

## Business outcome

- Reservation Cancelled: the rider gave up the reservation, and the bike is available to anyone again.

## Success signal

- (suggested) Share of reservations that end in a cancellation rather than expiring unused.

## Business rules mentioned

- "One reservation at a time per rider." (product meeting; cancelling frees the rider to reserve again)

## Scope

- **In:** the rider cancelling their own active reservation.
- **Out:** an unused reservation running out by itself (`fleet-operations-releases-unused-reservation`); cancelling a technician's reservation.

## Related intents

- follows: `rider-reserves-bike-at-station`

## Open questions

- Can a rider cancel and immediately reserve the same bike again?

## Notes for the spec

- The ticket suggests a delete call on the reservation to cancel it.
