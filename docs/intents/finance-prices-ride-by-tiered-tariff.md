---
name: finance-prices-ride-by-tiered-tariff
sequence: 13
title: Finance wants every finished ride priced by the tiered tariff
system: velocity
actor: Finance
activity: Pay for the ride
task: Price the ride
status: specified
date: 2026-11-08
supersedes:
superseded_by:
source: docs/domains/velocity/velocity-vision.md "Billing", "Returns" (Station Return); domain-expert answers DE-30, DE-31, DE-32, DE-34 (docs/bounded-contexts/velocity/domain-expert-answers.md)
---

# Finance wants every finished ride priced by the tiered tariff

## Intent

As a **Finance** team, I want **every finished ride priced by the time-based, tiered tariff** so that **short trips stay essentially free, long trips get progressively more expensive, and every rider knows what a ride costs before it is charged**.

## Why

The vision's pricing idea: "encourage short trips, let long trips get progressively more expensive, and never let a bike be held forever" ("Billing"). A ride can't be charged until its total fee is known, so pricing is the first step of paying for a ride. This intent covers the time-based part: the free period and the hourly fees, which every ride pays whatever way it ended. Return fees and the penalty build on it in their own intents.

## Business outcome

- Ride Priced: the finished ride has a total fee made of its hourly fees, calculated with the tariff valid when the ride started.

## Success signal

- (suggested) A high share of rides ends inside the free period.
- (suggested) No support contacts asking how a ride's fee was calculated that the fee breakdown couldn't answer.

## Business rules mentioned

- "Every session begins with a short free window. Very quick trips (a few minutes) cost nothing." (vision)
- Hour one, two and three each have their own fee; "from the fourth hour on the same hourly fee is applied for each additional hour." (vision)
- "When it completes, the first-hour fee is applied" (vision) — answered: every started billing hour is charged (DE-31).
- "The rider always pays according to the prices that were valid when the ride started — a price change during a ride never affects that ride." (vision)
- "The rider is not charged extra because our lock failed." (vision, Station Return; DE-32)
- A station return pays no return fee (DE-34).
- Launch tariff: free period 15 minutes; PLN 3.00, 5.00, 7.00, then PLN 9.00 per hour (DE-30).

## Scope

- **In:** pricing a finished ride by its duration with the free period and the tiered hourly fees; rides that ended with a station return or a lock failure, which pay hourly fees only.
- **Out:** the drop zone fee and the unauthorized zone return fee (`finance-prices-free-floating-return-by-zone`); the penalty for exceeding the maximum rental time (`finance-penalizes-overdue-rental`); publishing new prices (`pricing-manager-publishes-tariff`); charging the fee (`finance-charges-finished-ride`).

## Related intents

- builds on: `rider-returns-bike-at-station`
- enables: `finance-prices-free-floating-return-by-zone`
- enables: `finance-penalizes-overdue-rental`
- enables: `finance-charges-finished-ride`
- follows: `pricing-manager-publishes-tariff`

## Open questions

- None blocking: the vision's "when it completes" is answered by DE-31.

## Notes for the spec

- Accepted under the trainer's standing instruction of 2026-11-08 (autonomous run), to be reviewed in the PR.
