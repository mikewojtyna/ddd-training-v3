---
name: rider-reserves-bike
sequence: 7
title: A rider wants to reserve a bike they are standing near
system: velocity
actor: Rider
activity: Get a bike
task: Reserve a bike
status: delivered
date: 2026-11-08
supersedes:
superseded_by:
source: docs/domains/velocity/velocity-vision.md "Renting a Bike", "User Onboarding", "Account Blocks & Debt Recovery", "Key Business Rules to Know"; domain-expert answers DE-10, DE-12, DE-13, DE-14, DE-16, DE-17 (2026-11-08)
---

# A rider wants to reserve a bike they are standing near

## Intent

As a **Rider**, I want to **reserve the bike I'm standing next to** so that **nobody else grabs it in the seconds it takes me to unlock it**.

## Why

The vision's core promise is that "every ride starts in seconds". Renting starts with validating that the rider is near the bike and reserving it, because "it exists only to prevent someone else from grabbing the same bike in the seconds it takes to unlock" ("Renting a Bike"). The reservation is what guarantees that "a bike can never be in two active rental sessions at the same time" ("Key Business Rules to Know").

## Business outcome

- Bike Reserved: one specific bike is held for one rider for a short reservation period.

## Success signal

- (suggested) Almost no reservations are rejected because someone else took the bike first.
- (suggested) Seconds from reservation to the start of the ride stay low.

## Business rules mentioned

- "The rider's location is validated: they must be inside VeloCity's service area, standing near a bike." (vision) Near means within 50 metres (DE-12).
- "The reservation is short-lived." (vision) A rider's reservation lasts 2 minutes (DE-10).
- "Only an activated account can reserve and unlock a bike." "A blocked account cannot start new rentals." (vision) The account is checked when reserving (DE-14).
- "One rider, one bike": a rider with a reservation or a running rental session can't reserve another bike (DE-13).
- "A broken bike is never offered to riders." (vision) Only a technician can reserve a broken bike (DE-16).
- Reservations are free (DE-17).

## Scope

- **In:** a rider reserving one available bike near them; the reasons a reservation is refused.
- **Out:** unlocking the reserved bike (`rider-unlocks-reserved-bike`); releasing a reservation that wasn't used (`fleet-operations-releases-expired-reservations`); a technician reserving a broken bike (`technician-reserves-broken-bike`); cancelling a reservation on purpose (not requested).

## Related intents

- builds on: `fleet-manager-adds-bike-to-fleet`
- builds on: `rider-signs-user-agreement` (the account must be activated)
- enables: `rider-unlocks-reserved-bike`
- follows: `fleet-operations-releases-expired-reservations`

## Open questions

- Does "near" include a rider standing exactly 50 metres away?

## Notes for the spec

- Accepted under the trainer's standing instruction of 2026-11-08 (autonomous run), to be reviewed in the PR.
