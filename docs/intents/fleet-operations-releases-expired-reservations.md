---
name: fleet-operations-releases-expired-reservations
sequence: 9
title: Fleet operations wants reservations that weren't used released
system: velocity
actor: Fleet Operations
activity: Get a bike
task: Release unused reservations
status: delivered
date: 2026-11-08
supersedes:
superseded_by:
source: docs/domains/velocity/velocity-vision.md "Renting a Bike", "Broken Bikes & Maintenance"; domain-expert answers DE-10, DE-11, DE-17 (2026-11-08)
---

# Fleet operations wants reservations that weren't used released

## Intent

As a member of **Fleet Operations**, I want **reservations that weren't used in time released** so that **no bike stays blocked for a rider or technician who never came to unlock it**.

## Why

"The reservation expires before the rider unlocks the bike. The bike returns to the pool and is available to anyone" ("Renting a Bike"). The same happens to a technician's reservation, except that the bike is still broken: "it is not offered to riders, it simply waits until a technician reserves it again" ("Broken Bikes & Maintenance"). A reservation nobody uses must not take a bike off the street.

## Business outcome

- Reservation Expired: the holder no longer holds the bike; a rider's bike is available to anyone again, a broken bike waits for the next technician.

## Success signal

- (suggested) No bike stays reserved longer than its reservation period.
- (suggested) Share of reservations that expire, watched as a sign of friction at unlock.

## Business rules mentioned

- "The reservation expires before the rider unlocks the bike. The bike returns to the pool and is available to anyone." (vision)
- "The bike is still broken, so it is not offered to riders." (vision, technician's reservation)
- A rider's reservation lasts 2 minutes (DE-10); a technician's lasts 30 minutes (DE-11).
- Expired reservations are free (DE-17).

## Scope

- **In:** ending every reservation whose period has passed without an unlock, for riders and technicians.
- **Out:** reserving (`rider-reserves-bike`, `technician-reserves-broken-bike`); unlocking (`rider-unlocks-reserved-bike`, `technician-starts-maintenance`); a rider cancelling a reservation on purpose (not requested).

## Related intents

- follows: `rider-reserves-bike`
- follows: `technician-reserves-broken-bike`
- alternative to: `rider-unlocks-reserved-bike` (a reservation ends either by an unlock or by expiring)

## Open questions

- Is the rider told that their reservation expired?

## Notes for the spec

- Accepted under the trainer's standing instruction of 2026-11-08 (autonomous run), to be reviewed in the PR.
