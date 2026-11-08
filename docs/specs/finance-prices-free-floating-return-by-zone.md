---
name: finance-prices-free-floating-return-by-zone
intent: finance-prices-free-floating-return-by-zone
system: velocity
status: approved
date: 2026-11-08
---

# Spec: Finance wants a free-floating return priced by the zone where the ride ended

Concrete rules, examples and contract for [the intent](../intents/finance-prices-free-floating-return-by-zone.md).

## Vocabulary

| Term | Meaning |
|------|---------|
| Free-floating return | A ride the rider ended at their current position instead of at a station; the ride ends where the rider is. |
| Zone map | The city's map of approved drop zones. |
| Drop zone | A city-approved area where a bike may be left without a station. |
| Unauthorized zone | Any position outside every drop zone. |
| Return fee | The drop zone fee (launch: PLN 2.00) or the unauthorized zone return fee (launch: PLN 40.00) (DE-30); only free-floating returns pay one (DE-34). |
| Total fee | Hourly fees + return fee + penalty (hourly fees as in `finance-prices-ride-by-tiered-tariff`). |

## Rules

All examples on 2026-11-09 with the launch tariff unless stated otherwise. Position (52.2298, 21.0123) lies inside the Old Town Square drop zone on the zone map; position (52.2500, 21.1000) lies outside every drop zone.

### R1: A free-floating return inside a drop zone adds the drop zone fee

- **E1.1** Given Anna's ride on bike B-17 started at 10:00 and ended with a free-floating return at (52.2298, 21.0123) at 10:30, When the ride is priced, Then its hourly fees are PLN 3.00, its return fee is the drop zone fee PLN 2.00 and its total fee is PLN 5.00 (Ride Priced).
- **E1.2** Given Anna's ride started at 10:00 and ended with a free-floating return at (52.2298, 21.0123) at 10:10, inside the free period, When the ride is priced, Then its hourly fees are PLN 0.00, its return fee is PLN 2.00 and its total fee is PLN 2.00 (Ride Priced) (DE-84).
- **E1.3** Given Anna's ride started at 10:00 and ended with a free-floating return at 10:30 at (52.23104, 21.0122), 149 m from the centre of the Old Town Square drop zone (radius 150 m), right at its border, When the ride is priced, Then its return fee is the drop zone fee PLN 2.00 and its total fee is PLN 5.00 (Ride Priced) (DE-84).

### R2: A free-floating return outside every drop zone adds the unauthorized zone return fee

- **E2.1** Given Ben's ride on bike B-22 started at 10:00 and ended with a free-floating return at (52.2500, 21.1000) at 10:30, When the ride is priced, Then its hourly fees are PLN 3.00, its return fee is the unauthorized zone return fee PLN 40.00 and its total fee is PLN 43.00 (Ride Priced).
- **E2.2** Given the Old Town Square drop zone was removed from the zone map at 10:20, and Anna's ride started at 10:00 and ended with a free-floating return at (52.2298, 21.0123) at 10:30, When the ride is priced, Then the zone map valid at the ride's end applies, her return fee is the unauthorized zone return fee PLN 40.00 and her total fee is PLN 43.00 (Ride Priced) (DE-84).

### R3: A lock failure pays no return fee, wherever the bike is (DE-23, DE-32)

- **E3.1** Given Ben's ride started at 10:00 and he ended it at (52.2500, 21.1000) at 10:30 but the lock didn't close, so it ended in a lock failure, When the ride is priced, Then its return fee is PLN 0.00 and its total fee is PLN 3.00 (Ride Priced).
- **E3.2** Given Anna's ride started at 10:00 and ended in a lock failure at (52.2298, 21.0123) at 10:30, When the ride is priced, Then no drop zone fee is added and its total fee is PLN 3.00 (Ride Priced).

### R4: The return fee comes from the tariff valid when the ride started (vision)

Given the launch tariff and tariff T-2, valid from 2026-11-09 12:00, with a drop zone fee of PLN 3.00 and every other price as in the launch tariff:

- **E4.1** Given Anna's ride started at 11:50 and ended with a free-floating return at (52.2298, 21.0123) at 12:30, When the ride is priced, Then its return fee is PLN 2.00 and its total fee is PLN 5.00 (Ride Priced).
- **E4.2** Given Anna's ride started at 12:05 and ended with a free-floating return at (52.2298, 21.0123) at 12:40, When the ride is priced, Then its return fee is PLN 3.00 and its total fee is PLN 6.00 (Ride Priced).

## Contract

### Commands

| Command | Issued by | Input | On success | Rejected when |
|---------|-----------|-------|------------|---------------|
| Price Ride | Finance, whenever a rental session ended (Rental Session Ended) | ride, rider, started at, ended at, ending, position | Ride Priced | none |

### Queries

| Query | Asked by | Input | Result |
|-------|----------|-------|--------|
| None. | | | |

### Events

| Event | When | Data |
|-------|------|------|
| Ride Priced | a finished ride's total fee is calculated | ride, rider, tariff, billing hours, hourly fees, return fee, return fee kind, penalty, total fee, priced at |

## Data

| Shape | Field | Type | Constraint |
|-------|-------|------|------------|
| Ride | position | Position (latitude, longitude) | required for a free-floating return and a lock failure away from a station |
| Ride price | return fee kind | one of {none, drop zone, unauthorized zone} | none for every ending except free-floating return (R3, DE-34) |
| Ride price | return fee | Money ≥ 0 | from the tariff valid at the ride's start (R4) |
| Ride price | total fee | Money ≥ 0 | hourly fees + return fee + penalty |

## Out of scope

- The hourly fees themselves (`finance-prices-ride-by-tiered-tariff`).
- Ending the ride in a drop zone (`rider-ends-ride-in-drop-zone`).
- The penalty (`finance-penalizes-overdue-rental`).
- Changing the city's zone map.

## Open questions

- ~~**Q1** Does a free-floating return inside the free period pay the return fee, although the vision says "very quick trips cost nothing"?~~ Yes: "cost nothing" is about hourly fees; the return fee depends only on where the ride ends (role-played domain expert, DE-84, 2026-11-08)
- ~~**Q2** Is a position exactly on a drop zone's border inside the drop zone?~~ Yes, the border counts as inside (role-played domain expert, DE-84, 2026-11-08)
- **Q3** Is a bike left next to a station without being docked in a drop zone or an unauthorized zone? (R1, R2)
- ~~**Q4** If the zone map changes during a ride, which map decides the return fee?~~ The zone map valid at the end of the ride (role-played domain expert, DE-84, 2026-11-08)
- ~~**Q5** What are the launch return fees?~~ Drop zone fee PLN 2.00, unauthorized zone return fee PLN 40.00 (role-played domain expert, DE-30, 2026-11-08)
- ~~**Q6** Does a lock failure at a free-floating return pay a return fee?~~ No, wherever the bike is (role-played domain expert, DE-23 and DE-32, 2026-11-08)
- ~~**Q7** Does a station return pay a return fee?~~ No, only free-floating returns do (role-played domain expert, DE-34, 2026-11-08)

## Notes for the plan

- None.
