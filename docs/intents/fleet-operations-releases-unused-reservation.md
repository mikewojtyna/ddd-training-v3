---
name: fleet-operations-releases-unused-reservation
sequence: 3
title: Fleet operations wants unused reservations released
system: velocity
actor: Fleet Operations
activity: Keep the fleet available
task: Release unused reservations
status: draft
date: 2026-11-07
supersedes:
superseded_by:
source: velocity/briefs/VELO-142-reserve-bike-from-app.md (product meeting 2026-11-02; Ops comment)
---

# Fleet operations wants unused reservations released

## Intent

As a member of **Fleet Operations**, I want to **have a reservation the rider doesn't use end on its own** so that **the bike goes back to the pool and keeps earning money**.

## Why

"If the rider doesn't show up, the bike has to go back to the pool automatically" (product meeting). Ops points out that bikes sitting reserved don't earn money (VELO-142). The vision describes the same: "The reservation expires before the rider unlocks the bike. The bike returns to the pool and is available to anyone."

## Business outcome

- Reservation Expired: the rider didn't unlock the reserved bike in time, the reservation ended, and the bike is available to anyone again.

## Success signal

- (suggested) Share of reservations that expire unused, and the average time a bike spends reserved but unused.

## Business rules mentioned

- The reservation holds the bike "for about 10–15 minutes" (product meeting), after which "the bike has to go back to the pool automatically".

## Scope

- **In:** ending a rider's unused reservation once its time is up, and making the bike available again.
- **Out:** the rider cancelling on purpose (`rider-cancels-reservation`); a technician's reservation of a broken bike running out (`technician-reserves-broken-bike`); a warning before the reservation runs out (`rider-is-warned-before-reservation-expires`, candidate).

## Related intents

- follows: `rider-reserves-bike-at-station`

## Open questions

- Same duration question as `rider-reserves-bike-at-station`: when exactly is a reservation "unused"?
- Is there any consequence for a rider who keeps letting reservations expire? (Not mentioned anywhere.)

## Notes for the spec

- The ticket suggests a Redis expiry (15 minutes) to implement this.
