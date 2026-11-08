---
name: finance-charges-finished-ride
sequence: 17
title: Finance wants every finished ride charged to the rider's card
system: velocity
actor: Finance
activity: Pay for the ride
task: Charge the ride
status: specified
date: 2026-11-08
supersedes:
superseded_by:
source: docs/domains/velocity/velocity-vision.md "Settling the Ride", "Payment Processors", "Key Business Rules to Know"; domain-expert answers DE-35, DE-37, DE-71
---

# Finance wants every finished ride charged to the rider's card

## Intent

As a **Finance** team, I want **every finished ride charged to the rider's card as soon as its fee is known** so that **VeloCity gets paid for its rides without the rider having to do anything**.

## Why

"Once the total fee is known, the ride has to be settled — the rider is charged for it through their payment method on file." (vision, "Settling the Ride"). "Every ride is eventually settled." (vision, "Key Business Rules to Know"). Trust is earned once at onboarding, so paying for a ride must need no action from the rider ("Background").

## Business outcome

- Ride Settled: the ride's fee was charged to the rider's card, or the fee was PLN 0.00 and nothing had to be charged.
- Charge Attempt Failed: the first charge didn't go through; the ride waits for a retry.

## Success signal

- (suggested) A high share of rides is settled on the first charge attempt.

## Business rules mentioned

- "A charge either succeeds or it fails." (vision, "Payment Processors")
- The first charge attempt happens right after the ride (DE-35).
- A ride priced at PLN 0.00 is settled at once, without charging the card (DE-37).
- The rider's current verified card is charged; a rider whose card was invalidated has none, so the charge fails (DE-71).

## Scope

- **In:** charging the priced fee of a finished ride once, right after the ride; settling free rides without a charge; recording a failed first attempt.
- **Out:** pricing the ride (`finance-prices-ride-by-tiered-tariff`); retrying a failed charge and turning the fee into outstanding debt (`finance-retries-failed-charge`); refunds after a billing dispute (not an intent yet).

## Related intents

- builds on: `finance-prices-ride-by-tiered-tariff`
- builds on: `rider-adds-payment-method`
- enables: `finance-retries-failed-charge`

## Open questions

- None blocking.

## Notes for the spec

- Accepted under the trainer's standing instruction of 2026-11-08 (autonomous run), to be reviewed in the PR.
