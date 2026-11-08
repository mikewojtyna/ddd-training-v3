---
name: fleet-operations-withdraws-bike-signalling-fault
intent: fleet-operations-withdraws-bike-signalling-fault
system: velocity
status: approved
date: 2026-11-08
---

# Spec: Fleet operations wants a bike that signals a fault taken away from riders

Concrete rules, examples and contract for [the intent](../intents/fleet-operations-withdraws-bike-signalling-fault.md).

## Vocabulary

| Term | Meaning |
|------|---------|
| Fault signal | The bike itself reports a fault it detected, with a fault code such as "BRAKE_SENSOR" (vision "Broken Bikes & Maintenance"). |
| Withdrawn | No longer offered to riders; the vision calls such a bike "broken". Only a technician can reserve it (DE-16). |
| Withdrawal reason | Why a bike was withdrawn. For this intent: *fault signalled*. |

## Rules

### R1: A free bike that signals a fault is withdrawn at once (vision, DE-25)

- **E1.1** Given B-17 is available at station Central and nobody holds it, When B-17 signals fault "BRAKE_SENSOR" at 09:00, Then B-17 is withdrawn with reason *fault signalled* (Bike Withdrawn).
- **E1.2** Given B-17 signalled fault "BRAKE_SENSOR" at 09:00 and was withdrawn, When rider Anna, standing 10 m from B-17, reserves it at 09:05, Then the reservation is rejected (see `rider-reserves-bike`).

### R2: A rider's reservation on a bike that signals a fault is cancelled, and the bike is withdrawn (DE-25)

- **E2.1** Given B-17 at Central is reserved for Anna from 10:00 until 10:02, When B-17 signals fault "BRAKE_SENSOR" at 10:01, Then Anna's reservation is cancelled, Anna may reserve another bike, and B-17 is withdrawn with reason *fault signalled* (Bike Withdrawn) (Q1).

### R3: A fault signalled during a ride never interrupts it; the bike is withdrawn when the ride ends (DE-25, DE-86)

- **E3.1** Given Anna's rental session on B-17 started at 10:01, When B-17 signals fault "BRAKE_SENSOR" at 10:15, Then Anna's session keeps running and B-17 isn't withdrawn yet.
- **E3.2** Given Anna's rental session on B-17 started at 10:01 and B-17 signalled fault "BRAKE_SENSOR" at 10:15, When Anna returns B-17 at station Old Town at 10:40 and the lock closes, Then the session ended at 10:40 at Old Town with end reason *station return* (Rental Session Ended, see `rider-returns-bike-at-station`) and B-17 is withdrawn with reason *fault signalled* (Bike Withdrawn).

### R4: A bike already withdrawn stays withdrawn when it signals a fault (vision "A broken bike is never offered to riders")

- **E4.1** Given B-17 was withdrawn at 08:00 with reason *lock failed*, When B-17 signals fault "BRAKE_SENSOR" at 09:00, Then nothing changes: B-17 stays withdrawn and no second Bike Withdrawn happens (Q2).

## Contract

### Commands

| Command | Issued by | Input | On success | Rejected when |
|---------|-----------|-------|------------|---------------|
| Signal Fault | Bike (the bike itself) | bike, fault code | Bike Withdrawn | |

### Queries

| Query | Asked by | Input | Result |
|-------|----------|-------|--------|
| None. | | | |

### Events

| Event | When | Data |
|-------|------|------|
| Bike Withdrawn | a bike that signalled a fault is withdrawn: at once outside a ride, at the end of the ride during one | bike, withdrawal reason *fault signalled*, fault code, withdrawn at |

## Data

| Shape | Field | Type | Constraint |
|-------|-------|------|------------|
| Fault signal | bike | Identifier (Bike) | required |
| Fault signal | fault code | Text | required, as the bike reports it |
| Fault signal | signalled at | Timestamp | |

## Out of scope

- A fault reported by a rider at return (`rider-returns-bike-at-station`).
- A fault reported to customer support (`fleet-operations-takes-broken-bike-out-of-circulation`).
- A technician's fault report (`technician-reports-fault`).
- A failed lock at return (`rider-returns-bike-at-station`, `rider-ends-ride-in-drop-zone`).
- Repairing the bike (`technician-reserves-broken-bike` and following).

## Open questions

- **Q1** Is a rider whose reservation is cancelled because the bike signalled a fault told so? (R2, E2.1) Doesn't change whether the reservation is cancelled.
- **Q2** Should a second fault signal on an already withdrawn bike be recorded somewhere for the technician, e.g. the extra fault code? (R4, E4.1) E4.1 only says the bike's state doesn't change.
- ~~**Q3** The bike signals a fault during a ride: is the ride interrupted?~~ No; the bike is withdrawn as soon as the session ends. Outside a ride it is withdrawn at once, and a reservation on it is cancelled (role-played domain expert, DE-25, 2026-11-08); no withdrawal of any kind interrupts a ride (DE-86, 2026-11-08)

## Notes for the plan

- None.
