# Reserve a broken bike for maintenance

- **Status:** Draft
- **System:** VeloCity
- **Bounded contexts:** Rentals (owns the reservation), Fleet Maintenance (the client that asks for it)
- **Actor:** Technician
- **Part of:** Bike reservations. The related intents are [reserve-bike-for-ride](reserve-bike-for-ride.md), [expire-bike-reservation](expire-bike-reservation.md) and [release-reservation-on-failed-unlock](release-reservation-on-failed-unlock.md).

## Why

When a bike is reported broken, a technician goes to pick it up. While they are on the way, nobody else should grab that bike. Technicians go through the same reserve-and-unlock steps as riders, but for a different purpose: their unlock starts **maintenance**, never a rental session.

## Desired outcome

A technician can reserve a broken (withdrawn) bike. The reservation stops anyone else, including other technicians, from taking it until the technician unlocks it, the reservation expires, or the reservation is released.

## Scenarios

### Technician reserves a broken bike

- **Given** a bike that has been reported broken and withdrawn from circulation
- **When** a technician reserves it
- **Then** the bike is reserved for that technician and **Bike Reserved** is published with the bike, the technician and the reservation's expiry time

### Another technician already holds the bike

- **Given** a broken bike reserved by technician A
- **When** technician B tries to reserve it
- **Then** the reservation is refused

### Riders still can't touch it

- **Given** a broken bike reserved by a technician
- **When** a rider tries to reserve it
- **Then** the reservation is refused. A broken bike is never offered to riders.

### Technician unlocks the reserved bike

- **Given** a broken bike reserved by technician A
- **When** technician A unlocks it successfully
- **Then** the reservation is consumed, **no rental session starts**, and Fleet Maintenance reacts with **Maintenance Started**

### Unlock by someone other than the holder

- **Given** a broken bike reserved by technician A
- **When** anyone else tries to unlock it
- **Then** the unlock is refused

## Business rules

- Only riders and technicians reserve and unlock bikes.
- A technician reserves a bike in order to repair it. A technician's unlock starts maintenance, never a rental session, and is never billed.
- At most one active reservation exists per bike, whoever holds it.
- The bike stays out of circulation for the whole reservation and after it, until it is fixed.
- A technician's reservation ends the same three ways as a rider's: consumed by unlock, expired, or released after a failed unlock.

## Assumptions (decisions made without the domain expert)

- **Technicians reserve only broken (withdrawn) bikes.** The vision describes technician reservations only in the maintenance flow.
- **No account-activation or location check for technicians.** Technicians are VeloCity employees, not riders going through onboarding, and they reserve the bike *before* travelling to it, so "standing near the bike" can't apply.
- **Rentals stays generic.** Following the reference map, Rentals reserves a *withdrawn* bike for an authorised client. It doesn't know the word *broken* or *maintenance*. Fleet Maintenance calls Rentals to reserve and listens to the unlock to start maintenance.
- **A technician may hold several reservations at once**, for example to collect a few bikes in one trip. This is the opposite of the rider rule, and it needs confirming.

## Out of scope

- How a bike becomes broken (fault reports, failed locks, fleet-issue incidents).
- Maintenance itself, fixing the bike and putting it back into circulation.
- Assigning broken bikes to technicians or planning their routes.

## Open questions

1. **Reservation length conflict.** For riders the vision says a reservation covers "the seconds it takes to unlock". For technicians it covers the time "while the technician is on the way", which is probably minutes or longer. Is it one reservation concept with a different duration per holder, or two different holds?
2. Can a technician reserve an **available** (not broken) bike, for example for an inspection or rebalancing?
3. How many broken bikes can one technician reserve at the same time?
4. Can a technician release a reservation on purpose, for example when they are reassigned?
5. Does Fleet Maintenance decide which technician gets which bike, or does any technician just reserve one?

## Related

- `velocity/velocity-vision.md`: *Technicians*, *Broken Bikes & Maintenance*, *Key Business Rules to Know*.
- `velocity/velocity-context-map.md`: `Rentals -> Fleet Maintenance` (OHS/PL, "reserve / unlock"), *Reserve / unlock is a generic Rentals capability*.
- `src/main/cml/VeloCity-Context-Map.cml`: `Rentals [U,OHS,PL]->[D,CF] FleetMaintenance : BikeAvailability`.
