# Reserve a bike for a ride

- **Status:** Draft
- **System:** VeloCity
- **Bounded context:** Rentals
- **Actor:** Rider
- **Part of:** Bike reservations. The related intents are [reserve-broken-bike-for-maintenance](reserve-broken-bike-for-maintenance.md), [expire-bike-reservation](expire-bike-reservation.md) and [release-reservation-on-failed-unlock](release-reservation-on-failed-unlock.md).

## Why

A rider picks a bike and needs a few seconds to unlock it. In that gap nobody else may grab the same bike. A reservation is a brief, exclusive hold on one bike for one rider, from the moment of selection until the unlock. It is how VeloCity guarantees that **a bike is never in two active rental sessions at the same time**.

A reservation is not a phase of its own. It is a convenience step before the *Rental Session Started* pivot, with no fee and no billing (see `velocity/bounded-context-discovery-session.md`).

## Desired outcome

An eligible rider standing near an available bike can reserve it. Until the reservation is used, expires or is released, the bike can't be reserved or unlocked by anyone else.

## Scenarios

### Rider reserves an available bike

- **Given** an activated, unblocked rider inside the service area, standing near an available bike
- **When** the rider reserves the bike
- **Then** the bike is reserved for that rider and **Bike Reserved** is published with the bike, the rider and the reservation's expiry time

### Bike already reserved by someone else

- **Given** a bike reserved by rider A
- **When** rider B tries to reserve it
- **Then** the reservation is refused because the bike is not available, and A's reservation is untouched

### Withdrawn (broken) bike

- **Given** a bike that has been withdrawn from circulation
- **When** a rider tries to reserve it
- **Then** the reservation is refused. A broken bike is never offered to riders.

### Bike in an active rental session

- **Given** a bike that is currently rented
- **When** a rider tries to reserve it
- **Then** the reservation is refused

### Account not activated or blocked

- **Given** a rider whose account is incomplete, or blocked because of an invalid card or outstanding debt
- **When** the rider tries to reserve a bike
- **Then** the reservation is refused and the rider is told why

### Rider not near the bike or outside the service area

- **Given** a rider whose location is outside the service area or not near the selected bike
- **When** the rider tries to reserve it
- **Then** the reservation is refused

### Rider already holds a reservation

- **Given** a rider with an active reservation on bike X
- **When** the rider tries to reserve bike Y
- **Then** the reservation of Y is refused (assumption, see below)

### Only the holder can unlock

- **Given** a bike reserved by rider A
- **When** rider B tries to unlock it
- **Then** the unlock is refused
- **And when** rider A unlocks it successfully, the reservation is consumed and the rental session starts

## Business rules

- Only an activated account that is not blocked can reserve a bike.
- The rider's location is validated before the bike is reserved: inside the service area and near the bike.
- Only an **available** bike can be reserved. A reserved, rented or withdrawn bike can't be.
- A reservation is exclusive. At most one active reservation exists per bike.
- A reservation is short-lived and ends in exactly one way: it is **consumed** by a successful unlock, it **expires**, or it is **released** after a failed unlock.
- Reserving a bike is free and never billed.

## Assumptions (decisions made without the domain expert)

- **One active reservation per rider.** The vision doesn't say so, but a rider holding several bikes contradicts the purpose of the reservation.
- **A rider in an active rental session can't reserve another bike.** We serve individual riders only, with no group rides.
- **The reservation lives in Rentals**, as the reference context map puts "reserve & unlock for any authorised client" there. Rentals speaks of *available* / *withdrawn*, not *broken*.
- **The client-reported location is trusted**, as for returns in the reference map.

## Out of scope

- Unlocking mechanics and the start of the rental session, beyond consuming the reservation.
- Pricing of the ride.
- Technician reservations, see [reserve-broken-bike-for-maintenance](reserve-broken-bike-for-maintenance.md).

## Open questions

1. How long does a rider reservation last? The vision only says "short-lived" and "the seconds it takes to unlock".
2. Can a rider cancel a reservation on purpose, or does it only expire or get released?
3. Is there any limit or cooldown on repeated reservations, for example a rider who keeps reserving and letting reservations expire?
4. What exactly does "near a bike" mean? A distance, or being at the same station?
5. Can bikes left in a drop zone (outside any station) be reserved the same way as docked bikes? Today's `BikeStation` aggregate only knows bikes in a station.

## Related

- `velocity/velocity-vision.md`: *Renting a Bike*, *Key Business Rules to Know*, vocabulary entry *Reservation*.
- `velocity/velocity-context-map.md`: Rentals, *Reserve / unlock is a generic Rentals capability*.
- Code: `pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BikeStation` (currently `rent(bike).by(borrower).forDuration(duration)`, with no reservation step).
