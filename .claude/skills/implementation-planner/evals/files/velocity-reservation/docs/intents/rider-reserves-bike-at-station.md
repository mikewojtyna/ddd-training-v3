---
name: rider-reserves-bike-at-station
sequence: 2
title: A rider wants to reserve a bike at a bike station
system: velocity
actor: Rider
activity: Get a bike
task: Reserve a bike
status: specified
date: 2026-11-02
supersedes:
superseded_by:
source: docs/domains/velocity/velocity-vision.md "Renting a Bike"; product meeting 2026-11-02
---

# A rider wants to reserve a bike at a bike station

## Intent

As a **Rider**, I want to **reserve a bike at a bike station** so that **nobody else takes it while I'm walking to the station**.

## Why

Riders complain that the bike they saw as available in the app was gone when they arrived at the station.

## Business outcome

- Bike Reserved: the rider holds one specific bike at the station.
- Reservation Expired: an unused reservation ended and the bike is available to others again.

## Success signal

- (suggested) Fewer complaints about bikes disappearing before the rider arrives.

## Business rules mentioned

- "The reservation is short-lived" (vision). Duration not stated.
- "One active reservation per rider" (product).

## Scope

- **In:** reserving one available bike at a station; the reservation running out if unused.
- **Out:** cancelling a reservation (`rider-cancels-reservation`, candidate); unlocking the bike (`rider-rents-bike-from-station`).

## Related intents

- builds on: `rider-rents-bike-from-station`

## Open questions

- How long does a reservation last?

## Notes for the spec

- None.
