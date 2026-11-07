---
name: rider-reserves-bike-at-station
sequence: 2
title: A rider wants to reserve a bike at a bike station
system: velocity
actor: Rider
activity: Get a bike
task: Reserve a bike
status: planned
date: 2026-11-07
supersedes:
superseded_by:
source: velocity/briefs/VELO-142-reserve-bike-from-app.md (product meeting 2026-11-02); product owner decisions 2026-11-07
---

# A rider wants to reserve a bike at a bike station

## Intent

As a **Rider**, I want to **reserve an available bike at a station while I'm on my way there** so that **the bike is still there when I arrive**.

## Why

Riders keep finding that the bike they saw as available was gone by the time they walked to the station: 412 support contacts in October, and the second most common reason in the churn survey ("I can never count on a bike being there"). Competitors already offer reservations (VELO-142).

## Business outcome

- Bike Reserved: one specific bike at a station is held for the rider, and other riders can't take it.

## Success signal

- Support contacts about "bike gone when I arrived" down by 50% within two months of release (VELO-142).
- At least 30% of rentals start from a reservation (VELO-142).

## Business rules mentioned

- "A reservation holds the bike for 15 minutes." (product owner, 2026-11-07; the product meeting discussed 10–15 minutes)
- "Riders can reserve remotely, while they are still on their way to the station." (product owner, 2026-11-07)
- "You can rent only one bike at a time": while holding a reservation, a rider can rent only the reserved bike, and renting it ends the reservation. A rider who is riding can't reserve. (product owner, 2026-11-07)
- "One reservation at a time per rider." (product meeting)
- "Only riders with a fully activated account should be able to reserve." (product meeting; vision, "Onboarding")
- The reserved bike isn't offered to other riders. (VELO-142)

## Scope

- **In:** reserving one available bike at a station before arriving there; the bike not being offered to other riders while it's reserved; renting the reserved bike, which ends the reservation (scope widened by the product owner, 2026-11-07, because a reservation is only useful if the rider can rent the bike, and it needs a way to end).
- **Out:**
  - cancelling a reservation (`rider-cancels-reservation`);
  - an unused reservation running out (`fleet-operations-releases-unused-reservation`);
  - renting in general, without a reservation (`rider-rents-bike-from-station`);
  - a warning before the reservation runs out (`rider-is-warned-before-reservation-expires`, candidate);
  - paying for a reservation (`rider-pays-for-reservation`, candidate, not in this release).

## Related intents

- builds on: `rider-rents-bike-from-station`
- enables: `rider-cancels-reservation`
- enables: `fleet-operations-releases-unused-reservation`

## Open questions

- Decided 2026-11-07 by the product owner: the reservation is remote and lasts 15 minutes. This deliberately departs from the vision, which describes a reservation lasting "the seconds it takes to unlock", for a rider "standing near a bike". The vision text is unchanged.

## Notes for the spec

- The ticket's draft acceptance criteria suggest a reservations endpoint (create returns an identifier, 409 when the rider already has an active reservation), a 15-minute expiry kept in Redis, and a `RESERVED` status on the bike table, reusing the unlock flow.
- The mobile team needs the contract by sprint 24.
- Analytics event `reservation_created` to Mixpanel, to measure the success signal.
