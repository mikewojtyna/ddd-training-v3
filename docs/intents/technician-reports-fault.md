---
name: technician-reports-fault
sequence: 32
title: A technician wants to report a bike broken during a routine check
system: velocity
actor: Technician
activity: Keep the fleet healthy
task: Report a fault
status: delivered
date: 2026-11-08
supersedes:
superseded_by:
source: docs/domains/velocity/velocity-vision.md "Broken Bikes & Maintenance", "Who Uses the System" (Technicians); docs/bounded-contexts/velocity/domain-expert-answers.md DE-62
---

# A technician wants to report a bike broken during a routine check

## Intent

As a **Technician**, I want to **report a bike broken when I find a fault during a routine check** so that **it is taken away from riders at once and gets repaired**.

## Why

Technicians look after the fleet, so they notice faults no rider or bike has reported yet: a loose handlebar, a worn brake pad. The vision lists only riders and the bike itself as reporters ("Broken Bikes & Maintenance"); the domain expert added that a technician can report a bike broken during a routine check and that it is withdrawn from riders at once (DE-62).

## Business outcome

- Bike Reported Broken: the bike is known as broken, reported by the technician.
- Bike Withdrawn: the bike is no longer offered to riders.

## Success signal

- (suggested) Share of bikes reported broken by technicians before any rider complained about them.

## Business rules mentioned

- "A technician can report a bike broken during a routine check; it is withdrawn from riders at once." (DE-62)
- "A broken bike is never offered to riders." (vision)

## Scope

- **In:** a technician reporting a fault on a bike in circulation; reporting a bike that is already broken.
- **Out:** faults detected at return or signalled by the bike (`fleet-operations-takes-broken-bike-out-of-circulation`, `fleet-operations-withdraws-bike-signalling-fault`); riders reporting incidents (`rider-reports-incident`); repairing the bike (`technician-reserves-broken-bike`).

## Related intents

- alternative to: `fleet-operations-takes-broken-bike-out-of-circulation`
- enables: `technician-reserves-broken-bike`

## Open questions

- Can a technician report a bike that a rider is riding right now, and what happens to the ride?

## Notes for the spec

- Accepted under the trainer's standing instruction of 2026-11-08 (autonomous run), to be reviewed in the PR.
