---
name: finance-penalizes-overdue-rental
sequence: 15
title: Finance wants a penalty charged for a rental that exceeded the maximum rental time
system: velocity
actor: Finance
activity: Pay for the ride
task: Price the ride
status: specified
date: 2026-11-08
supersedes:
superseded_by:
source: docs/domains/velocity/velocity-vision.md "Billing", "Key Business Rules to Know"; domain-expert answers DE-20, DE-21, DE-30, DE-33
---

# Finance wants a penalty charged for a rental that exceeded the maximum rental time

## Intent

As a **Finance** team, I want **a penalty added to the price of a rental that exceeded the maximum rental time** so that **riders feel that keeping a bike that long is no longer a rental but a breach of the network's rules**.

## Why

"There is a hard ceiling: if a session runs past the maximum rental time, the session is ended automatically and a penalty is applied on top of the hourly fees. [...] It means 'at this point, this is no longer a rental — it is a problem.'" (vision, "Billing"). The maximum rental time exists "so bikes don't get parked on someone's balcony for a week".

## Business outcome

- Ride Priced: an overdue rental carries the penalty, shown apart from its hourly fees.

## Success signal

- (suggested) The number of rentals ended for exceeding the maximum rental time falls month over month.

## Business rules mentioned

- "A penalty is applied on top of the hourly fees." (vision)
- "The distinction between an hourly fee and a penalty matters. Hourly fees are the normal cost of the service. A penalty says the rider crossed a rule of the network." (vision)
- Maximum rental time is 12 hours; at exactly 12:00:00 the session is still a ride (DE-20).
- Hourly fees for the full 12 hours plus the penalty; no return fee, because the bike wasn't returned anywhere (DE-33).
- Launch penalty PLN 150.00 (DE-30).

## Scope

- **In:** pricing a rental that was ended automatically for exceeding the maximum rental time: hourly fees for the full 12 hours, the penalty, no return fee.
- **Out:** ending the overdue rental and withdrawing the bike (`fleet-operations-ends-overdue-rental`); the hourly fees themselves (`finance-prices-ride-by-tiered-tariff`); charging the fee (`finance-charges-finished-ride`).

## Related intents

- builds on: `finance-prices-ride-by-tiered-tariff`
- builds on: `fleet-operations-ends-overdue-rental`
- enables: `finance-charges-finished-ride`

## Open questions

- If an overdue rental is noticed and ended some minutes after the 12 hours, do the hourly fees still stop at 12 hours?

## Notes for the spec

- Accepted under the trainer's standing instruction of 2026-11-08 (autonomous run), to be reviewed in the PR.
