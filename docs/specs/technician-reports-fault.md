---
name: technician-reports-fault
intent: technician-reports-fault
system: velocity
status: approved
date: 2026-11-08
---

# Spec: A technician wants to report a bike broken during a routine check

Concrete rules, examples and contract for [the intent](../intents/technician-reports-fault.md).

## Vocabulary

| Term | Meaning |
|------|---------|
| Fault report | A technician's statement that a bike is faulty, with a short description of the fault. |
| Broken bike | A bike reported faulty; it waits for a technician and is never offered to riders until it is back in circulation (vision). |
| Withdrawn bike | A bike no longer offered to riders; only a technician may reserve it (DE-16). |
| Routine check | A technician looking over bikes in circulation; not modelled itself, only its fault reports are. |

## Rules

### R1: A technician's fault report makes the bike broken and withdraws it from riders at once (DE-62)

- **E1.1** Given bike B-22 is available at station Central and isn't broken, When technician Tom reports a fault on B-22 at 09:00 ("loose handlebar"), Then B-22 is broken with fault source *technician* (Bike Reported Broken) and is withdrawn with reason *requested* (Bike Withdrawn).
- **E1.2** Given Tom reported B-22 broken at 09:00, When rider Anna, standing 10 m from B-22, reserves it at 09:05, Then the reservation is rejected.
- **E1.3** Given Anna's rental session on B-22 started at 08:50, When Tom reports a fault on B-22 at 09:00 ("cracked rear light"), Then B-22 is broken and withdrawn (Bike Reported Broken, Bike Withdrawn) and Anna's ride goes on until she returns B-22 (DE-92, DE-86).

### R2: A rider's reservation on the reported bike is cancelled (DE-62 "at once")

- **E2.1** Given B-22 is reserved for Anna until 09:02 (DE-10), When Tom reports a fault on B-22 at 09:01 ("worn brake pads"), Then Anna's reservation is cancelled and B-22 is broken and withdrawn (Bike Reported Broken, Bike Withdrawn).

### R3: A fault report on a bike that is already broken changes nothing

- **E3.1** Given B-17 has been broken since 08:00 with fault source *lock failed*, When Tom reports a fault on B-17 at 09:00 ("bent mudguard"), Then nothing is recorded: no Bike Reported Broken, no Bike Withdrawn, and B-17 stays broken with fault source *lock failed*.

## Contract

### Commands

| Command | Issued by | Input | On success | Rejected when |
|---------|-----------|-------|------------|---------------|
| Report Fault | Technician | technician, bike, description | Bike Reported Broken, Bike Withdrawn | – |

### Queries

| Query | Asked by | Input | Result |
|-------|----------|-------|--------|
| None. | | | |

### Events

| Event | When | Data |
|-------|------|------|
| Bike Reported Broken | a bike that wasn't broken is reported faulty | bike, fault source (*technician*), technician, description, reported at |
| Bike Withdrawn | the bike stops being offered to riders | bike, withdrawal reason (*requested*), withdrawn at |

## Data

| Shape | Field | Type | Constraint |
|-------|-------|------|------------|
| Fault report | technician | Identifier (Technician) | required |
| Fault report | bike | Identifier (Bike) | required; a bike known to VeloCity |
| Fault report | description | Text | required, not blank |

## Out of scope

- Faults detected at a return, signalled by the bike or reported through an incident (`fleet-operations-takes-broken-bike-out-of-circulation`, `fleet-operations-withdraws-bike-signalling-fault`).
- Riders reporting incidents (`rider-reports-incident`).
- Reserving and repairing the bike (`technician-reserves-broken-bike`).

## Open questions

- ~~**Q1** Can a technician report a bike broken, and is it withdrawn at once?~~ Yes, during a routine check; it is withdrawn from riders at once (role-played domain expert, DE-62, 2026-11-08)
- ~~**Q2** Can a technician report a bike that a rider is riding right now, and does the ride go on?~~ Yes, the report is accepted and the ride goes on (role-played domain expert, DE-92, DE-86, 2026-11-08)
- **Q3** Should a fault report on an already broken bike add the technician's description to it, for the technician who repairs it? (E3.1)

## Notes for the plan

- Accepted under the trainer's standing instruction of 2026-11-08 (autonomous run); the intent had no technical notes.
