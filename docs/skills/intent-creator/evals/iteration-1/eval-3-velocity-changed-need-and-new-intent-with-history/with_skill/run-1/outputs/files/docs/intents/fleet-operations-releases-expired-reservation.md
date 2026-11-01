---
name: fleet-operations-releases-expired-reservation
sequence: 5
title: Fleet operations wants unused reservations released after they expire
system: velocity
actor: Fleet Operations
activity: Get a bike
task: Release an expired reservation
status: draft
date: 2026-11-01
supersedes:
superseded_by:
source: conversation 2026-11-01 (product request after the pilot); velocity/velocity-vision.md "Renting a Bike"
---

# Fleet operations wants unused reservations released after they expire

## Intent

As a **Fleet Operations** team, I want to **have a reservation released once its time is up and the bike wasn't unlocked** so that **the bike goes back to the pool and other riders can rent it instead of it standing idle**.

## Why

Product wants a reservation to hold a bike for 15 minutes. Without an end to that hold, a rider who never shows up would block a bike indefinitely, which costs rentals and frustrates other riders at that station. The vision states the same rule: "The reservation expires before the rider unlocks the bike. The bike returns to the pool and is available to anyone." It also frees the rider's single reservation slot so they can reserve again.

## Business outcome

- Reservation Expired: the rider didn't unlock the reserved bike in time, so the reservation ended.
- Bike Available Again: the bike that was reserved is back in the pool and anyone can rent or reserve it.

## Success signal

- (suggested) No bike stays reserved past the reservation time.
- (suggested) Share of reservations that expire unused, tracked to see whether 15 minutes is the right length.

## Business rules mentioned

- "Hold a bike for 15 minutes" (product).
- "One active reservation per rider" (product); an expired reservation no longer counts as active.
- "The reservation expires before the rider unlocks the bike. The bike returns to the pool and is available to anyone." (vision, "Renting a Bike").
- For a broken bike reserved by a technician, "the bike is still broken, so it is not offered to riders" (vision, "Broken Bikes & Maintenance"); out of scope here.

## Scope

- **In:** ending a rider's reservation that wasn't used in time and making the bike available again.
- **Out:** making the reservation (`rider-reserves-bike-at-station`); the rider cancelling it early (`rider-cancels-reservation`, candidate); releasing it after a failed unlock (`rider-reservation-released-after-failed-unlock`, candidate); expiry of a technician's reservation on a broken bike (`technician-reserves-broken-bike`, candidate).

## Related intents

- follows: `rider-reserves-bike-at-station`

## Open questions

- Should the rider be told when their reservation expires, or warned shortly before?
- Is there a consequence for a rider whose reservations keep expiring unused (a fee, a cool-down)?
- Who in the business owns this rule: Fleet Operations, as written here, or product? The actor name "Fleet Operations" is new on the story map and needs confirming.

## Notes for the spec

- None.
