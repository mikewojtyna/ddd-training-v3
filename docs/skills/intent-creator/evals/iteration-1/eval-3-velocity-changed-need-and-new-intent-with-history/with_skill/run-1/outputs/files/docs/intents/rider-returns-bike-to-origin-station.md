---
name: rider-returns-bike-to-origin-station
sequence: 2
title: A rider wants to return a bike to the station it was rented from
system: velocity
actor: Rider
activity: Return the bike
task: Return at a station
status: superseded
date: 2026-10-20
supersedes:
superseded_by: rider-returns-bike-to-any-station
source: workshop discussion 2026-10-20
---

# A rider wants to return a bike to the station it was rented from

## Intent

As a **Rider**, I want to **return my bike to the station I rented it from** so that **my rental ends and I stop paying**.

## Why

Riders need a way to end a rental. For the pilot, operations wanted bikes to come back to their origin station so each station keeps its fleet without rebalancing trucks.

## Business outcome

- Bike Returned: the bike is back at its origin station and the rental ended.

## Success signal

- (suggested) Share of rentals that end with a return within the allowed time.

## Business rules mentioned

- "A bike must be returned to the same station it was rented from" (operations, pilot).

## Scope

- **In:** returning the bike at its origin station.
- **Out:** drop-zone returns (not planned yet).

## Related intents

- follows: `rider-rents-bike-from-station`

## Open questions

- None.

## Notes for the spec

- None.
