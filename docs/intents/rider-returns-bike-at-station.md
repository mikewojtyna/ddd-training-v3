---
name: rider-returns-bike-at-station
sequence: 10
title: A rider wants to return the bike at a station and end the ride
system: velocity
actor: Rider
activity: Return the bike
task: Return at a station
status: specified
date: 2026-11-08
supersedes:
superseded_by:
source: docs/domains/velocity/velocity-vision.md "Returns" ("Station Return"), "Broken Bikes & Maintenance"; domain-expert answers DE-22, DE-24 (2026-11-08)
---

# A rider wants to return the bike at a station and end the ride

## Intent

As a **Rider**, I want to **return my bike at a station** so that **my ride ends there and I stop paying for it**.

## Why

"The rider brings the bike back to an official station. The system attempts to lock the bike in the station. If locking succeeds, the session ends cleanly. If locking fails, the session is ended immediately and the rider is told why: the session cannot silently drift" ("Returns"). A rider can also report a fault while returning ("Broken Bikes & Maintenance"), which takes the bike away from the next rider.

## Business outcome

- Rental Session Ended: the ride is over, at a known station, with the reason it ended (a station return or a lock failure).
- Bike Withdrawn: a bike whose lock failed, or about which the rider reported a fault, is no longer offered to riders.

## Success signal

- (suggested) No ride keeps running after the rider has brought the bike back to a station.
- (suggested) Faults reported at return lead to the bike being taken away from riders before anyone else rents it.

## Business rules mentioned

- "If locking succeeds, the session ends cleanly." (vision)
- "If locking fails, the session is ended immediately and the rider is told why. The rider is not charged extra because our lock failed. The bike is treated as broken and waits for a technician." (vision)
- "A rider reports a fault when returning the bike to a station" makes the bike broken (vision). The ride still ends normally, with normal fees (DE-24).
- The rider names the station they are at, and that is trusted (DE-22).

## Scope

- **In:** ending a running ride at a station: lock closes, lock fails, fault reported at return.
- **Out:** what the ride costs (`finance-prices-ride-by-tiered-tariff`); ending the ride outside a station (`rider-ends-ride-in-drop-zone`); a ride ended automatically (`fleet-operations-ends-overdue-rental`); a fault the bike itself signalled during the ride (`fleet-operations-withdraws-bike-signalling-fault`); repairing the bike (`technician-reserves-broken-bike` and following).

## Related intents

- builds on: `rider-unlocks-reserved-bike`
- alternative to: `rider-ends-ride-in-drop-zone`
- enables: `finance-prices-ride-by-tiered-tariff`
- enables: `technician-reserves-broken-bike`

## Open questions

- None.

## Notes for the spec

- Accepted under the trainer's standing instruction of 2026-11-08 (autonomous run), to be reviewed in the PR.
