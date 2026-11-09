---
name: rider-unlocks-reserved-bike
sequence: 8
title: A rider wants to unlock the bike they reserved and start riding
system: velocity
actor: Rider
activity: Get a bike
task: Unlock a bike
status: delivered
date: 2026-11-08
supersedes:
superseded_by:
source: docs/domains/velocity/velocity-vision.md "Renting a Bike", "Key Business Rules to Know"; domain-expert answers DE-10, DE-14, DE-15 (2026-11-08)
---

# A rider wants to unlock the bike they reserved and start riding

## Intent

As a **Rider**, I want to **unlock the bike I reserved** so that **my ride starts right away**.

## Why

"After that, unlocking a bike is a single tap" ("Background"). Unlocking is the moment the rental session starts, and the vision is explicit that it can fail: "The unlocking fails (mechanical issue, connectivity problem, or for any other reason). The reservation is then released and the rider is free to try another bike" ("Renting a Bike"). The rider must never be stuck holding a bike that won't open.

## Business outcome

- Rental Session Started: the rider's ride on the bike has begun.
- Unlock Failed: the lock didn't open, the reservation is released and the bike is available again.

## Success signal

- (suggested) A low share of unlocks fail.
- (suggested) Riders whose unlock failed start a ride on another bike within a few minutes.

## Business rules mentioned

- "The rider unlocks the bike. The rental session starts." (vision)
- "If the unlocking fails, the reservation is released and the rider is free to try another bike." (vision) A failed unlock doesn't make the bike broken; it is available again (DE-15).
- "A bike can never be in two active rental sessions at the same time." (vision)
- The account isn't checked again at unlock; it was checked when reserving, and the reservation lasts 2 minutes (DE-14, DE-10).

## Scope

- **In:** the reservation holder unlocking the bike, the session starting, and a failed unlock releasing the reservation.
- **Out:** reserving (`rider-reserves-bike`); a reservation that runs out before unlocking (`fleet-operations-releases-expired-reservations`); a technician unlocking a broken bike (`technician-starts-maintenance`); ending the ride (`rider-returns-bike-at-station`, `rider-ends-ride-in-drop-zone`, `fleet-operations-ends-overdue-rental`).

## Related intents

- builds on: `rider-reserves-bike`
- enables: `rider-returns-bike-at-station`
- enables: `rider-ends-ride-in-drop-zone`

## Open questions

- The vision says "No unlock without a fully activated account" and "A blocked account cannot start new rentals", while DE-14 says the account isn't checked again at unlock. Is a rider blocked during their 2-minute reservation still allowed to start the ride?
- Can the rider still unlock in the very second the reservation period ends?

## Notes for the spec

- Accepted under the trainer's standing instruction of 2026-11-08 (autonomous run), to be reviewed in the PR.
