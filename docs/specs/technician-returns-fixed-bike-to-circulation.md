---
name: technician-returns-fixed-bike-to-circulation
intent: technician-returns-fixed-bike-to-circulation
system: velocity
status: approved
date: 2026-11-08
---

# Spec: A technician wants to return a fixed bike to circulation

Concrete rules, examples and contract for [the intent](../intents/technician-returns-fixed-bike-to-circulation.md).

## Vocabulary

| Term | Meaning |
|------|---------|
| Under maintenance | A broken bike unlocked by a technician, who is working on it. |
| Return to circulation | The technician leaves the fixed bike at a station or in a drop zone, where riders can rent it again (DE-60). Returning it means it is fixed; there is no separate step (DE-91). |
| Bike fixed | The moment maintenance succeeds; here the moment the bike is returned to circulation. |
| Fleet-issue incident | A rider's incident about a named bike that is broken or can't be unlocked (DE-50); linked to the broken bike. |

## Rules

### R1: Returning a bike under maintenance fixes it and makes it available where the technician leaves it (DE-60)

- **E1.1** Given bike B-17 has been under maintenance by technician Tom since 09:20, When Tom returns B-17 to circulation at station Central at 11:00, Then B-17 is fixed (Bike Fixed) and available to riders at Central (Bike Made Available).
- **E1.2** Given B-22 has been under maintenance by Tom since 09:20, When Tom returns B-22 to circulation in drop zone Old Town Square at position (52.2297, 21.0122) at 11:00, Then B-22 is fixed (Bike Fixed) and available to riders at that position (Bike Made Available).
- **E1.3** Given B-17 has been under maintenance by Tom since 09:20, When technician Tara returns B-17 to circulation at Old Town at 11:00, Then B-17 is fixed (Bike Fixed) and available at Old Town (Bike Made Available): any technician may return it (DE-91).

### R2: A bike returns to circulation only after maintenance has started (vision)

- **E2.1** Given B-17 is broken and reserved for Tom, but Tom hasn't unlocked it, When Tom returns B-17 to circulation at Central at 09:15, Then it is rejected and B-17 stays broken.
- **E2.2** Given B-40 is available at Central and isn't broken, When Tom returns B-40 to circulation at Old Town at 10:00, Then it is rejected and nothing changes.

### R3: Fixing a bike resolves every linked fleet-issue incident that isn't resolved yet, whatever its state (DE-55)

- **E3.1** Given B-17 is under maintenance by Tom and linked to fleet-issue incident I-1 (accepted by agent Zoe) and I-2 (reported, not picked up yet), When Tom returns B-17 to circulation at Central at 11:00, Then I-1 and I-2 are each resolved with "bike fixed" (Incident Resolved).
- **E3.2** Given B-17 is under maintenance by Tom and linked to fleet-issue incident I-1, which Zoe resolved at 10:00 with "brake adjusted on site", When Tom returns B-17 to circulation at Central at 11:00, Then I-1 stays resolved with Zoe's resolution and no second Incident Resolved is recorded for it.

## Contract

### Commands

| Command | Issued by | Input | On success | Rejected when |
|---------|-----------|-------|------------|---------------|
| Return Bike to Circulation | Technician | technician, bike, station or drop-zone position | Bike Fixed, Bike Made Available, Incident Resolved | R2 |

### Queries

| Query | Asked by | Input | Result |
|-------|----------|-------|--------|
| None. | | | |

### Events

| Event | When | Data |
|-------|------|------|
| Bike Fixed | a bike under maintenance is returned to circulation | bike, technician, fixed at, linked incidents |
| Bike Made Available | the fixed bike is offered to riders again | bike, station or position, available at |
| Incident Resolved | a linked fleet-issue incident that wasn't resolved is resolved because the bike is fixed | incident, resolution ("bike fixed"), resolved at |

## Data

| Shape | Field | Type | Constraint |
|-------|-------|------|------------|
| Return to circulation | technician | Identifier (Technician) | required |
| Return to circulation | bike | Identifier (Bike) | required; under maintenance (R2) |
| Return to circulation | place | one of {station, position} | a station, or the station or position the technician names, trusted as given (DE-60, DE-91) |

## Out of scope

- Unlocking the bike and starting maintenance (`technician-starts-maintenance`).
- Retiring a bike that can't be repaired (DE-61, out of scope).
- Resolving incidents that aren't fleet issues of this bike (`support-agent-resolves-incident`).

## Open questions

- ~~**Q1** Where do fixed bikes go?~~ The technician brings the bike to a station or a drop zone and returns it to circulation there (role-played domain expert, DE-60, 2026-11-08)
- ~~**Q2** What happens to a fleet-issue incident when its bike is fixed?~~ It is resolved automatically ("bike fixed"), whatever state it is in, unless it is already resolved (role-played domain expert, DE-55, 2026-11-08)
- ~~**Q3** Is a separate "bike fixed" step needed before the return (vision steps 5 and 6)?~~ No, returning the bike to circulation after maintenance is the moment it is fixed (role-played domain expert, DE-91, 2026-11-08)
- ~~**Q4** Must the technician who started the maintenance return the bike?~~ No, any technician may return a bike under maintenance (role-played domain expert, DE-91, 2026-11-08)
- ~~**Q5** May a technician return a fixed bike at a position outside every station and drop zone?~~ The technician names the station or position where they leave it, and it is trusted as given (role-played domain expert, DE-91, 2026-11-08)

## Notes for the plan

- Accepted under the trainer's standing instruction of 2026-11-08 (autonomous run); the intent had no technical notes.
