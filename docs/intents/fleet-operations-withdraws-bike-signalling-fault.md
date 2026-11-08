---
name: fleet-operations-withdraws-bike-signalling-fault
sequence: 31
title: Fleet operations wants a bike that signals a fault taken away from riders
system: velocity
actor: Fleet Operations
activity: Keep the fleet healthy
task: Take broken bikes out of circulation
status: delivered
date: 2026-11-08
supersedes:
superseded_by:
source: docs/domains/velocity/velocity-vision.md "Broken Bikes & Maintenance", "Key Business Rules to Know"; domain-expert answers DE-25 (2026-11-08)
---

# Fleet operations wants a bike that signals a fault taken away from riders

## Intent

As a member of **Fleet Operations**, I want **a bike that signals a fault of its own taken away from riders** so that **nobody starts a ride on a bike that we already know is faulty**.

## Why

"The bike itself signals a problem (for example, a failed lock or a mechanical fault detected at the station). From that moment on, the bike is broken and riders can no longer rent it" ("Broken Bikes & Maintenance"). "A broken bike is never offered to riders" ("Key Business Rules to Know"). The domain expert added that a ride is never interrupted by such a signal (DE-25).

## Business outcome

- Bike Withdrawn: the faulty bike is no longer offered to riders.

## Success signal

- (suggested) No ride starts on a bike after it signalled a fault.

## Business rules mentioned

- "From that moment on, the bike is broken and riders can no longer rent it." (vision)
- The ride isn't interrupted. The bike is withdrawn as soon as the session ends. Outside a ride it is withdrawn at once, and a reservation on it is cancelled (DE-25).

## Scope

- **In:** a fault signalled by the bike itself, when the bike is free, reserved by a rider, or ridden.
- **Out:** a fault reported by a rider at return (`rider-returns-bike-at-station`); a fault reported to customer support (`fleet-operations-takes-broken-bike-out-of-circulation`); a technician's fault report (`technician-reports-fault`); a failed lock at return (`rider-returns-bike-at-station`, `rider-ends-ride-in-drop-zone`); repairing the bike (`technician-reserves-broken-bike` and following).

## Related intents

- alternative to: `fleet-operations-takes-broken-bike-out-of-circulation`
- alternative to: `technician-reports-fault`
- enables: `technician-reserves-broken-bike`

## Open questions

- Is a rider whose reservation is cancelled because the bike signalled a fault told so?

## Notes for the spec

- Accepted under the trainer's standing instruction of 2026-11-08 (autonomous run), to be reviewed in the PR.
