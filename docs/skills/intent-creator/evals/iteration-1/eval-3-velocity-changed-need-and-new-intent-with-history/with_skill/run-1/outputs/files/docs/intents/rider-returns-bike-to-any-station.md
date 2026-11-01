---
name: rider-returns-bike-to-any-station
sequence: 3
title: A rider wants to return a bike to any VeloCity station
system: velocity
actor: Rider
activity: Return the bike
task: Return at a station
status: draft
date: 2026-11-01
supersedes: rider-returns-bike-to-origin-station
superseded_by:
source: conversation 2026-11-01 (operations decision after the pilot); velocity/velocity-vision.md "Station Return"
---

# A rider wants to return a bike to any VeloCity station

## Intent

As a **Rider**, I want to **return my bike to any VeloCity station** so that **I can end my rental where my trip ends, not where it started**.

## Why

This replaces `rider-returns-bike-to-origin-station`. During the pilot, operations required every bike to come back to the station it was rented from, so that each station kept its own fleet without rebalancing trucks. After the pilot, operations changed that policy: riders may now return a bike to any VeloCity station. In operations' words, "rebalancing trucks are cheaper than angry riders": being forced back to the origin station frustrated riders more than moving bikes between stations costs.

The need to end a rental at a station stays the same. What changed is which stations count: any VeloCity station instead of only the origin station.

## Business outcome

- Bike Returned: the bike is at a VeloCity station (the origin station or another one) and the rental ended.

## Success signal

- (suggested) Fewer rider complaints about having to go back to the origin station.
- (suggested) Share of rentals that end at a station other than the origin station.
- (suggested) Rebalancing cost stays below the cost of the rider frustration it replaces (operations' trade-off).

## Business rules mentioned

- "Riders can return a bike to any VeloCity station, not just the one they rented from" (operations, after the pilot).

## Scope

- **In:** returning the bike at any VeloCity station, including the origin station.
- **Out:** drop-zone returns (not planned yet); moving bikes between stations to keep the fleet balanced (candidate `fleet-operations-rebalances-stations`, not written); a return when the station can't lock the bike (part of the vision's "Station Return", see open questions).

## Related intents

- follows: `rider-rents-bike-from-station`

## Open questions

- Is there any pricing difference when a bike is returned to a station other than the origin station, or is it the same as an origin-station return?
- What happens when the chosen station is full? Does the rider get to return there anyway, or must they find another station?
- Does operations need its own intent for rebalancing (`fleet-operations-rebalances-stations`), or is rebalancing handled outside VeloCity's product for now?
- The vision's "Station Return" also covers a lock failure at return (session ended, bike treated as broken). Is that part of this intent or a separate one?

## Notes for the spec

- None.
