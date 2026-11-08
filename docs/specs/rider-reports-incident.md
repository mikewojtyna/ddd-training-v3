---
name: rider-reports-incident
intent: rider-reports-incident
system: velocity
status: approved
date: 2026-11-08
---

# Spec: A rider wants to report an incident

Concrete rules, examples and contract for [the intent](../intents/rider-reports-incident.md).

## Vocabulary

| Term | Meaning |
|------|---------|
| Incident | A problem a rider reported; it is worked by a support agent until it is resolved. |
| Category | What the rider picks when reporting: *broken bike*, *can't unlock*, *wrongful charge*, *safety* or *other*. |
| Incident type | How the incident is routed: *fleet issue*, *billing dispute*, *safety concern* or *other*; detected from the category (DE-50). |
| Fleet issue | An incident about a specific faulty bike; the bike becomes broken and is handed over to technicians (DE-52). |
| Open | Reported and not yet picked up by any support agent. |
| Broken | A bike that riders can no longer rent until a technician fixes it (vision, "Broken Bikes & Maintenance"). |

## Rules

### R1: A rider can report an incident at any time; it is open until a support agent picks it up

- **E1.1** Given Anna has no incidents, When she reports *wrongful charge* with "charged twice for one ride" at 18:20 on 2026-11-08, Then incident I-2 is reported for Anna as open, with no assigned agent and type *billing dispute* (Incident Reported).
- **E1.2** Given Ben's account is blocked for *outstanding debt*, When Ben reports *other* with "the app shows a ride I didn't take" at 09:10 on 2026-11-10, Then incident I-8 is reported for Ben with type *other* (Incident Reported) (DE-97).

### R2: The incident type is detected from the category the rider picks

- **E2.1** Given bike B-22 stands at station Old Town, When Ben reports *can't unlock* for B-22 with "the lock doesn't open" at 08:05 on 2026-11-09, Then incident I-3 is reported with type *fleet issue* (Incident Reported, Fleet Issue Reported).
- **E2.2** Given Ben has no incidents, When he reports *safety* with "a car cut me off at the Central crossing" at 17:40 on 2026-11-09, Then incident I-4 is reported with type *safety concern* (Incident Reported).
- **E2.3** Given Anna has no open incidents, When she reports *broken bike* without naming a bike, with "the bike I rode yesterday creaked" at 12:00 on 2026-11-10, Then incident I-5 is reported with type *other* and nothing is handed over to technicians (Incident Reported).

### R3: A fleet issue makes its bike broken at once and hands it over to technicians

- **E3.1** Given bike B-17 is available at station Central, When Anna reports *broken bike* for B-17 with "rear brake doesn't work" at 10:00 on 2026-11-08, Then incident I-1 is reported with type *fleet issue* and B-17 is broken from 10:00, so no rider can reserve it (Incident Reported, Fleet Issue Reported).
- **E3.2** Given bike B-17 is available at Central, When Anna reports *wrongful charge* naming B-17 with "charged for a ride on B-17 I never took", Then the incident's type is *billing dispute*, nothing is handed over to technicians and B-17 stays available (Incident Reported).
- **E3.3** Given Anna is riding B-17 in a rental session that started at 09:40 on 2026-11-11, When she reports *broken bike* for B-17 with "gears slip" at 09:55, Then incident I-9 is a *fleet issue* and B-17 is broken (Incident Reported, Fleet Issue Reported), but Anna's ride goes on (DE-86).

## Contract

### Commands

| Command | Issued by | Input | On success | Rejected when |
|---------|-----------|-------|------------|---------------|
| Report Incident | Rider | rider, category, description, bike (optional) | Incident Reported, Fleet Issue Reported | none (Fleet Issue Reported only for a fleet issue, R3) |

### Queries

| Query | Asked by | Input | Result |
|-------|----------|-------|--------|
| None. | | | |

### Events

| Event | When | Data |
|-------|------|------|
| Incident Reported | a rider reported an incident and its type was detected | incident, rider, category, incident type, description, bike (if named), reported at |
| Fleet Issue Reported | an incident of type *fleet issue* was reported | incident, bike, description, reported at |

## Data

| Shape | Field | Type | Constraint |
|-------|-------|------|------------|
| Incident report | category | one of {broken bike, can't unlock, wrongful charge, safety, other} | required |
| Incident report | description | Text | the rider's own words |
| Incident report | bike | Identifier (Bike) | optional; with *broken bike* or *can't unlock* it makes the incident a fleet issue (DE-50) |
| Incident | incident type | one of {fleet issue, billing dispute, safety concern, other} | detected from category and bike (DE-50) |

## Out of scope

- Picking up, accepting and rejecting incidents (`support-agent-decides-on-incident`).
- Escalating and resolving (`support-agent-escalates-incident`, `support-agent-resolves-incident`).
- Taking the broken bike out of circulation, cancelling a reservation on it, repairing it (`fleet-operations-takes-broken-bike-out-of-circulation`, `technician-returns-fixed-bike-to-circulation`).

## Open questions

- ~~**Q1** How is the incident type detected?~~ From the category: *broken bike* or *can't unlock* with a bike → fleet issue; *wrongful charge* → billing dispute; *safety* → safety concern; anything else → other; a broken-bike report without a bike is other (role-played domain expert, DE-50, 2026-11-08)
- ~~**Q2** When does a fleet issue make the bike broken?~~ Immediately when the incident is reported and its type detected (role-played domain expert, DE-52, 2026-11-08)
- ~~**Q3** A rider's *can't unlock* report makes the bike broken, but a failed unlock signal doesn't (DE-15). Intended?~~ Yes: a rider's report is a fleet issue and the bike is broken; a single failed unlock signal is not a report (role-played domain expert, DE-85, 2026-11-08)
- ~~**Q4** Is the ride interrupted when the rider reports a fleet issue on the bike they ride?~~ No, no withdrawal interrupts a ride (role-played domain expert, DE-86, 2026-11-08)
- ~~**Q5** Can a blocked rider report an incident?~~ Yes (role-played domain expert, DE-97, 2026-11-08)

## Notes for the plan

- None.
