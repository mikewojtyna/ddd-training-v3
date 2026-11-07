---
name: technician-reserves-broken-bike
sequence: 5
title: A technician wants to reserve a broken bike before driving to it
system: velocity
actor: Technician
activity: Keep the fleet available
task: Reserve a broken bike for repair
status: draft
date: 2026-11-07
supersedes:
superseded_by:
source: velocity/briefs/VELO-142-reserve-bike-from-app.md (Ops comment); velocity/velocity-vision.md "Broken Bikes & Maintenance"
---

# A technician wants to reserve a broken bike before driving to it

## Intent

As a **Technician**, I want to **reserve a broken bike while I drive to it** so that **two technicians aren't sent to the same bike**.

## Why

Ops (Fleet) asked for it in VELO-142: "they'd like to hold a broken bike while driving to it, so riders' reports don't send two technicians to the same bike". The vision already describes it: "A technician reserves the bike. The reservation keeps anyone else from grabbing it while the technician is on the way."

## Business outcome

- Broken Bike Reserved: one broken bike is held for one technician, and no other technician picks it up.

## Success signal

- (suggested) No more repair trips where two technicians arrive at the same bike.

## Business rules mentioned

- When a technician's reservation expires, "the bike is still broken, so it is not offered to riders; it simply waits until a technician reserves it again." (vision)
- "A technician's unlock never starts a rental session." (vision)

## Scope

- **In:** a technician reserving one broken bike.
- **Out:** riders reserving bikes (`rider-reserves-bike-at-station`); repairing the bike and returning it to circulation (not captured yet).

## Related intents

- alternative to: `rider-reserves-bike-at-station`

## Open questions

- How long does a technician's reservation hold the bike? Is it the same as a rider's?
- Ops asked "Can we do both in one go?". Is this intent in the same release as the rider's reservation, or later?

## Notes for the spec

- None.
