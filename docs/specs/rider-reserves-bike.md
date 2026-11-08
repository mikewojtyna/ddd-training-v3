---
name: rider-reserves-bike
intent: rider-reserves-bike
system: velocity
status: approved
date: 2026-11-08
---

# Spec: A rider wants to reserve a bike they are standing near

Concrete rules, examples and contract for [the intent](../intents/rider-reserves-bike.md).

## Vocabulary

| Term | Meaning |
|------|---------|
| Reservation | A brief, exclusive hold on one specific bike for one rider, between choosing it and unlocking it. Free of charge (DE-17). |
| Reservation period | How long a rider's reservation holds the bike: 2 minutes (DE-10), up to and including its end time (DE-81). |
| Near | Within 50 metres of the bike (DE-12). Bikes are only ever inside the area VeloCity operates in, so being near a bike also means being inside it. |
| Eligible to rent | The rider's account is activated (every onboarding step done) and not blocked (vision "User Onboarding", "Account Blocks & Debt Recovery"). |
| Available | A bike that may be reserved by a rider. A broken bike (withdrawn) is not available (vision, DE-16). |

## Rules

### R1: An eligible rider standing near an available bike can reserve it for 2 minutes

- **E1.1** Given bike B-17 is available at station Central and rider Anna is eligible to rent, When Anna, standing 20 m from B-17, reserves it at 10:00, Then B-17 is reserved for Anna until 10:02 (Bike Reserved).

### R2: A rider can reserve only a bike within 50 metres (vision, DE-12)

- **E2.1** Given B-17 is available at Central and Anna is eligible to rent, When Anna, standing 400 m from B-17, reserves it, Then the reservation is rejected and B-17 stays available.
- **E2.2** Given B-17 is available at Central and Anna is eligible to rent, When Anna, standing exactly 50 m from B-17, reserves it at 10:00, Then B-17 is reserved for Anna until 10:02 (Bike Reserved) (Q1).

### R3: Only a rider whose account is activated and not blocked can reserve (vision, DE-14)

- **E3.1** Given rider Ben has confirmed his email and phone and given his personal data but hasn't added a payment method yet, When Ben, standing 10 m from available B-17, reserves it, Then the reservation is rejected.
- **E3.2** Given rider Carla's account is activated but blocked because of outstanding debt, When Carla, standing 10 m from available B-17, reserves it, Then the reservation is rejected.

### R4: One rider, one bike (DE-13)

- **E4.1** Given Anna holds a reservation for B-17 until 10:02, When Anna, standing 15 m from available B-22, reserves B-22 at 10:01, Then the reservation is rejected and B-22 stays available.
- **E4.2** Given Anna's ride on B-17 started at 09:30 and is still running, When Anna, standing 15 m from available B-22, reserves B-22 at 10:00, Then the reservation is rejected.

### R5: A bike that is reserved, ridden or broken can't be reserved by a rider (vision, DE-16)

- **E5.1** Given B-17 is reserved for Anna until 10:02, When eligible rider Ben, standing 5 m from B-17, reserves it at 10:01, Then the reservation is rejected and B-17 stays reserved for Anna.
- **E5.2** Given B-17 is broken and withdrawn from riders at Central, When eligible Anna, standing 20 m from B-17, reserves it, Then the reservation is rejected.

## Contract

### Commands

| Command | Issued by | Input | On success | Rejected when |
|---------|-----------|-------|------------|---------------|
| Reserve Bike | Rider | rider, bike, rider's position | Bike Reserved | R2, R3, R4, R5 |

### Queries

| Query | Asked by | Input | Result |
|-------|----------|-------|--------|
| Rider's Active Rental | Rider | rider | the reserved bike and when the reservation ends, or the bike of the running ride, or none |

### Events

| Event | When | Data |
|-------|------|------|
| Bike Reserved | a rider's reservation is accepted | rider, bike, reserved at, reserved until |

## Data

| Shape | Field | Type | Constraint |
|-------|-------|------|------------|
| Reservation | rider | Identifier (Rider) | required; eligible to rent (R3); no other reservation or running ride (R4) |
| Reservation | bike | Identifier (Bike) | required; available and not reserved or ridden (R5) |
| Reservation | rider's position | Position (latitude, longitude) | required; within 50 m of the bike (R2) |
| Reservation | reserved until | Timestamp | reserved at + 2 minutes (DE-10); the reservation holds up to and including this time (DE-81) |

## Out of scope

- Unlocking the reserved bike (`rider-unlocks-reserved-bike`).
- Releasing a reservation that wasn't used (`fleet-operations-releases-expired-reservations`).
- A technician reserving a broken bike (`technician-reserves-broken-bike`).
- A rider cancelling a reservation on purpose (not requested).
- Any fee: reservations are free (DE-17).

## Open questions

- **Q1** Is a rider standing exactly 50 m from the bike "near" it? (R2, E2.2) E2.2 uses the literal reading of "within 50 metres": yes.
- ~~**Q2** How long does a rider's reservation last?~~ 2 minutes (role-played domain expert, DE-10, 2026-11-08)
- ~~**Q3** What does "standing near a bike" mean?~~ Within 50 metres of the bike; bikes are only ever inside the area VeloCity operates in, so this is also that check (role-played domain expert, DE-12, 2026-11-08)
- ~~**Q4** Can a rider hold several bikes?~~ No: one rider, one bike, counting both reservations and running rides (role-played domain expert, DE-13, 2026-11-08)
- ~~**Q5** Is the account checked at reservation or at unlock?~~ At both: when reserving (DE-14) and again at unlock (role-played domain expert, DE-80, 2026-11-08; see `rider-unlocks-reserved-bike`)
- ~~**Q6** Does a reservation cost anything?~~ No (role-played domain expert, DE-17, 2026-11-08)
- ~~**Q7** Does a reservation still hold at the exact second its period ends?~~ Yes, up to and including its end time: reserved at 10:00:00, it holds until 10:02:00 and has expired from 10:02:01 (role-played domain expert, DE-81, 2026-11-08)

## Notes for the plan

- None.
