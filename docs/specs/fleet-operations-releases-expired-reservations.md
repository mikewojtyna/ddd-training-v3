---
name: fleet-operations-releases-expired-reservations
intent: fleet-operations-releases-expired-reservations
system: velocity
status: approved
date: 2026-11-08
---

# Spec: Fleet operations wants reservations that weren't used released

Concrete rules, examples and contract for [the intent](../intents/fleet-operations-releases-expired-reservations.md).

## Vocabulary

| Term | Meaning |
|------|---------|
| Reservation | A brief, exclusive hold on one bike for one rider or technician, between choosing it and unlocking it. |
| Reservation period | 2 minutes for a rider (DE-10), 30 minutes for a technician (DE-11), counted from the moment of reserving; the reservation holds up to and including its end time (DE-81). |
| Expired reservation | A reservation whose period has passed without an unlock. It costs nothing (DE-17). |
| Broken bike | A bike withdrawn from riders; only a technician can reserve it (vision, DE-16). |

## Rules

### R1: A rider's reservation not used within 2 minutes expires and the bike is available to anyone (vision, DE-10)

- **E1.1** Given B-17 at station Central was reserved for Anna at 10:00, until 10:02, and Anna hasn't unlocked it, When the reservations are checked at 10:02:01, Then Anna's reservation expired and B-17 is available to anyone (Reservation Expired).
- **E1.2** Given B-17 was reserved for Anna at 10:00, until 10:02, When the reservations are checked at exactly 10:02:00, Then B-17 is still reserved for Anna (DE-81).
- **E1.3** Given Anna's reservation of B-17 expired at 10:02:01, When rider Ben, standing 10 m from B-17, reserves it at 10:04, Then B-17 is reserved for Ben until 10:06 (Bike Reserved, see `rider-reserves-bike`).

### R2: A technician's reservation not used within 30 minutes expires, and the bike stays broken (vision "Broken Bikes & Maintenance", DE-11)

- **E2.1** Given broken B-31 was reserved for technician Tom at 09:00, until 09:30, and Tom hasn't unlocked it, When the reservations are checked at 09:30:01, Then Tom's reservation expired and B-31 stays withdrawn from riders, waiting for a technician to reserve it again (Reservation Expired).
- **E2.2** Given broken B-31 was reserved for Tom at 09:00, When the reservations are checked at exactly 09:30:00, Then B-31 is still reserved for Tom (DE-81).

### R3: A reservation that ended by an unlock doesn't expire

- **E3.1** Given B-17 was reserved for Anna at 10:00 and Anna unlocked it at 10:01, starting her ride, When the reservations are checked at 10:03, Then nothing expires and Anna's ride on B-17 keeps running.
- **E3.2** Given Anna's unlock of B-17 failed at 10:01, releasing her reservation, When the reservations are checked at 10:03, Then nothing expires (her reservation already ended with Unlock Failed, see `rider-unlocks-reserved-bike`).

## Contract

### Commands

| Command | Issued by | Input | On success | Rejected when |
|---------|-----------|-------|------------|---------------|
| Release Expired Reservations | Fleet Operations | the current time | Reservation Expired | |

### Queries

| Query | Asked by | Input | Result |
|-------|----------|-------|--------|
| None. | | | |

### Events

| Event | When | Data |
|-------|------|------|
| Reservation Expired | a reservation's period passed without an unlock; one per expired reservation | bike, holder (rider or technician), reserved at, expired at |

## Data

| Shape | Field | Type | Constraint |
|-------|-------|------|------------|
| Expired reservation | holder | Identifier (Rider or Technician) | required |
| Expired reservation | bike | Identifier (Bike) | required |
| Expired reservation | expired at | Timestamp | later than reserved at + reservation period, from the next second on (R1, R2, DE-81) |

## Out of scope

- Reserving (`rider-reserves-bike`, `technician-reserves-broken-bike`).
- Unlocking (`rider-unlocks-reserved-bike`, `technician-starts-maintenance`).
- A rider cancelling a reservation on purpose (not requested).
- A reservation cancelled because the bike signalled a fault (`fleet-operations-withdraws-bike-signalling-fault`).
- How often the reservations are checked: the examples only say what a check at a given time finds.

## Open questions

- ~~**Q1** Is a reservation still held at exactly the second its period ends (10:02:00 for a rider reserving at 10:00)?~~ Yes, up to and including its end time, for riders and technicians; it has expired from 10:02:01 (role-played domain expert, DE-81, 2026-11-08)
- **Q2** Is the rider told that their reservation expired? (R1) Doesn't change whether it expires.
- ~~**Q3** How long does a rider's reservation last?~~ 2 minutes (role-played domain expert, DE-10, 2026-11-08)
- ~~**Q4** How long does a technician's reservation last?~~ 30 minutes, because the technician has to get to the bike (role-played domain expert, DE-11, 2026-11-08)
- ~~**Q5** Does an expired reservation cost anything?~~ No (role-played domain expert, DE-17, 2026-11-08)

## Notes for the plan

- None.
