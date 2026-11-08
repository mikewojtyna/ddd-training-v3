---
name: technician-starts-maintenance
sequence: 34
title: A technician wants to unlock a reserved broken bike and start maintenance
system: velocity
actor: Technician
activity: Keep the fleet healthy
task: Start maintenance
status: delivered
date: 2026-11-08
supersedes:
superseded_by:
source: docs/domains/velocity/velocity-vision.md "Broken Bikes & Maintenance", "Key Business Rules to Know"; docs/bounded-contexts/velocity/domain-expert-answers.md DE-11, DE-15, DE-16
---

# A technician wants to unlock a reserved broken bike and start maintenance

## Intent

As a **Technician**, I want to **unlock the broken bike I reserved and start maintenance on it** so that **I can repair it without a ride starting or anyone being billed**.

## Why

In the maintenance flow the technician "unlocks the bike" and "maintenance is started. Unlike a rider's unlock, a technician's unlock never starts a rental session" ("Broken Bikes & Maintenance"). "Maintenance is never billed. It is not a ride." When the unlock fails, "the reservation is released, the bike stays broken, and a technician tries again later".

## Business outcome

- Bike Unlocked: the broken bike is open for the technician who reserved it.
- Maintenance Started: the technician is working on the bike.
- Unlock Failed: the lock didn't open; the reservation is released and the bike stays broken.

## Success signal

- (suggested) Time from a technician's reservation to maintenance started.
- (suggested) No ride is ever priced or charged for a technician's unlock.

## Business rules mentioned

- "Only riders and technicians reserve and unlock bikes. A rider's unlock starts a rental session; a technician's unlock starts maintenance." (vision)
- "Maintenance is never billed." (vision)
- "The unlock fails. The reservation is released, the bike stays broken, and a technician tries again later." (vision)

## Scope

- **In:** a technician unlocking the broken bike they reserved; maintenance starting; a failed unlock.
- **Out:** reserving the bike (`technician-reserves-broken-bike`); returning the fixed bike (`technician-returns-fixed-bike-to-circulation`); a rider's unlock (`rider-unlocks-reserved-bike`).

## Related intents

- follows: `technician-reserves-broken-bike`
- enables: `technician-returns-fixed-bike-to-circulation`
- builds on: `rider-unlocks-reserved-bike`

## Open questions

- None.

## Notes for the spec

- Accepted under the trainer's standing instruction of 2026-11-08 (autonomous run), to be reviewed in the PR.
