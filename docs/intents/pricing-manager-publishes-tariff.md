---
name: pricing-manager-publishes-tariff
sequence: 16
title: A pricing manager wants to publish a new tariff
system: velocity
actor: Pricing Manager
activity: Set prices
task: Publish a tariff
status: specified
date: 2026-11-08
supersedes:
superseded_by:
source: docs/domains/velocity/velocity-vision.md "Billing"; domain-expert answer DE-30
---

# A pricing manager wants to publish a new tariff

## Intent

As a **Pricing Manager**, I want to **publish a new tariff that becomes valid at a chosen moment** so that **VeloCity can change its prices without ever changing the price of a ride that has already started**.

## Why

The vision says "the rider always pays according to the prices that were valid when the ride started — a price change during a ride never affects that ride" ("Billing"). So prices do change over time, and each change must be announced with the moment it starts to apply. VeloCity launches with one tariff (DE-30); every later change is a newly published tariff.

## Business outcome

- Tariff Published: a complete set of prices (free period, hourly fees, return fees, penalty) is known, together with the moment from which it applies to newly started rides.

## Success signal

- (suggested) No ride is ever priced with a tariff that wasn't valid when it started.

## Business rules mentioned

- "A price change during a ride never affects that ride." (vision)
- A tariff consists of the free period, the fees of hour 1, 2 and 3, the fee of every further hour, the drop zone fee, the unauthorized zone return fee and the penalty (vision "Billing", DE-30).
- The drop zone fee is "small, but real"; the unauthorized zone return fee is "much higher" (vision).

## Scope

- **In:** publishing a complete tariff with the moment it becomes valid; seeing the published tariffs.
- **Out:** pricing a ride with the tariff (`finance-prices-ride-by-tiered-tariff`, `finance-prices-free-floating-return-by-zone`, `finance-penalizes-overdue-rental`); withdrawing a tariff before it becomes valid (not an intent yet); changing the city's zone map.

## Related intents

- enables: `finance-prices-ride-by-tiered-tariff`
- enables: `finance-prices-free-floating-return-by-zone`
- enables: `finance-penalizes-overdue-rental`

## Open questions

- Can a published tariff be withdrawn before it becomes valid?
- Must the unauthorized zone return fee always be higher than the drop zone fee?

## Notes for the spec

- Accepted under the trainer's standing instruction of 2026-11-08 (autonomous run), to be reviewed in the PR.
