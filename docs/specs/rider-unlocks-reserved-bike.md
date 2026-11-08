---
name: rider-unlocks-reserved-bike
intent: rider-unlocks-reserved-bike
system: velocity
status: approved
date: 2026-11-08
---

# Spec: A rider wants to unlock the bike they reserved and start riding

Concrete rules, examples and contract for [the intent](../intents/rider-unlocks-reserved-bike.md).

## Vocabulary

| Term | Meaning |
|------|---------|
| Reservation | A rider's 2-minute exclusive hold on one bike (DE-10), see `rider-reserves-bike`. |
| Unlock | Opening the bike's lock for the rider who holds its reservation. |
| Rental session | A rider's ride, from a successful unlock until the ride ends (vision "Domain Vocabulary"). |
| Failed unlock | The lock doesn't open, for any reason (vision "Renting a Bike"). |

## Rules

### R1: The rider holding the reservation unlocks the bike, and the ride starts

- **E1.1** Given B-17 at station Central is reserved for Anna from 10:00 until 10:02, When Anna unlocks B-17 at 10:01 and the lock opens, Then a rental session starts for Anna on B-17 at 10:01 (Rental Session Started) and the reservation is used up.
- **E1.2** Given B-17 is reserved for Anna from 10:00 until 10:02, When Anna unlocks B-17 at exactly 10:02:00 and the lock opens, Then a rental session starts for Anna on B-17 at 10:02 (Rental Session Started) (DE-81).

### R2: A failed unlock releases the reservation and the bike is available again (vision, DE-15)

- **E2.1** Given B-17 at Central is reserved for Anna until 10:02, When Anna unlocks B-17 at 10:01 and the lock doesn't open, Then no rental session starts, Anna holds no reservation and B-17 is available to anyone again (Unlock Failed).
- **E2.2** Given Anna's unlock of B-17 failed at 10:01, When Anna, standing 10 m from available B-22, reserves B-22 at 10:01:30, Then B-22 is reserved for Anna (Bike Reserved, see `rider-reserves-bike`).

### R3: Only the holder of a reservation that hasn't run out can unlock the bike (vision)

- **E3.1** Given B-17 is reserved for Anna until 10:02, When rider Ben unlocks B-17 at 10:01, Then the unlock is rejected, the lock stays closed and B-17 stays reserved for Anna.
- **E3.2** Given B-22 is available and Anna holds no reservation, When Anna unlocks B-22, Then the unlock is rejected and the lock stays closed.
- **E3.3** Given B-17 was reserved for Anna until 10:02 and the reservation hasn't been released yet, When Anna unlocks B-17 at 10:02:30, Then the unlock is rejected and the lock stays closed (the reservation period is over, see `fleet-operations-releases-expired-reservations`).

### R4: An ineligible rider's unlock is rejected, and nothing changes (vision, DE-80)

- **E4.1** Given eligible Anna reserved B-17 at 10:00, until 10:02, and her account was blocked at 10:01 because her card became invalid, When Anna unlocks B-17 at 10:01:30, Then the unlock is rejected, the lock stays closed, no rental session starts and B-17 stays reserved for Anna until 10:02, when the reservation runs out on its own (see `fleet-operations-releases-expired-reservations`).

## Contract

### Commands

| Command | Issued by | Input | On success | Rejected when |
|---------|-----------|-------|------------|---------------|
| Unlock Bike | Rider | rider, bike | Rental Session Started or Unlock Failed | R3, R4 |

### Queries

| Query | Asked by | Input | Result |
|-------|----------|-------|--------|
| None. | | | |

### Events

| Event | When | Data |
|-------|------|------|
| Rental Session Started | the lock opened for the rider holding the reservation | rental session, rider, bike, station or position where the ride starts, started at |
| Unlock Failed | the lock didn't open for the reservation holder | rider, bike, failed at |

## Data

| Shape | Field | Type | Constraint |
|-------|-------|------|------------|
| Rental session | rental session | Identifier (Rental session) | one per successful unlock |
| Rental session | rider | Identifier (Rider) | the reservation holder (R3), eligible to rent at the unlock (R4) |
| Rental session | bike | Identifier (Bike) | in at most one running rental session (vision) |
| Rental session | started at | Timestamp | the moment the lock opened; no later than the end of the reservation period (R1, R3) |

## Out of scope

- Reserving (`rider-reserves-bike`).
- A reservation that runs out before unlocking (`fleet-operations-releases-expired-reservations`).
- A technician unlocking a broken bike (`technician-starts-maintenance`).
- Ending the ride (`rider-returns-bike-at-station`, `rider-ends-ride-in-drop-zone`, `fleet-operations-ends-overdue-rental`).
- Withdrawing a bike after repeated failed unlocks (not requested; DE-15 says a failed unlock doesn't make a bike broken).

## Open questions

- ~~**Q1** Can the rider still unlock at exactly the second the reservation period ends (10:02:00 for a reservation made at 10:00)?~~ Yes: a reservation holds up to and including its end time; from 10:02:01 it has expired (role-played domain expert, DE-81, 2026-11-08)
- ~~**Q2** The vision says "No unlock without a fully activated account" and "A blocked account cannot start new rentals", while DE-14 checked the account only when reserving. Is the account checked again at unlock?~~ Yes: an ineligible rider's unlock is rejected, nothing changes and the reservation runs out on its own (role-played domain expert, DE-80, 2026-11-08, revising DE-14)
- ~~**Q3** Does a failed unlock make the bike broken?~~ No: the reservation is released and the bike is available again; a really faulty bike signals the fault itself (role-played domain expert, DE-15, 2026-11-08)

## Notes for the plan

- None.
