---
name: technician-returns-fixed-bike-to-circulation
sequence: 35
title: A technician wants to return a fixed bike to circulation
system: velocity
actor: Technician
activity: Keep the fleet healthy
task: Return a fixed bike to circulation
status: specified
date: 2026-11-08
supersedes:
superseded_by:
source: docs/domains/velocity/velocity-vision.md "Broken Bikes & Maintenance", "Customer Support & Incidents", "Key Business Rules to Know"; docs/bounded-contexts/velocity/domain-expert-answers.md DE-55, DE-60, DE-61
---

# A technician wants to return a fixed bike to circulation

## Intent

As a **Technician**, I want to **return a bike I have fixed to circulation at a station or in a drop zone** so that **riders can rent it again and the riders who reported it learn their problem is solved**.

## Why

The maintenance flow ends when "the bike is fixed" and "the bike is returned to circulation — it is back at a station or in a drop zone and available to riders again" ("Broken Bikes & Maintenance"). The technician brings the bike there (DE-60). "Every incident ... must end in a resolved state": a fleet-issue incident about the bike is resolved automatically once the bike is fixed (DE-55), so support agents don't chase incidents that are already solved.

## Business outcome

- Bike Fixed: maintenance on the bike succeeded.
- Bike Made Available: the bike is back at a station or in a drop zone and riders can rent it.
- Incident Resolved: every fleet-issue incident about the bike that wasn't resolved yet is resolved as "bike fixed".

## Success signal

- (suggested) Time from "reported broken" to "back in circulation".
- (suggested) No fleet-issue incident stays open after its bike is back in circulation.

## Business rules mentioned

- "A bike returns to circulation only after it is fixed." (vision)
- The technician brings the bike to a station or a drop zone and returns it to circulation there (DE-60).
- A fleet-issue incident is resolved automatically ("bike fixed") when the bike is fixed, whatever state it is in, unless it is already resolved (DE-55).
- Unrepairable bikes are out of scope (DE-61).

## Scope

- **In:** returning a bike under maintenance to circulation at a station or a drop zone; resolving the bike's fleet-issue incidents.
- **Out:** starting maintenance (`technician-starts-maintenance`); retiring an unrepairable bike (DE-61, not planned); resolving other incidents (`support-agent-resolves-incident`).

## Related intents

- follows: `technician-starts-maintenance`
- follows: `fleet-operations-takes-broken-bike-out-of-circulation`
- alternative to: `support-agent-resolves-incident`

## Open questions

- Is "bike fixed" a step of its own before the return, or does returning the bike mean it is fixed?
- Must the technician who started the maintenance be the one who returns the bike?

## Notes for the spec

- Accepted under the trainer's standing instruction of 2026-11-08 (autonomous run), to be reviewed in the PR.
