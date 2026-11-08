---
name: fleet-operations-ends-overdue-rental
sequence: 12
title: Fleet operations wants a ride that runs past the maximum rental time ended
system: velocity
actor: Fleet Operations
activity: Ride
task: Ride within the maximum rental time
status: delivered
date: 2026-11-08
supersedes:
superseded_by:
source: docs/domains/velocity/velocity-vision.md "Billing", "Key Business Rules to Know"; domain-expert answers DE-20, DE-21, DE-33 (2026-11-08)
---

# Fleet operations wants a ride that runs past the maximum rental time ended

## Intent

As a member of **Fleet Operations**, I want **every ride that runs past the maximum rental time ended automatically** so that **no bike is held forever and a bike that wasn't returned gets recovered**.

## Why

"A rental session must eventually end. Either through a proper return, or automatically when the maximum rental time is exceeded. There is no such thing as an eternal ride" ("Key Business Rules to Know"). "The maximum rental time exists so bikes don't get parked on someone's balcony for a week. It means: at this point, this is no longer a rental, it is a problem" ("Billing").

## Business outcome

- Rental Session Ended: the overdue ride is over, with the reason "maximum rental time exceeded" and no known place.
- Bike Withdrawn: the bike, which wasn't returned, is no longer offered to riders and has to be recovered.

## Success signal

- (suggested) No ride is ever longer than the maximum rental time.
- (suggested) Bikes that weren't returned are recovered.

## Business rules mentioned

- "If a session runs past the maximum rental time, the session is ended automatically and a penalty is applied." (vision) The maximum rental time is 12 hours; at exactly 12:00:00 it is still a ride (DE-20).
- The automatically ended ride ends nowhere known; the bike is withdrawn as not returned and treated as broken (DE-21).

## Scope

- **In:** ending every ride that has run longer than the maximum rental time, and withdrawing its bike.
- **Out:** the penalty and the hourly fees (`finance-penalizes-overdue-rental`); finding the bike (not modelled, DE-26); warning the rider before the limit (not requested).

## Related intents

- builds on: `rider-unlocks-reserved-bike`
- alternative to: `rider-returns-bike-at-station`, `rider-ends-ride-in-drop-zone`
- enables: `finance-penalizes-overdue-rental`

## Open questions

- Should the rider be warned before the maximum rental time is reached?

## Notes for the spec

- Accepted under the trainer's standing instruction of 2026-11-08 (autonomous run), to be reviewed in the PR.
