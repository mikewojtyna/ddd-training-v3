---
name: rider-reserves-bike-at-station
intent: rider-reserves-bike-at-station
system: velocity
status: draft
date: 2026-11-06
---

# Spec: A rider wants to reserve a bike at a bike station

Concrete rules, examples and contract for [the intent](../intents/rider-reserves-bike-at-station.md).

## Vocabulary

| Term | Meaning |
|------|---------|
| Reservation | A brief, exclusive hold on one specific bike at a station for one rider, between picking the bike and unlocking it (vision glossary). |
| Active reservation | A reservation that has neither expired nor been used to rent the bike. |
| Reservation period | How long a reservation holds the bike before it expires. Length: see Q1. |
| Available bike | A bike at a station that is not reserved, not rented and not broken; anyone may reserve or rent it. |
| Activated account | A rider account whose onboarding is complete (vision "Onboarding"). |

## Rules

### R1: A rider can reserve an available bike at a station

- **E1.1** Given bike B-17 is available at station Central and rider Anna has an activated account and no reservation, When Anna reserves B-17 at Central at 10:00, Then B-17 is reserved for Anna until 10:00 + ? (reservation period, Q1) (Bike Reserved).

### R2: A rider holds at most one active reservation

- **E2.1** Given Anna holds an active reservation for B-17 at Central, When Anna reserves B-22 at Central, Then the reservation is rejected, Anna still holds B-17 and B-22 stays available.
- **E2.2** Given Anna's reservation for B-17 has expired (Reservation Expired), When Anna reserves B-22 at Central, Then B-22 is reserved for Anna (Bike Reserved).

### R3: A reserved bike is held for its rider only; nobody else can reserve or rent it

- **E3.1** Given B-17 is reserved for Anna, When rider Ben reserves B-17, Then the reservation is rejected and B-17 stays reserved for Anna.
- **E3.2** Given B-17 is reserved for Anna, When Ben rents B-17, Then the rental is rejected and B-17 stays reserved for Anna.
- **E3.3** Given B-17 is reserved for Anna, When Anna rents B-17 before her reservation expires, Then the rental starts (Bike Rented) and Anna no longer holds an active reservation.

### R4: An unused reservation expires at the end of the reservation period and the bike is available again

- **E4.1** Given Anna reserved B-17 at Central at 10:00 and hasn't rented it, When the reservation period ends at 10:00 + ? (Q1), Then Anna's reservation expires and B-17 is available at Central (Reservation Expired).
- **E4.2** Given Anna's reservation for B-17 expired (Reservation Expired), When Ben reserves B-17, Then B-17 is reserved for Ben (Bike Reserved).
- **E4.3** Given Anna reserved B-17 at 10:00, When Anna rents B-17 exactly at the moment the reservation period ends, Then ? (Q4).

### R5: Only an available bike can be reserved

- **E5.1** Given B-31 at Central is rented by Ben, When Anna reserves B-31, Then the reservation is rejected.
- **E5.2** Given B-40 at Central has been reported broken, When Anna reserves B-40, Then the reservation is rejected (a broken bike is never offered to riders).
- **E5.3** Given B-17 is docked at station Central, When Anna reserves B-17 at station Riverside, Then the reservation is rejected.

### R6: Only a rider with an activated account can reserve a bike

- **E6.1** Given rider Carl hasn't signed the rental agreement yet (onboarding incomplete), When Carl reserves B-17 at Central, Then the reservation is rejected and B-17 stays available.

## Contract

### Commands

| Command | Issued by | Input | On success | Rejected when |
|---------|-----------|-------|------------|---------------|
| Reserve Bike | Rider | rider, bike, station | Bike Reserved | R2, R3, R5, R6 |

### Queries

| Query | Asked by | Input | Result |
|-------|----------|-------|--------|
| Rider's Active Reservation | Rider | rider | bike, station, expires at (or none) |

### Events

| Event | When | Data |
|-------|------|------|
| Bike Reserved | a reservation is accepted | rider, bike, station, reserved at, expires at |
| Reservation Expired | the reservation period of an unused reservation ended | rider, bike, station, expired at |

## Data

| Shape | Field | Type | Constraint |
|-------|-------|------|------------|
| Reservation | rider | Identifier (Rider) | required; at most one active reservation per rider (R2) |
| Reservation | bike | Identifier (Bike) | required; at most one active reservation per bike (R3) |
| Reservation | station | Identifier (Station) | required; the station where the bike is docked (R5) |
| Reservation | reserved at | Timestamp | required |
| Reservation | expires at | Timestamp | reserved at + reservation period (? Q1) |

## Out of scope

- Cancelling a reservation (`rider-cancels-reservation`, candidate).
- Unlocking the bike and starting the rental (`rider-rents-bike-from-station`); E3.2 and E3.3 only state how a reservation constrains renting.
- What happens to the reservation when unlocking fails ("the reservation is then released", vision): belongs with unlocking.
- Technicians reserving broken bikes for maintenance (vision "Broken Bikes & Maintenance"): the intent's actor is the Rider only.
- Reserving bikes outside stations (drop zones): the intent says "at a bike station".
- Notifying the rider before or when the reservation expires.

## Open questions

- **Q1** How long is the reservation period? (R1, R4, E1.1, E4.1, E4.3, Data "expires at") The vision only says "short-lived" and "the seconds it takes to unlock"; the intent says the rider is "walking to the station", which suggests minutes. Please give a concrete number.
- **Q2** Contradiction between the intent and the vision: the vision says the rider's location is validated first ("standing near a bike") and the reservation only covers the seconds before unlocking, while the intent wants the bike held "while I'm walking to the station". Can a rider reserve a bike remotely, from anywhere in the city, or only when standing near the bike? (R1, E1.1; decides whether a location constraint becomes a rule)
- **Q3** Does a rider whose account is blocked (unpaid debt, penalty) get rejected when reserving, or only when renting? The vision says "a blocked account cannot start new rentals" and says nothing about reservations. (R6)
- **Q4** If the rider tries to rent at exactly the moment the reservation period ends, is the reservation still valid? (E4.3)
- **Q5** Can a rider reserve the same bike again straight after their own reservation expired, or is there a waiting time? (R2, R4, E2.2)
- **Q6** Is there any consequence (fee, limit on reservations per day) for riders who repeatedly let reservations expire? None is stated; confirm there is none for this intent. (R4)
- **Q7** Does a rider who already has an active rental session get rejected when reserving another bike? Nothing in the vision or the intent says so. (R2)

## Notes for the plan

- None.
