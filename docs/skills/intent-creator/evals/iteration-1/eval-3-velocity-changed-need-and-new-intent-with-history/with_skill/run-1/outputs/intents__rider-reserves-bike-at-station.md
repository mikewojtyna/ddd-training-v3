---
name: rider-reserves-bike-at-station
sequence: 4
title: A rider wants to reserve a bike at a bike station
system: velocity
actor: Rider
activity: Get a bike
task: Reserve a bike
status: draft
date: 2026-11-01
supersedes:
superseded_by:
source: conversation 2026-11-01 (product request after the pilot); velocity/velocity-vision.md "Renting a Bike"
---

# A rider wants to reserve a bike at a bike station

## Intent

As a **Rider**, I want to **reserve a bike at a bike station for a short while** so that **the bike is still there for me when I arrive to unlock it**.

## Why

Product wants reservations after the pilot. Today a rider can only rent a bike that is available at the moment they unlock it, so a bike they saw as available can be gone by the time they reach the station. A reservation holds one bike for the rider for a limited time, which turns "I hope a bike is there" into "my bike is waiting".

The vision already mentions a reservation as part of renting ("Renting a Bike", step 2), but only as a hold of a few seconds while unlocking. Product's request is a longer hold the rider makes before reaching the bike. See open questions.

## Business outcome

- Bike Reserved: one specific bike at the station is held for the rider and is not available to anyone else while the reservation lasts.

## Success signal

- (suggested) Share of rentals that start from a reservation.
- (suggested) Fewer rider complaints about a bike being gone on arrival at the station.
- (suggested) Share of reservations that end in a rental rather than expiring.

## Business rules mentioned

- "Hold a bike for 15 minutes" (product).
- "One active reservation per rider" (product).
- "Only an activated account can reserve and unlock a bike" (vision, "User Onboarding").
- "A bike can never be in two active rental sessions at the same time. Reservations exist to guarantee this…" (vision, "Key Business Rules to Know").

## Scope

- **In:** a rider reserving one available bike at a station; the reservation holding that bike for the rider; renting the reserved bike while the reservation lasts (via `rider-rents-bike-from-station`).
- **Out:** releasing a reservation that wasn't used in time (`fleet-operations-releases-expired-reservation`); the rider cancelling their own reservation (`rider-cancels-reservation`, candidate); releasing the reservation when unlocking fails (`rider-reservation-released-after-failed-unlock`, candidate); technicians reserving a broken bike (`technician-reserves-broken-bike`, candidate); renting itself (`rider-rents-bike-from-station`).

## Related intents

- builds on: `rider-rents-bike-from-station`
- enables: `fleet-operations-releases-expired-reservation`

## Open questions

- The vision describes the reservation as lasting only "the seconds it takes to unlock" and as a step inside every rental. Product now wants a 15-minute hold. Is the reservation optional (a rider may still rent an available bike without reserving), or does every rental now start with a reservation?
- Is a reservation free, or is there a fee (for example when it expires unused)?
- Can a rider with an active rental also hold a reservation?
- When a rider tries to reserve while they already hold one, should the attempt be refused, or should the new reservation replace the old one?
- Does the rider have to be near the station to reserve (the vision validates the rider's location when renting), or can they reserve from anywhere?
- Do technicians follow the same 15-minute and one-reservation rules? The vision says they "go through the same reserve-and-unlock steps".

## Notes for the spec

- None.
