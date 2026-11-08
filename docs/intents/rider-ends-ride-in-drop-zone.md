---
name: rider-ends-ride-in-drop-zone
sequence: 11
title: A rider wants to end the ride where they are, outside a station
system: velocity
actor: Rider
activity: Return the bike
task: End the ride in a drop zone
status: specified
date: 2026-11-08
supersedes:
superseded_by:
source: docs/domains/velocity/velocity-vision.md "Returns" ("Drop Zone Return (Free-Floating)"), "Key Business Rules to Know"; domain-expert answers DE-23 (2026-11-08)
---

# A rider wants to end the ride where they are, outside a station

## Intent

As a **Rider**, I want to **end my ride where I am, without a station** so that **I can leave the bike close to where I'm going**.

## Why

"Every ride starts in seconds and ends anywhere where the city permits it" ("Background"). "The rider ends the ride at their current location. The ride always ends where the rider is" and "Riders can always end their ride; where they end it decides what they pay" ("Returns", "Key Business Rules to Know"). The lock can still fail outside a station.

## Business outcome

- Rental Session Ended: the ride is over, at the rider's position, with the reason it ended (a free-floating return or a lock failure).
- Bike Withdrawn: a bike whose lock failed is no longer offered to riders.

## Success signal

- (suggested) Share of rides ended outside a station, and how many of them end in drop zones.

## Business rules mentioned

- "The ride always ends where the rider is." (vision)
- "Bike locking can still fail (bike needs to be locked outside the station, too)." (vision)
- A lock failure at a free-floating return is treated exactly like one at a station: the ride ends at that moment and the bike is withdrawn, wherever the bike is (DE-23).
- "Riders can always end their ride; where they end it decides what they pay." (vision)

## Scope

- **In:** ending a running ride at the rider's position, in a drop zone or anywhere else; lock closes or fails.
- **Out:** what the ride and the return cost, drop zone fee or unauthorized zone return fee (`finance-prices-free-floating-return-by-zone`); returning at a station (`rider-returns-bike-at-station`); a ride ended automatically (`fleet-operations-ends-overdue-rental`).

## Related intents

- builds on: `rider-unlocks-reserved-bike`
- alternative to: `rider-returns-bike-at-station`
- enables: `finance-prices-free-floating-return-by-zone`

## Open questions

- Can a rider report a fault when ending the ride outside a station, as they can at a station?

## Notes for the spec

- Accepted under the trainer's standing instruction of 2026-11-08 (autonomous run), to be reviewed in the PR.
