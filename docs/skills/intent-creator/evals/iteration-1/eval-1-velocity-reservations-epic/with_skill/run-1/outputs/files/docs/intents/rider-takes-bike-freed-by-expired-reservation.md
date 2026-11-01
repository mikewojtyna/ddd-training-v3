---
name: rider-takes-bike-freed-by-expired-reservation
sequence: 2
title: A rider wants a bike whose reservation ran out to be available again
system: velocity
actor: Rider
activity: Get a bike
task: Reserve a bike
status: draft
date: 2026-11-01
supersedes:
superseded_by:
source: velocity/velocity-vision.md "Renting a Bike"; conversation 2026-11-01
---

# A rider wants a bike whose reservation ran out to be available again

## Intent

As a **Rider**, I want **a bike whose reservation ran out without being unlocked to become available again** so that **a bike nobody is going to ride isn't kept away from me**.

## Why

Reservations only make sense if they end. The vision says: "**The reservation expires** before the rider unlocks the bike. The bike returns to the pool and is available to anyone." Without this, every abandoned reservation would take a working bike out of circulation, which goes against the vision's goal of "a bike within a short walk of wherever they are".

## Business outcome

- Reservation Expired: the rider who reserved the bike no longer holds it.
- Bike Available Again: the bike is back in the pool and any rider can reserve it.

## Success signal

- (suggested) No working bike stays held by a reservation longer than the agreed reservation time.
- (suggested) Few complaints from riders who saw a bike but could not reserve it because someone else's reservation never ended.

## Business rules mentioned

- "The reservation expires before the rider unlocks the bike. The bike returns to the pool and is available to anyone." (vision)
- The reservation is "short-lived" (vision). No duration is stated.

## Scope

- **In:** a rider's reservation running out before they unlock the bike, and the bike returning to the pool for everyone.
- **Out:**
  - making the reservation (`rider-reserves-bike`);
  - the reservation being released because the unlock failed (part of `rider-unlocks-reserved-bike`, candidate);
  - a rider ending the reservation on purpose (`rider-cancels-reservation`, candidate);
  - a technician's reservation running out on a broken bike, which does not make the bike available to riders (`technician-retakes-broken-bike-after-reservation-expires`).

## Related intents

- follows: `rider-reserves-bike`
- alternative to: `rider-cancels-reservation` (candidate)

## Open questions

- How long until a reservation expires? See `rider-reserves-bike`.
- Is the rider who held the reservation told that it expired? The vision doesn't say.
- Can the same rider reserve the same bike again right after their reservation expired?

## Notes for the spec

- None.
