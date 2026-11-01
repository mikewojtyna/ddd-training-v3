---
name: rider-returns-bike-to-any-station
sequence: 3
title: A rider wants to return a bike to any VeloCity station
system: velocity
actor: Rider
activity: Return the bike
task: Return at a station
status: accepted
date: 2026-11-01
supersedes: rider-returns-bike-to-origin-station
superseded_by:
source: operations decision after the pilot (relayed 2026-11-01)
---

# A rider wants to return a bike to any VeloCity station

## Intent

As a **Rider**, I want to **return my bike to any VeloCity station** so that **I can end my ride where my trip ends, my rental ends and I stop paying**.

## Why

During the pilot, riders had to bring the bike back to the station they rented it from (`rider-returns-bike-to-origin-station`). That made one-way trips impossible and frustrated riders. Operations decided that rebalancing bikes between stations with trucks is cheaper than losing riders, so the same-station rule is dropped.

## Business outcome

- Bike Returned: the bike is docked at a VeloCity station (any station) and the rental ended.

## Success signal

- (suggested) Share of rentals that end at a station other than the origin (one-way trips).
- (suggested) Share of rentals that end with a return within the allowed time.
- (suggested) Rebalancing cost per rental, to confirm operations' bet that trucks are cheaper than lost riders.

## Business rules mentioned

- A bike can be returned to any VeloCity station, not only the one it was rented from (operations, after the pilot). Replaces "a bike must be returned to the same station it was rented from".
- Unchanged: a rental can't be longer than 11 hours.

## Scope

- **In:** returning the bike at any VeloCity station, including its origin station.
- **Out:** drop-zone and unauthorized-zone returns (not planned yet); rebalancing the fleet between stations (an operations concern, not part of the rider's return).

## Related intents

- supersedes: `rider-returns-bike-to-origin-station`
- follows: `rider-rents-bike-from-station`

## Open questions

- What happens when the target station has no free dock? Is the return refused, does the rider get sent to the nearest station with space, or is extra time granted?
- Does returning to a different station change the price (e.g. a one-way fee), or is it free like an origin return?
- Who triggers rebalancing, and does the Rentals context need to publish anything for it beyond Bike Returned?

## Notes for the spec

- The pilot behaviour checked the return station against the rental's origin station; that check must go wherever it is enforced.
