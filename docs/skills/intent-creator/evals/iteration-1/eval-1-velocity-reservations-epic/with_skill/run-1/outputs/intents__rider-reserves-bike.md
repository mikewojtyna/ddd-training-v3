---
name: rider-reserves-bike
sequence: 1
title: A rider wants to reserve a bike before unlocking it
system: velocity
actor: Rider
activity: Get a bike
task: Reserve a bike
status: draft
date: 2026-11-01
supersedes:
superseded_by:
source: velocity/velocity-vision.md "Renting a Bike", "Key Business Rules to Know"; conversation 2026-11-01
---

# A rider wants to reserve a bike before unlocking it

## Intent

As a **Rider**, I want to **reserve an available bike before I unlock it** so that **nobody else grabs that bike in the moment between choosing it and unlocking it**.

## Why

Product asked for it (conversation 2026-11-01: "riders should be able to reserve bikes in VeloCity before unlocking them").

The vision makes reserving the second step of renting a bike: "The bike is **reserved** for that rider. The reservation is short-lived; it exists only to prevent someone else from grabbing the same bike in the seconds it takes to unlock." It also backs one of the key business rules: "A bike can never be in two active rental sessions at the same time. Reservations exist to guarantee this during the seconds between 'I picked this bike' and 'I unlocked this bike.'"

## Business outcome

- Bike Reserved: one specific bike is held for this rider, and no other rider or technician can take it while the reservation lasts.

## Success signal

- (suggested) No rides where two riders tried to start a session on the same bike.
- (suggested) Fewer support contacts from riders who chose a bike and found it taken before they could unlock it.

## Business rules mentioned

- "The reservation is short-lived" (vision). No duration is stated.
- "Only an activated account can reserve and unlock a bike" (vision, "User Onboarding").
- The rider's location is validated before the bike is reserved: "they must be inside VeloCity's service area, standing near a bike" (vision, "Renting a Bike").
- "Only riders and technicians reserve and unlock bikes" (vision).
- "A broken bike is never offered to riders" (vision), so a rider can only reserve an available bike.

## Scope

- **In:** a rider with an activated account reserving one available bike, and that bike being held for them until they unlock it or the reservation ends.
- **Out:**
  - unlocking the reserved bike and starting the rental session (`rider-unlocks-reserved-bike`, candidate; renting itself is `rider-rents-bike-from-station`, candidate backfill), including releasing the reservation when the unlock fails;
  - a reservation that runs out before the unlock (`rider-takes-bike-freed-by-expired-reservation`);
  - a rider cancelling their own reservation (`rider-cancels-reservation`, candidate, not in the vision);
  - technicians reserving broken bikes (`technician-reserves-broken-bike`).

## Related intents

- enables: `rider-takes-bike-freed-by-expired-reservation`
- enables: `rider-unlocks-reserved-bike` (candidate)
- builds on: `rider-rents-bike-from-station` (candidate backfill: renting already exists)
- enables: `technician-reserves-broken-bike` (the vision says technicians go "through the same reserve-and-unlock steps", for a different goal and outcome)

## Open questions

- How long does a reservation last? The vision only says "short-lived" and "the seconds it takes to unlock".
- Can a rider reserve ahead of time, before reaching the bike? The request says "reserve bikes before unlocking them", but the vision validates that the rider is already "standing near a bike" before reserving. Is reservation only the instant hold at the bike, or also a hold while walking to it?
- How many reservations can one rider hold at a time? Not stated.
- Can a rider with a blocked account reserve? The vision says "Only an activated account can reserve" and "A blocked account cannot start new rentals", but doesn't say whether a blocked account counts as activated.
- Can a rider reserve any bike, or only bikes at stations? The vision says riders "walk up to any available VeloCity bike", and bikes can also stand in drop zones.
- Does a reservation cost anything? Nothing in the vision mentions a charge.

## Notes for the spec

- None stated by the user. Existing renting code lives in the Rentals context (`BikeStation.rent(...)`); the spec should decide whether reservations belong there.
