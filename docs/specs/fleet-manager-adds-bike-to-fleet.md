---
name: fleet-manager-adds-bike-to-fleet
intent: fleet-manager-adds-bike-to-fleet
system: velocity
status: approved
date: 2026-11-08
---

# Spec: A fleet manager wants to add a new bike to the fleet at a station

Concrete rules, examples and contract for [the intent](../intents/fleet-manager-adds-bike-to-fleet.md).

## Vocabulary

| Term | Meaning |
|------|---------|
| Fleet | All bikes VeloCity can lend to riders or repair. A bike joins the fleet once, when a fleet manager adds it at a station. |
| Available | A bike that may be reserved by a rider (vision "Renting a Bike"). |
| Station | An official location where bikes are docked and locked; the city decides where stations are (vision "City Operations"). |

## Rules

### R1: A new bike added at a station is available there from that moment (DE-18)

- **E1.1** Given bike B-40 isn't part of the fleet yet, When fleet manager Fiona adds B-40 to the fleet at station Central at 07:30, Then B-40 is available at Central from 07:30 (Bike Added to Fleet).
- **E1.2** Given Fiona added B-40 to the fleet at Central at 07:30, When rider Anna, standing next to B-40, reserves it at 07:45, Then B-40 is reserved for Anna (Bike Reserved, see `rider-reserves-bike`).

### R2: A bike joins the fleet only once

- **E2.1** Given B-40 was added to the fleet at Central on 2 November, When Fiona adds B-40 again at station Old Town on 8 November, Then it is rejected and B-40 stays where it is, in the state it is in.

### R3: A bike is added only at a station (DE-18, vision "City Operations")

- **E3.1** Given B-41 isn't part of the fleet yet and "Harbour Café" is not a station, When Fiona adds B-41 at "Harbour Café", Then it is rejected and B-41 isn't part of the fleet (Q1).

## Contract

### Commands

| Command | Issued by | Input | On success | Rejected when |
|---------|-----------|-------|------------|---------------|
| Add Bike to Fleet | Fleet Manager | bike, station | Bike Added to Fleet | R2, R3 |

### Queries

| Query | Asked by | Input | Result |
|-------|----------|-------|--------|
| None. | | | |

### Events

| Event | When | Data |
|-------|------|------|
| Bike Added to Fleet | a new bike has joined the fleet at a station | bike, station, position of the station, added at |

## Data

| Shape | Field | Type | Constraint |
|-------|-------|------|------------|
| New bike | bike | Identifier (Bike) | required; not yet part of the fleet (R2) |
| New bike | station | Identifier (Station) | required; one of the city's stations (R3) |
| New bike | added at | Timestamp | the moment the fleet manager added it |

## Out of scope

- Reserving the bike (`rider-reserves-bike`).
- Returning a fixed bike to circulation (`technician-returns-fixed-bike-to-circulation`).
- Taking a bike out of circulation (`fleet-operations-withdraws-bike-signalling-fault`, `fleet-operations-takes-broken-bike-out-of-circulation`).
- Retiring a bike for good (out of scope, DE-61).

## Open questions

- **Q1** Can a new bike be added to the fleet in a drop zone instead of at a station? (R3, E3.1) DE-18 only mentions stations; E3.1 shows the literal reading.
- ~~**Q2** Where do bikes come from?~~ A fleet manager adds each new bike at a station; from then on it is available (role-played domain expert, DE-18, 2026-11-08).

## Notes for the plan

- None.
