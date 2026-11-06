---
name: rider-reports-faulty-bike
sequence: 3
title: A rider wants to report a faulty bike
system: velocity
actor: Rider
activity: Ride
task: Report a problem
status: draft
date: 2026-11-04
supersedes:
superseded_by:
source: conversation 2026-11-04
---

# A rider wants to report a faulty bike

## Intent

As a **Rider**, I want to **report that my bike is faulty** so that **it gets repaired and I'm not charged for a broken ride**.

## Why

Riders end up paying for rides on bikes with flat tyres or broken brakes.

## Business outcome

- Faulty Bike Reported: the bike is flagged as faulty by a rider.

## Success signal

- (suggested) Faulty bikes are withdrawn faster.

## Business rules mentioned

- None stated.

## Scope

- **In:** reporting a fault during or right after a ride.
- **Out:** the repair itself.

## Related intents

- follows: `rider-rents-bike-from-station`

## Open questions

- Is the ride free when the bike is faulty?

## Notes for the spec

- None.
