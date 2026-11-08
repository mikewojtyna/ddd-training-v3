---
name: fleet-manager-adds-bike-to-fleet
sequence: 6
title: A fleet manager wants to add a new bike to the fleet at a station
system: velocity
actor: Fleet Manager
activity: Keep the fleet healthy
task: Add a bike to the fleet
status: specified
date: 2026-11-08
supersedes:
superseded_by:
source: docs/domains/velocity/velocity-vision.md "Background", "Renting a Bike"; domain-expert answers DE-18 (2026-11-08)
---

# A fleet manager wants to add a new bike to the fleet at a station

## Intent

As a **Fleet Manager**, I want to **add a new bike to the fleet at a station** so that **riders find it there and can start riding it**.

## Why

The vision promises "every resident and visitor a bike within a short walk of wherever they are", but never says where bikes come from. The domain expert answered that a fleet manager adds each new bike to the fleet at a station, and from then on it is available (DE-18). Without this, there is no bike for anyone to reserve, so it is the first thing the fleet needs.

## Business outcome

- Bike Added to Fleet: a new bike is part of the fleet and is available to riders at the station where it was put.

## Success signal

- (suggested) Every bike delivered to VeloCity is available to riders on the day it arrives.

## Business rules mentioned

- "A fleet manager adds a new bike to the fleet at a station. From then on it is available." (DE-18)
- Bikes are only ever inside the area VeloCity operates in (DE-12).

## Scope

- **In:** adding one new bike to the fleet at one station, after which it is available.
- **Out:** reserving the bike (`rider-reserves-bike`); returning a fixed bike to circulation (`technician-returns-fixed-bike-to-circulation`); taking a bike out of circulation (`fleet-operations-withdraws-bike-signalling-fault`, `fleet-operations-takes-broken-bike-out-of-circulation`); retiring a bike for good (not planned, DE-61).

## Related intents

- enables: `rider-reserves-bike`
- alternative to: `technician-returns-fixed-bike-to-circulation` (both make a bike available; this one for a bike new to the fleet)

## Open questions

- Can a new bike be added to the fleet in a drop zone, or only at a station? DE-18 only mentions stations.

## Notes for the spec

- Accepted under the trainer's standing instruction of 2026-11-08 (autonomous run), to be reviewed in the PR.
