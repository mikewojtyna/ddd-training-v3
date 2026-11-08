---
name: finance-prices-free-floating-return-by-zone
sequence: 14
title: Finance wants a free-floating return priced by the zone where the ride ended
system: velocity
actor: Finance
activity: Pay for the ride
task: Price the ride
status: specified
date: 2026-11-08
supersedes:
superseded_by:
source: docs/domains/velocity/velocity-vision.md "Drop Zone Return (Free-Floating)", "Who Uses the System" (City Operations), "Key Business Rules to Know"; domain-expert answers DE-23, DE-30, DE-32, DE-34
---

# Finance wants a free-floating return priced by the zone where the ride ended

## Intent

As a **Finance** team, I want **a free-floating return priced by the zone where the ride ended** so that **the shape of the network the city agreed to is enforced by pricing instead of by physical infrastructure**.

## Why

"The shape of the VeloCity network is enforced by pricing, not by locks. Riders can always end their ride; where they end it decides what they pay." (vision, "Key Business Rules to Know"). Free-floating returns cost the operator more to manage than station returns, and returns outside the city's approved drop zones break the network rules. The city defines where drop zones are ("City Operations").

## Business outcome

- Ride Priced: a ride that ended free-floating carries a drop zone fee or an unauthorized zone return fee on top of its hourly fees.

## Success signal

- (suggested) The share of free-floating returns paying the unauthorized zone return fee falls over time.

## Business rules mentioned

- "Approved drop zone. A drop zone fee is applied — small, but real." (vision)
- "Unauthorized zone. A much higher unauthorized zone return fee is applied." (vision)
- "The drop zone fee is normal; the unauthorized zone fee is a signal." (vision)
- "The ride always ends where the rider is." (vision)
- A lock failure at a free-floating return pays no return fee, wherever the bike is (DE-23, DE-32).
- Only free-floating returns pay a return fee (DE-34).
- Launch prices: drop zone fee PLN 2.00, unauthorized zone return fee PLN 40.00 (DE-30).

## Scope

- **In:** adding the drop zone fee or the unauthorized zone return fee to a ride that ended free-floating, decided by the city's zone map at the position where the ride ended.
- **Out:** the hourly fees themselves (`finance-prices-ride-by-tiered-tariff`); ending the ride in a drop zone (`rider-ends-ride-in-drop-zone`); the penalty (`finance-penalizes-overdue-rental`); changing the city's zone map (not an intent yet).

## Related intents

- builds on: `finance-prices-ride-by-tiered-tariff`
- builds on: `rider-ends-ride-in-drop-zone`
- enables: `finance-charges-finished-ride`

## Open questions

- Does a very quick free-floating trip, which the vision says "costs nothing", still pay the drop zone fee?
- Is a bike left right next to a station, but not docked, in a drop zone or an unauthorized zone?

## Notes for the spec

- Accepted under the trainer's standing instruction of 2026-11-08 (autonomous run), to be reviewed in the PR.
