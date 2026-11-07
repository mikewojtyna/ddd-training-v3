---
name: rider-rents-bike-from-station
sequence: 1
title: A rider wants to rent a bike from a bike station
system: velocity
actor: Rider
activity: Get a bike
task: Unlock a bike
status: delivered
date: 2026-11-01
supersedes:
superseded_by:
source: velocity/velocity-vision.md "Renting a Bike"; existing Rentals behaviour (backfilled)
---

# A rider wants to rent a bike from a bike station

## Intent

As a **Rider**, I want to **rent an available bike from a bike station** so that **I can ride it across the city**.

## Why

Backfilled: this behaviour already exists. Renting is the core of VeloCity's business: every ride and every fee starts with a rental (vision, "Renting a Bike").

## Business outcome

- Bike Rented: the bike left the station and a rental session started for the rider.

## Success signal

- (suggested) Number of rentals started per day.

## Business rules mentioned

- "A bike can never be in two active rental sessions at the same time." (vision, Key Business Rules)
- A rental can't be longer than 11 hours.

## Scope

- **In:** renting one available bike at a station.
- **Out:** reserving the bike first (`rider-reserves-bike-at-station`); returning the bike (not captured yet).

## Related intents

- enables: `rider-reserves-bike-at-station`

## Open questions

- None.

## Notes for the spec

- None.
