---
name: technician-retakes-broken-bike-after-reservation-expires
sequence: 4
title: A technician wants a broken bike whose reservation ran out to be reservable by technicians again
system: velocity
actor: Technician
activity: Repair a broken bike
task: Reserve a broken bike
status: draft
date: 2026-11-01
supersedes:
superseded_by:
source: velocity/velocity-vision.md "Broken Bikes & Maintenance"; conversation 2026-11-01
---

# A technician wants a broken bike whose reservation ran out to be reservable by technicians again

## Intent

As a **Technician**, I want **a broken bike whose reservation ran out to be free for any technician to reserve again, while staying away from riders**, so that **no broken bike is left waiting for a technician who never came**.

## Why

The vision says: "**The reservation expires** before the technician unlocks the bike. The bike is still broken, so it is not offered to riders — it simply waits until a technician reserves it again." This is where the technician's reservation differs from the rider's: when a rider's reservation expires the bike goes back to everyone (`rider-takes-bike-freed-by-expired-reservation`), but a broken bike must never go back to riders.

## Business outcome

- Broken Bike Reservation Expired: the technician who reserved the bike no longer holds it.
- Broken Bike Back in the Repair Queue: the bike is still broken, not offered to riders, and waiting for a technician to reserve it again.

## Success signal

- (suggested) No broken bike stays held by an expired reservation.
- (suggested) No rider ever gets offered a broken bike after a technician's reservation expired.

## Business rules mentioned

- "The bike is still broken, so it is not offered to riders — it simply waits until a technician reserves it again." (vision)
- "A broken bike is never offered to riders." (vision)

## Scope

- **In:** a technician's reservation of a broken bike running out before they unlock it, and the bike staying broken and reservable by technicians.
- **Out:**
  - making the reservation (`technician-reserves-broken-bike`);
  - the reservation being released because the unlock failed (part of `technician-unlocks-broken-bike`, candidate; the vision says "the bike stays broken, and a technician tries again later");
  - a rider's reservation running out (`rider-takes-bike-freed-by-expired-reservation`).

## Related intents

- follows: `technician-reserves-broken-bike`
- alternative to: `rider-takes-bike-freed-by-expired-reservation` (same event for riders, different consequence)

## Open questions

- Should anyone (the technician, a dispatcher) be told that the reservation expired, so the bike isn't forgotten? The vision only says the bike "waits".
- Can the same technician reserve it again right away?

## Notes for the spec

- None.
