---
name: technician-reserves-broken-bike
intent: technician-reserves-broken-bike
system: velocity
status: approved
date: 2026-11-08
---

# Spec: A technician wants to reserve a broken bike

Concrete rules, examples and contract for [the intent](../intents/technician-reserves-broken-bike.md).

## Vocabulary

| Term | Meaning |
|------|---------|
| Broken bike | A bike reported faulty; it waits for a technician and is never offered to riders until it is back in circulation (vision). |
| Technician's reservation | A technician's exclusive hold on one broken bike while they travel to it; it lasts 30 minutes (DE-11). |
| Reservation period | How long a reservation holds the bike: 30 minutes for a technician (DE-11), 2 minutes for a rider (DE-10). |

## Rules

### R1: A technician can reserve a broken bike for 30 minutes (DE-11)

- **E1.1** Given bike B-17 is broken at station Old Town and nobody holds it, When technician Tom reserves B-17 at 09:00, Then B-17 is reserved for Tom until 09:30 (Bike Reserved).
- **E1.2** Given B-22 is broken with fault source *not returned* and its position isn't known, When Tom reserves B-22 at 09:00 without giving his own position, Then B-22 is reserved for Tom until 09:30 (Bike Reserved) (DE-26).
- **E1.3** Given B-17 is reserved for Tom until 09:30 and B-22 is broken at Central, When Tom reserves B-22 at 09:05, Then B-22 is reserved for Tom until 09:35 (Bike Reserved), and Tom still holds B-17 (DE-92).

### R2: A technician reserves only broken bikes (DE-16)

- **E2.1** Given bike B-40 is available at Central and isn't broken, When Tom reserves B-40 at 09:00, Then the reservation is rejected and B-40 stays available to riders.

### R3: A reserved broken bike can't be reserved by another technician (vision, Reservation)

- **E3.1** Given B-17 is reserved for Tom until 09:30, When technician Tara reserves B-17 at 09:10, Then the reservation is rejected and B-17 stays reserved for Tom.

### R4: An unused technician's reservation expires after 30 minutes and the bike stays broken (vision, DE-11)

- **E4.1** Given B-17 is reserved for Tom from 09:00 until 09:30 and Tom hasn't unlocked it, When the reservations are checked at 09:30:01, Then Tom's reservation expired (Reservation Expired) and B-17 is still broken, not offered to riders, waiting for any technician.
- **E4.2** Given B-17 is reserved for Tom from 09:00 until 09:30, When the reservations are checked at 09:29, Then B-17 is still reserved for Tom.
- **E4.3** Given B-17 is reserved for Tom from 09:00 until 09:30, When the reservations are checked at exactly 09:30:00, Then B-17 is still reserved for Tom: a reservation holds up to and including its end time (DE-81).

## Contract

### Commands

| Command | Issued by | Input | On success | Rejected when |
|---------|-----------|-------|------------|---------------|
| Reserve Broken Bike | Technician | technician, bike | Bike Reserved | R2, R3 |
| Expire Reservations | Fleet Operations (as time passes) | now | Reservation Expired | – |

### Queries

| Query | Asked by | Input | Result |
|-------|----------|-------|--------|
| Broken Bikes | Technician | none | list of bike, fault source, reported at, station or last known position (if known), reserved by and until (if reserved) |

### Events

| Event | When | Data |
|-------|------|------|
| Bike Reserved | a technician's reservation is accepted | holder (technician), bike, reserved at, expires at |
| Reservation Expired | a technician's reservation ran out unused | holder (technician), bike, expired at |

## Data

| Shape | Field | Type | Constraint |
|-------|-------|------|------------|
| Technician's reservation | technician | Identifier (Technician) | required |
| Technician's reservation | bike | Identifier (Bike) | required; a broken bike (R2) |
| Technician's reservation | expires at | Timestamp | reserved at + 30 minutes (DE-11) |

## Out of scope

- Unlocking the bike and starting maintenance (`technician-starts-maintenance`).
- Riders' reservations and their expiry (`rider-reserves-bike`, `fleet-operations-releases-expired-reservations`).
- Finding a bike that wasn't returned from its last signalled position (not modelled, DE-26).

## Open questions

- ~~**Q1** How long does a technician's reservation last?~~ 30 minutes, because the technician has to get to the bike (role-played domain expert, DE-11, 2026-11-08)
- ~~**Q2** Is a technician's reservation still valid at exactly 30:00 minutes?~~ Yes, up to and including its end time; expired from 09:30:01 (role-played domain expert, DE-81, 2026-11-08)
- ~~**Q3** May a technician hold several reservations at the same time?~~ Yes, they drive a van (role-played domain expert, DE-92, 2026-11-08)
- ~~**Q4** Must a technician be near the bike to reserve it?~~ No, the 50 m rule is for riders only; a technician reserves without giving a position (role-played domain expert, DE-26, 2026-11-08)
- ~~**Q5** Can a technician reserve a bike that isn't broken?~~ No, technicians reserve only broken bikes (role-played domain expert, DE-16, 2026-11-08)

## Notes for the plan

- Accepted under the trainer's standing instruction of 2026-11-08 (autonomous run); the intent had no technical notes.
