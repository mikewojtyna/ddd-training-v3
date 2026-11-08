---
name: finance-prices-ride-by-tiered-tariff
intent: finance-prices-ride-by-tiered-tariff
system: velocity
status: approved
date: 2026-11-08
---

# Spec: Finance wants every finished ride priced by the tiered tariff

Concrete rules, examples and contract for [the intent](../intents/finance-prices-ride-by-tiered-tariff.md).

## Vocabulary

| Term | Meaning |
|------|---------|
| Ride | A finished rental session, from its start to its end, with the way it ended. |
| Tariff | The prices valid from a moment on: free period, fees of hour 1, 2 and 3, fee of every further hour, drop zone fee, unauthorized zone return fee, penalty. Launch tariff: 15 min free, PLN 3.00 / 5.00 / 7.00, then PLN 9.00 per hour (DE-30). |
| Free period | The first 15 minutes of a ride, free of charge (DE-30). |
| Billing hour | Each started hour after the free period; every started billing hour is charged (DE-31). |
| Hourly fee | The fee of one billing hour: hour 1, 2 and 3 have their own fee, every hour from the 4th on costs the same. |
| Ending | How the ride ended: station return, free-floating return, lock failure, maximum rental time exceeded. |
| Return fee | Drop zone fee or unauthorized zone return fee; only free-floating returns pay one (DE-34). |
| Total fee | Hourly fees + return fee + penalty. |

## Rules

All examples on 2026-11-09 with the launch tariff unless stated otherwise.

### R1: A ride that lasts no longer than the free period is free

- **E1.1** Given Anna's ride on bike B-17 started at 10:00 and ended with a station return at Central at 10:12, When the ride is priced, Then its hourly fees are PLN 0.00 and its total fee is PLN 0.00 (Ride Priced).
- **E1.2** Given Anna's ride started at 10:00:00 and ended with a station return at Central at 10:15:00 (exactly 15 minutes), When the ride is priced, Then its total fee is PLN 0.00 (Ride Priced) (DE-31).

### R2: Every started billing hour is charged (DE-31)

- **E2.1** Given Ben's ride started at 10:00:00 and ended with a station return at Old Town at 10:15:01, When the ride is priced, Then it has 1 billing hour and its total fee is PLN 3.00 (Ride Priced) (DE-83).
- **E2.2** Given Ben's ride started at 10:00:00 and ended with a station return at Old Town at 11:15:00 (exactly 75 minutes), When the ride is priced, Then it has 1 billing hour and its total fee is PLN 3.00 (Ride Priced).
- **E2.3** Given Ben's ride started at 10:00:00 and ended with a station return at Old Town at 11:15:01, When the ride is priced, Then it has 2 billing hours, its hourly fees are PLN 3.00 + PLN 5.00 and its total fee is PLN 8.00 (Ride Priced).

### R3: Hourly fees rise for hours 1 to 3 and stay the same from the 4th hour on

- **E3.1** Given Anna's ride started at 10:00 and ended with a station return at Central at 13:15 (3 h after the free period), When the ride is priced, Then it has 3 billing hours, its hourly fees are PLN 3.00 + 5.00 + 7.00 and its total fee is PLN 15.00 (Ride Priced).
- **E3.2** Given Anna's ride started at 10:00 and ended with a station return at Central at 14:20 (4 h 5 min after the free period), When the ride is priced, Then it has 5 billing hours, its hourly fees are PLN 3.00 + 5.00 + 7.00 + 9.00 + 9.00 and its total fee is PLN 33.00 (Ride Priced).

### R4: A ride is priced with the tariff that was valid when it started (vision)

Given the launch tariff and tariff T-2, valid from 2026-11-09 12:00, with hour 1 at PLN 4.00 and every other price as in the launch tariff:

- **E4.1** Given Ben's ride started at 11:50 and ended with a station return at Central at 12:30, When the ride is priced, Then it is priced with the launch tariff and its total fee is PLN 3.00 (Ride Priced).
- **E4.2** Given Ben's ride started at 12:05 and ended with a station return at Central at 12:40, When the ride is priced, Then it is priced with T-2 and its total fee is PLN 4.00 (Ride Priced).

### R5: A station return and a lock failure pay hourly fees only (DE-32, DE-34)

- **E5.1** Given Anna's ride started at 10:00 and ended with a station return at Old Town at 10:30, When the ride is priced, Then its return fee is PLN 0.00 and its total fee is PLN 3.00 (Ride Priced).
- **E5.2** Given Anna's ride started at 10:00 and ended in a lock failure at 10:30 at position (52.2500, 21.1000), outside every drop zone, When the ride is priced, Then its return fee is PLN 0.00, there is no penalty and its total fee is PLN 3.00 (Ride Priced) (DE-23).

## Contract

### Commands

| Command | Issued by | Input | On success | Rejected when |
|---------|-----------|-------|------------|---------------|
| Price Ride | Finance, whenever a rental session ended (Rental Session Ended) | ride, rider, started at, ended at, ending, station or position | Ride Priced | none |

### Queries

| Query | Asked by | Input | Result |
|-------|----------|-------|--------|
| None. | | | |

### Events

| Event | When | Data |
|-------|------|------|
| Ride Priced | a finished ride's total fee is calculated | ride, rider, tariff, billing hours, hourly fees, return fee, penalty, total fee, priced at |

## Data

| Shape | Field | Type | Constraint |
|-------|-------|------|------------|
| Ride | started at, ended at | Timestamp | ended at after started at |
| Ride | ending | one of {station return, free-floating return, lock failure, maximum rental time exceeded} | required |
| Ride price | tariff | Identifier (Tariff) | the tariff valid at started at (R4) |
| Ride price | billing hours | Number | 0 when the ride lasts at most the free period, else every started hour after it (R1, R2) |
| Ride price | hourly fees | list of Money | one per billing hour |
| Ride price | return fee | Money ≥ 0 | PLN 0.00 for station return and lock failure (R5) |
| Ride price | penalty | Money ≥ 0 | PLN 0.00 unless the maximum rental time was exceeded |
| Ride price | total fee | Money ≥ 0 | hourly fees + return fee + penalty |

## Out of scope

- The drop zone fee and the unauthorized zone return fee (`finance-prices-free-floating-return-by-zone`).
- The penalty for exceeding the maximum rental time (`finance-penalizes-overdue-rental`).
- Publishing new prices (`pricing-manager-publishes-tariff`).
- Charging the fee (`finance-charges-finished-ride`).

## Open questions

- ~~**Q1** DE-31 says "15:01 starts hour 1": minutes or seconds?~~ Seconds: billing time is measured to the second; 15 min 0 s is free, 15 min 1 s starts hour 1 (role-played domain expert, DE-83, 2026-11-08)
- ~~**Q2** Is an hour's fee applied only when the hour completes (vision "Billing"), or for every started hour?~~ Every started billing hour is charged; 15:00 is free, 75:00 is hour 1 only (role-played domain expert, DE-31, 2026-11-08)
- ~~**Q3** What are the launch prices?~~ Free period 15 min; PLN 3.00, 5.00, 7.00, then 9.00 per hour (role-played domain expert, DE-30, 2026-11-08)
- ~~**Q4** What is charged when the lock fails?~~ Hourly fees up to the failed lock, nothing extra (role-played domain expert, DE-32, 2026-11-08)
- ~~**Q5** Does a station return pay a return fee?~~ No, only free-floating returns do (role-played domain expert, DE-34, 2026-11-08)

## Notes for the plan

- None.
