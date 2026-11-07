---
name: rider-reserves-bike-at-station
intent: rider-reserves-bike-at-station
system: velocity
status: approved
date: 2026-11-07
---

# Spec: A rider wants to reserve a bike at a bike station

Concrete rules, examples and contract for [the intent](../intents/rider-reserves-bike-at-station.md).

## Vocabulary

| Term | Meaning |
|------|---------|
| Reservation | A rider's hold on one specific bike at a station. While it lasts, no other rider can reserve or rent that bike. |
| Reservation period | How long a reservation holds the bike: 15 minutes from the moment it's made, up to and including the end time, so a reservation made at 10:00 still holds at 10:15 (product owner, 2026-11-07). |
| Available bike | A bike at a station that isn't rented, reserved, broken or in maintenance. |
| Active reservation | A reservation that has been made and hasn't ended yet. It ends when the rider rents the reserved bike (R8) or once the reservation period has passed, i.e. after 10:15 for one made at 10:00. |
| Activated account | A rider account with every onboarding step complete (vision, "Onboarding"). |

## Rules

### R1: A rider can reserve an available bike at a station without being at the station

- **E1.1** Given bike B-17 is available at station Central and rider Anna, who has an activated account, is at home 2 km away, When Anna reserves B-17 at 10:00, Then B-17 is reserved for Anna until 10:15 (Bike Reserved).
- **E1.2** Given B-17 at Central has just been rented by rider Ben, When Anna reserves B-17, Then the reservation is rejected.

### R2: A rider holds at most one active reservation

- **E2.1** Given Anna holds a reservation for B-17 at Central until 10:15, When Anna reserves B-22 at station Riverside at 10:05, Then the reservation is rejected and B-22 stays available.

### R3: While a bike is reserved, nobody else can reserve or rent it

- **E3.1** Given B-17 is reserved for Anna until 10:15, When Ben reserves B-17 at 10:05, Then the reservation is rejected.
- **E3.2** Given B-17 is reserved for Anna until 10:15, When Ben rents B-17 at 10:15, Then the rental is rejected, because the reservation still holds at its end time.

### R4: A reserved bike isn't offered to other riders

- **E4.1** Given B-17 and B-22 are at Central, B-17 is reserved for Anna and B-22 is available, When Ben asks which bikes are available at Central, Then he sees only B-22.

### R5: A broken bike can't be reserved by a rider (vision)

- **E5.1** Given B-31 at Central has been reported broken, When Anna reserves B-31, Then the reservation is rejected.

### R6: Only a rider with an activated account can reserve a bike

- **E6.1** Given rider Carl hasn't finished onboarding (no verified payment method), When Carl reserves B-17, Then the reservation is rejected and B-17 stays available.

### R7: A rider who is riding a bike can't reserve another one

- **E7.1** Given Anna rented B-05 at 09:40 and is still riding it, When Anna reserves B-17 at Central at 10:00, Then the reservation is rejected and B-17 stays available.

### R8: A rider who holds a reservation can rent only the reserved bike, and renting it ends the reservation

- **E8.1** Given Anna holds a reservation for B-17 at Central until 10:15 and B-22 is available at Central, When Anna rents B-22 at 10:05, Then the rental is rejected and B-17 stays reserved for her.
- **E8.2** Given Anna holds a reservation for B-17 until 10:15, When Anna rents B-17 at 10:10, Then the rental starts for Anna and her reservation ends (Bike Rented).

## Contract

### Commands

| Command | Issued by | Input | On success | Rejected when |
|---------|-----------|-------|------------|---------------|
| Reserve Bike | Rider | rider, bike, station | Bike Reserved | R1, R2, R3, R5, R6, R7 |
| Rent Bike (from `rider-rents-bike-from-station`, gains a rule here) | Rider | rider, bike, station | Bike Rented | R3, R8 |

### Queries

| Query | Asked by | Input | Result |
|-------|----------|-------|--------|
| Available Bikes at Station | Rider | station | list of available bikes |

### Events

| Event | When | Data |
|-------|------|------|
| Bike Reserved | a reservation is accepted | rider, bike, station, reserved at, reserved until |
| Bike Rented | a rental starts (existing event from `rider-rents-bike-from-station`; this spec adds rules R3 and R8 to Rent Bike) | rider, bike, station, started at, from reservation (Yes/No, for the intent's success signal) |

## Data

| Shape | Field | Type | Constraint |
|-------|-------|------|------------|
| Reservation | rider | Identifier (Rider) | required; at most one active reservation per rider (R2) |
| Reservation | bike | Identifier (Bike) | required; at most one active reservation per bike (R3) |
| Reservation | station | Identifier (Station) | required; the station where the bike is |
| Reservation | reserved at | Timestamp | required |
| Reservation | reserved until | Timestamp | reserved at + 15 minutes |

## Out of scope

- An unused reservation running out once the reservation period has passed, and the bike becoming available again (`fleet-operations-releases-unused-reservation`).
- Cancelling a reservation (`rider-cancels-reservation`).
- A technician reserving a broken bike (`technician-reserves-broken-bike`).
- A warning before the reservation runs out (`rider-is-warned-before-reservation-expires`, candidate).
- Paying for a reservation (`rider-pays-for-reservation`, candidate, not in this release).
- Releasing the reservation when unlocking fails (vision; no intent yet, candidate).
- A rider who is riding can't rent a second bike. This follows from the product owner's "you can rent only one bike at a time", but it's a rule on renting itself (`rider-rents-bike-from-station`).

## Open questions

- ~~**Q4** Is B-17 still reserved at exactly 10:15 for a reservation made at 10:00?~~ Yes (product owner, 2026-11-07). See E3.2.
- ~~**Q1** Can a rider who is already riding a bike reserve another one?~~ No (product owner, 2026-11-07). See R7.
- ~~**Q2** What happens to Anna's reservation when she rents B-17 herself, and can she rent B-22 while she holds it?~~ "You can rent only one bike at a time" (product owner, 2026-11-07). Confirmed by Q5.
- ~~**Q5** Does that mean Anna can't rent B-22 instead, and that her reservation ends when she rents B-17?~~ Yes to both (product owner, 2026-11-07). See R8.
- ~~**Q3** Must a rider be inside VeloCity's service area to reserve?~~ No (product owner, 2026-11-07). No location rule applies to reserving.
- ~~**Q0** Is the reservation remote or only near the bike, and how long does it last?~~ Remote, 15 minutes (product owner, 2026-11-07). This departs from the vision ("the seconds it takes to unlock", "standing near a bike"). E1.1 shows it.

## Notes for the plan

- The ticket's draft acceptance criteria suggest a reservations endpoint (create returns an identifier, 409 when the rider already has an active reservation), a 15-minute expiry kept in Redis, and a `RESERVED` status on the bike table, reusing the unlock flow.
- The mobile team needs the contract by sprint 24.
- Analytics event `reservation_created` to Mixpanel, to measure the intent's success signal.
- The success signal "30% of rentals start from a reservation" needs to know whether a rental started from a reservation (E8.2).
