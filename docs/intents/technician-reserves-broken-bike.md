---
name: technician-reserves-broken-bike
sequence: 33
title: A technician wants to reserve a broken bike
system: velocity
actor: Technician
activity: Keep the fleet healthy
task: Reserve a broken bike
status: specified
date: 2026-11-08
supersedes:
superseded_by:
source: docs/domains/velocity/velocity-vision.md "Broken Bikes & Maintenance", "Renting a Bike", "Domain Vocabulary" (Reservation); docs/bounded-contexts/velocity/domain-expert-answers.md DE-11, DE-16, DE-26
---

# A technician wants to reserve a broken bike

## Intent

As a **Technician**, I want to **reserve a broken bike** so that **no other technician takes it while I'm on my way to it**.

## Why

The maintenance flow "reuses the same steps riders use to get a bike": a technician reserves the broken bike, and "the reservation keeps anyone else from grabbing it while the technician is on the way" ("Broken Bikes & Maintenance"). Because the technician has to travel to the bike, the reservation lasts much longer than a rider's (DE-11). If it runs out, "the bike is still broken, so it is not offered to riders — it simply waits until a technician reserves it again".

## Business outcome

- Bike Reserved: the broken bike is held for one technician.
- Reservation Expired: an unused technician reservation ends and the bike waits, still broken, for any technician.

## Success signal

- (suggested) Fewer cases of two technicians travelling to the same broken bike.
- (suggested) Share of technician reservations that expire unused.

## Business rules mentioned

- A technician's reservation lasts 30 minutes (DE-11).
- Only a technician can reserve a broken bike; technicians reserve only broken bikes (DE-16).
- The 50 m rule applies to riders only; a technician reserves without giving a position (DE-26).
- "The reservation expires before the technician unlocks the bike. The bike is still broken ... it simply waits until a technician reserves it again." (vision)

## Scope

- **In:** reserving a broken bike; finding which bikes are broken; the reservation running out.
- **Out:** unlocking the bike and starting maintenance (`technician-starts-maintenance`); riders' reservations (`rider-reserves-bike`, `fleet-operations-releases-expired-reservations`); finding a bike that was not returned on the street (not modelled, DE-26).

## Related intents

- follows: `fleet-operations-takes-broken-bike-out-of-circulation`
- follows: `technician-reports-fault`
- enables: `technician-starts-maintenance`
- builds on: `rider-reserves-bike`

## Open questions

- May a technician hold several reservations at the same time?

## Notes for the spec

- Accepted under the trainer's standing instruction of 2026-11-08 (autonomous run), to be reviewed in the PR.
