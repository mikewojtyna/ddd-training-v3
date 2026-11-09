---
name: rider-cancels-reservation
sequence: 36
title: A rider wants to cancel a reservation they no longer need
system: velocity
actor: Rider
activity: Get a bike
task: Cancel a reservation
status: specified
date: 2026-11-10
supersedes:
superseded_by:
source: product request 2026-11-10 ("riders who change their mind block the bike for the full two minutes")
---

# A rider wants to cancel a reservation they no longer need

## Intent

As a **Rider**, I want to **cancel my reservation** so that **the bike is free for somebody else right away when I
change my mind**.

## Why

A reservation holds the bike for 2 minutes (DE-10). A rider who notices a flat tyre or decides to walk keeps the bike
away from everybody else until the reservation expires. Product wants the bike back in circulation immediately.

## Business outcome

- Reservation Cancelled: the bike is free and available to any rider again before the reservation period ends.

## Success signal

- (suggested) Fewer reservations end by expiry.

## Business rules mentioned

- Only the rider who holds a reservation may cancel it.
- A ride that has started can't be "cancelled"; the rider returns the bike instead.

## Scope

- **In:** a rider cancelling their own reservation; the reasons a cancellation is refused.
- **Out:** a technician cancelling a service reservation; any fee (reservations are free, DE-17).

## Related intents

- builds on: `rider-reserves-bike`
- follows: `fleet-operations-releases-expired-reservations`

## Open questions

- None.

## Notes for the spec

- Accepted by the product owner on 2026-11-10.
