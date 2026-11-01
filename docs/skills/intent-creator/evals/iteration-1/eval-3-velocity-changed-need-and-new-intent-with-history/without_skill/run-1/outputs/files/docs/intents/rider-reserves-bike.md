---
name: rider-reserves-bike
sequence: 4
title: A rider wants to reserve a bike before getting to the station
system: velocity
actor: Rider
activity: Get a bike
task: Reserve a bike
status: proposed
date: 2026-11-01
supersedes:
superseded_by:
source: product request after the pilot (relayed 2026-11-01)
---

# A rider wants to reserve a bike before getting to the station

## Intent

As a **Rider**, I want to **hold a bike for 15 minutes** so that **it is still there when I reach the station and can rent it**.

## Why

Product request after the pilot. Riders walking to a station have no guarantee a bike will still be available when they arrive. A short hold removes that uncertainty.

## Business outcome

- Bike Reserved: one bike is held for the rider and cannot be reserved or rented by anyone else.
- Reservation Expired: the rider did not rent the bike within 15 minutes, and the bike is available to everyone again.
- Reservation Fulfilled: the rider rented the reserved bike (`rider-rents-bike-from-station`), and the reservation ended.

## Success signal

- (suggested) Share of reservations that turn into a rental (vs. expire or get cancelled).
- (suggested) Number of reservations per day.

## Business rules mentioned

- A reservation holds a bike for 15 minutes (product).
- A rider can have at most one active reservation at a time (product).
- Assumed: a reserved bike cannot be reserved or rented by another rider while the hold lasts.
- Assumed: when 15 minutes pass without a rental, the reservation expires and the bike is available again.

## Scope

- **In:** a rider reserving one available bike at a station; the reservation expiring after 15 minutes; the reservation ending when the rider rents that bike.
- **Out:** technicians reserving broken bikes for maintenance; reservation fees or no-show penalties; extending a reservation.

## Related intents

- enables: `rider-rents-bike-from-station` (renting a reserved bike)

## Open questions

- Can a rider cancel a reservation before it expires?
- Can a rider with an active rental reserve another bike, or does "one active reservation" also mean "not while riding"?
- Is a reservation free? Is there a penalty for repeated no-shows?
- Does the reservation pick a specific bike, or "any bike at station X"?
- Is reserving optional (riders can still walk up and rent an unreserved bike), or does every rental go through a reservation?
- Who may reserve: only activated accounts, and not blocked ones?

## Notes for the spec

- `velocity/velocity-vision.md` already describes a reservation in "Renting a Bike", but as a seconds-long technical hold during unlock, also used by technicians. This 15-minute rider hold is a different, longer-lived business concept; decide whether they are the same concept with a different duration or two separate ones before modelling.
