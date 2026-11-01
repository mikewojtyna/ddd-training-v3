---
name: technician-reserves-broken-bike
sequence: 3
title: A technician wants to reserve a broken bike before going to pick it up
system: velocity
actor: Technician
activity: Repair a broken bike
task: Reserve a broken bike
status: draft
date: 2026-11-01
supersedes:
superseded_by:
source: velocity/velocity-vision.md "Who Uses the System / Technicians", "Broken Bikes & Maintenance"; conversation 2026-11-01
---

# A technician wants to reserve a broken bike before going to pick it up

## Intent

As a **Technician**, I want to **reserve a broken bike before I go to pick it up** so that **nobody else grabs it while I'm on the way, and I don't travel to a bike that is no longer there**.

## Why

Product asked for it (conversation 2026-11-01: "and technicians too when they go pick up a broken one").

The vision makes it the second step of the maintenance flow: "A technician **reserves the bike**. The reservation keeps anyone else from grabbing it while the technician is on the way." Technicians "may reserve and unlock a bike just like a rider can, but they do it to repair the bike, not to ride it."

This is a separate need from `rider-reserves-bike`: a different actor with a different goal (repairing, not riding), and the bike stays out of circulation the whole time.

## Business outcome

- Broken Bike Reserved: one specific broken bike is held for this technician, and no other technician can take it while the reservation lasts. It is still not offered to riders.

## Success signal

- (suggested) Fewer trips where two technicians went for the same broken bike.
- (suggested) Shorter time from a bike being reported broken to maintenance starting.

## Business rules mentioned

- "The reservation keeps anyone else from grabbing it while the technician is on the way." (vision)
- "A broken bike is never offered to riders. It stays out of circulation until a technician fixes it." (vision)
- "Only riders and technicians reserve and unlock bikes. A rider's unlock starts a rental session; a technician's unlock starts maintenance." (vision)

## Scope

- **In:** a technician reserving one broken bike, and that bike being held for them until they unlock it or the reservation ends.
- **Out:**
  - the bike being reported broken (`bike-is-reported-broken`, candidate);
  - unlocking the bike and starting maintenance (`technician-unlocks-broken-bike`, candidate), including releasing the reservation when the unlock fails;
  - a technician's reservation running out (`technician-retakes-broken-bike-after-reservation-expires`);
  - riders reserving bikes (`rider-reserves-bike`).

## Related intents

- follows: `bike-is-reported-broken` (candidate)
- enables: `technician-retakes-broken-bike-after-reservation-expires`
- enables: `technician-unlocks-broken-bike` (candidate)
- builds on: `rider-reserves-bike` (the vision says technicians go "through the same reserve-and-unlock steps")

## Open questions

- How long does a technician's reservation last? The vision says it covers the time the technician "is on the way", which is far longer than the rider's "seconds it takes to unlock". Is it the same rule as for riders or a separate one?
- Can a technician reserve a bike that is not reported broken? The vision says a technician "may reserve and unlock a bike just like a rider can", but every example is about broken bikes.
- How many broken bikes can one technician hold at a time (for example, collecting several on one trip)?
- Is a broken bike assigned to a technician by someone, or does any technician pick one and reserve it?

## Notes for the spec

- None.
