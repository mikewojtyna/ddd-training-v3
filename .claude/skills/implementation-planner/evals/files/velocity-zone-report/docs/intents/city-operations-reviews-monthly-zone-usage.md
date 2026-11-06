---
name: city-operations-reviews-monthly-zone-usage
sequence: 2
title: City operations wants to review monthly bike usage per city zone
system: velocity
actor: City Operations
activity: Plan the network
task: Review usage per zone
status: specified
date: 2026-11-05
supersedes:
superseded_by:
source: contract with the city, clause 7.2 (monthly usage reporting)
---

# City operations wants to review monthly bike usage per city zone

## Intent

As **City Operations**, I want to **review how many rides started and ended in each city zone every month** so that **we can show the city where to add stations and meet the reporting clause of our contract**.

## Why

The city contract (clause 7.2) requires a monthly usage report per zone. Today it is assembled by hand from spreadsheets.

## Business outcome

- Monthly Zone Usage Published: the usage figures for every zone for a closed month are available to City Operations.

## Success signal

- (suggested) The report is delivered to the city by the 5th of each month without manual work.

## Business rules mentioned

- "Rides count in the zone where they started and in the zone where they ended."
- "A month is closed on its last day at midnight, city time."

## Scope

- **In:** monthly counts of started and ended rides per zone.
- **Out:** revenue per zone; real-time dashboards.

## Related intents

- follows: `rider-rents-bike-from-station`

## Open questions

- None.

## Notes for the spec

- The city would like a CSV export.
