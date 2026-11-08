---
name: rider-reports-incident
sequence: 25
title: A rider wants to report an incident
system: velocity
actor: Rider
activity: Get help
task: Report an incident
status: planned
date: 2026-11-08
supersedes:
superseded_by:
source: docs/domains/velocity/velocity-vision.md "Customer Support & Incidents", "Broken Bikes & Maintenance"; docs/bounded-contexts/velocity/domain-expert-answers.md DE-50, DE-52
---

# A rider wants to report an incident

## Intent

As a **Rider**, I want **to report an incident (a broken bike, a bike I can't unlock, a wrongful charge, a safety issue or anything else)** so that **someone at VeloCity takes care of it and, if a bike is faulty, nobody else gets it**.

## Why

"Riders can report incidents at any time: a broken brake, a bike they can't unlock, a wrongful charge, an unpleasant experience." The incident's type is detected "so it can be routed correctly", and "when an incident is about a faulty bike (a fleet issue), the bike is marked as broken and handed over to technicians" ("Customer Support & Incidents"). A rider's report is also one of the ways a bike becomes broken ("Broken Bikes & Maintenance").

## Business outcome

- Incident Reported: the rider's problem is on record with its detected type, waiting for a support agent.
- Fleet Issue Reported: a faulty bike named by the rider is handed over to technicians.

## Success signal

- (suggested) Share of fleet issues whose bike is out of riders' reach within a minute of the report.
- (suggested) Fewer riders contacting VeloCity twice about the same problem.

## Business rules mentioned

- Riders can report incidents "at any time" (vision).
- The type is detected from the category the rider picks: *broken bike* or *can't unlock* with a bike named → fleet issue; *wrongful charge* → billing dispute; *safety* → safety concern; anything else → other. A broken-bike report without a bike is other (DE-50).
- A fleet issue makes the bike broken immediately when the incident is reported and its type detected. "Safety first." (DE-52)

## Scope

- **In:** reporting an incident with a category, a description and optionally a bike; detecting its type; handing a fleet issue over to technicians.
- **Out:** picking up and deciding (`support-agent-decides-on-incident`); escalating (`support-agent-escalates-incident`); resolving (`support-agent-resolves-incident`); taking the bike out of circulation and repairing it (`fleet-operations-takes-broken-bike-out-of-circulation`, `technician-returns-fixed-bike-to-circulation`).

## Related intents

- enables: `support-agent-decides-on-incident`
- enables: `fleet-operations-takes-broken-bike-out-of-circulation`

## Open questions

- A rider's *can't unlock* report makes the bike broken (DE-50, DE-52), while a failed unlock signalled by the bike itself does not (DE-15). Is that difference intended?
- Can a blocked rider, or a rider who hasn't finished joining, report an incident?

## Notes for the spec

- Accepted under the trainer's standing instruction of 2026-11-08 (autonomous run), to be reviewed in the PR.
