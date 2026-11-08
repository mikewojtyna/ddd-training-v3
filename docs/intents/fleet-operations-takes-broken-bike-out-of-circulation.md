---
name: fleet-operations-takes-broken-bike-out-of-circulation
sequence: 30
title: Fleet operations wants every faulty bike known as broken and kept away from riders
system: velocity
actor: Fleet Operations
activity: Keep the fleet healthy
task: Take broken bikes out of circulation
status: planned
date: 2026-11-08
supersedes:
superseded_by:
source: docs/domains/velocity/velocity-vision.md "Broken Bikes & Maintenance", "Customer Support & Incidents", "Key Business Rules to Know"; docs/bounded-contexts/velocity/domain-expert-answers.md DE-16, DE-21, DE-23, DE-24, DE-25, DE-52
---

# Fleet operations wants every faulty bike known as broken and kept away from riders

## Intent

As a **Fleet Operations** team, I want **every bike that turned out faulty (a failed lock, a fault reported at return, a fault the bike signalled, a bike that was never returned, or a rider's fleet-issue incident) to become a broken bike that riders can't rent** so that **no rider is handed a broken bike and technicians know which bikes need repair**.

## Why

The vision says "when that happens, the bike must stop being offered to riders and must get fixed as quickly as possible" and lists several ways a bike is reported broken: a fault reported at return, a rider's incident about the bike, and the bike signalling a problem ("Broken Bikes & Maintenance"). A key business rule is "A broken bike is never offered to riders". A bike whose session was ended for exceeding the maximum rental time was never returned and is treated as broken too (DE-21). A fleet-issue incident makes the bike broken immediately, "safety first" (DE-52).

## Business outcome

- Bike Reported Broken: the bike is known as broken, with what reported it, and waits for a technician.
- Bike Withdrawn: a bike reported through a fleet-issue incident is no longer offered to riders.

## Success signal

- (suggested) No ride starts on a bike that was reported broken and not yet back in circulation.
- (suggested) Time from a fault being detected to the bike being known as broken stays within seconds.

## Business rules mentioned

- "From that moment on, the bike is broken and riders can no longer rent it." (vision)
- "A broken bike is never offered to riders. It stays out of circulation until a technician fixes it." (vision)
- "When an incident is about a faulty bike (a fleet issue), the bike is marked as broken and handed over to technicians for maintenance." (vision)
- A fleet issue makes the bike broken immediately when the incident is reported (DE-52).
- A bike not returned after the maximum rental time is treated as broken (DE-21).
- Only a technician can reserve a broken bike (DE-16).

## Scope

- **In:** a bike taken away from riders because of a fault (failed lock, fault reported at return, fault signalled, not returned) becoming broken; a fleet-issue incident making the bike broken and taking it away from riders; a second report about an already broken bike.
- **Out:** detecting the fault itself at return (`rider-returns-bike-at-station`, `rider-ends-ride-in-drop-zone`); ending an overdue rental (`fleet-operations-ends-overdue-rental`); taking a bike away when it signals a fault (`fleet-operations-withdraws-bike-signalling-fault`); reporting and typing the incident (`rider-reports-incident`); a technician's own fault report (`technician-reports-fault`); repairing the bike (`technician-reserves-broken-bike`, `technician-starts-maintenance`, `technician-returns-fixed-bike-to-circulation`).

## Related intents

- follows: `rider-returns-bike-at-station`
- follows: `rider-ends-ride-in-drop-zone`
- follows: `fleet-operations-ends-overdue-rental`
- follows: `fleet-operations-withdraws-bike-signalling-fault`
- follows: `rider-reports-incident`
- enables: `technician-reserves-broken-bike`
- enables: `technician-returns-fixed-bike-to-circulation`

## Open questions

- What happens when a rider reports a fleet issue about the bike they are riding right now?

## Notes for the spec

- Accepted under the trainer's standing instruction of 2026-11-08 (autonomous run), to be reviewed in the PR.
