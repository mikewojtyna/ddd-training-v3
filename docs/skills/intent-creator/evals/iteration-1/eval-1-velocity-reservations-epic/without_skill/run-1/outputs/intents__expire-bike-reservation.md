# Expire a bike reservation

- **Status:** Draft
- **System:** VeloCity
- **Bounded context:** Rentals
- **Actor:** The system (time passes); this affects riders and technicians
- **Part of:** Bike reservations. The related intents are [reserve-bike-for-ride](reserve-bike-for-ride.md), [reserve-broken-bike-for-maintenance](reserve-broken-bike-for-maintenance.md) and [release-reservation-on-failed-unlock](release-reservation-on-failed-unlock.md).

## Why

A reservation exists only to cover the short gap before an unlock. If the holder never unlocks, the bike must not stay held forever. For a rider's bike, that means the bike goes back to the pool. For a broken bike, it means another technician can pick it up.

## Desired outcome

A reservation that is not consumed by an unlock before its expiry time expires on its own. The bike then goes back to whatever state it was in before the reservation.

## Scenarios

### Rider's reservation expires

- **Given** an available bike reserved by a rider
- **When** the reservation's expiry time passes without an unlock
- **Then** **Bike Reservation Expired** is published and the bike is available to anyone again

### Technician's reservation expires

- **Given** a broken bike reserved by a technician
- **When** the reservation's expiry time passes without an unlock
- **Then** **Bike Reservation Expired** is published, the bike **stays broken and is not offered to riders**, and it waits until a technician reserves it again

### Unlock after expiry

- **Given** a reservation that has expired
- **When** the former holder tries to unlock the bike
- **Then** the unlock is refused (unless they reserve the bike again)

### Unlock just in time

- **Given** a reservation that has not expired yet
- **When** the holder unlocks the bike successfully
- **Then** the reservation is consumed and never expires

## Business rules

- Every reservation has an expiry time, set when it is made.
- A reservation that expires never leads to a rental session or to maintenance.
- Expiry gives the bike back to its state before the reservation: available for a rider's reservation, withdrawn for a technician's one.
- A reservation ends exactly once: consumed, expired or released. Expiry can't happen after a successful unlock.

## Assumptions (decisions made without the domain expert)

- **Expiry is free.** The vision mentions no fee or penalty for letting a reservation expire.
- **The holder is not notified by a separate business step.** The app may show it, but the domain has no "reservation expired notification".

## Out of scope

- The actual durations, see the open questions.
- Abuse prevention for repeated expiries.

## Open questions

1. What is the expiry time for a rider's reservation, and for a technician's? (See the length conflict in [reserve-broken-bike-for-maintenance](reserve-broken-bike-for-maintenance.md).)
2. Should repeated expiries by the same rider have any consequence?
3. Does Fleet Maintenance need to react to a technician's expired reservation, for example by flagging the bike for another technician?

## Related

- `velocity/velocity-vision.md`: *Renting a Bike* ("The reservation expires…"), *Broken Bikes & Maintenance* ("The reservation expires…").
