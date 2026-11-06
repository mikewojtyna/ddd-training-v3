---
name: city-operations-reviews-monthly-zone-usage
intent: city-operations-reviews-monthly-zone-usage
system: velocity
status: approved
date: 2026-11-06
---

# Spec: City operations wants to review monthly bike usage per city zone

Concrete rules, examples and contract for [the intent](../intents/city-operations-reviews-monthly-zone-usage.md).

## Vocabulary

| Term | Meaning |
|------|---------|
| Zone | An area of the city as defined by the city's zone map. |
| Zone usage | For one zone and one closed month: the number of rides that started there and the number that ended there. |
| Closed month | A calendar month whose last day has passed midnight, city time. |

## Rules

### R1: A ride counts as started in the zone of its start and as ended in the zone of its end

- **E1.1** Given rider Anna rents a bike in zone Old Town on 2026-10-03 and returns it in zone Riverside on the same day, When City Operations reviews October 2026, Then Old Town shows 1 ride started and Riverside shows 1 ride ended.

### R2: Usage is published only for closed months

- **E2.1** Given it is 2026-10-31 23:30 city time, When City Operations asks for October 2026, Then the request is rejected because October is not closed yet.
- **E2.2** Given it is 2026-11-01 00:05 city time, When City Operations asks for October 2026, Then the usage for October is available (Monthly Zone Usage Published).

### R3: A ride belongs to the month in which it started

- **E3.1** Given Ben rents a bike on 2026-10-31 23:50 and returns it on 2026-11-01 00:20, When City Operations reviews October and November 2026, Then the ride counts as started and ended in October, and not in November.

## Contract

### Commands

| Command | Issued by | Input | On success | Rejected when |
|---------|-----------|-------|------------|---------------|
| Publish Monthly Zone Usage | City Operations | month | Monthly Zone Usage Published | R2 |

### Queries

| Query | Asked by | Input | Result |
|-------|----------|-------|--------|
| Zone Usage for Month | City Operations | month | list of zone, rides started, rides ended |

### Events

| Event | When | Data |
|-------|------|------|
| Monthly Zone Usage Published | usage for a closed month is published | month, list of zone usage |

## Data

| Shape | Field | Type | Constraint |
|-------|-------|------|------------|
| Zone usage | rides started | Number | ≥ 0 |
| Zone usage | rides ended | Number | ≥ 0 |

## Out of scope

- Revenue per zone.
- Real-time dashboards.

## Open questions

- None.

## Notes for the plan

- The city would like a CSV export.
