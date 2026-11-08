---
name: finance-penalizes-overdue-rental
intent: finance-penalizes-overdue-rental
system: velocity
status: approved
date: 2026-11-08
---

# Spec: Finance wants a penalty charged for a rental that exceeded the maximum rental time

Concrete rules, examples and contract for [the intent](../intents/finance-penalizes-overdue-rental.md).

## Vocabulary

| Term | Meaning |
|------|---------|
| Maximum rental time | 12 hours. A ride of exactly 12:00:00 is still a ride; once it runs longer it is ended automatically, and it ends at the 12-hour mark (started + 12 h) whenever that is noticed (DE-20, DE-82). |
| Overdue rental | A ride ended automatically because it exceeded the maximum rental time; its ending is *maximum rental time exceeded*. |
| Penalty | The price for crossing the network's rule, shown apart from the hourly fees; launch tariff PLN 150.00 (DE-30). |
| Hourly fees | As in `finance-prices-ride-by-tiered-tariff`: 15 min free, then every started billing hour at PLN 3.00, 5.00, 7.00, then 9.00 (DE-30, DE-31). For 12 hours: 11 h 45 min after the free period = 12 billing hours = 3 + 5 + 7 + 9 × 9 = PLN 96.00. |

## Rules

All examples on 2026-11-09 with the launch tariff unless stated otherwise.

### R1: An overdue rental pays the hourly fees for the full 12 hours plus the penalty (DE-33)

- **E1.1** Given Ben's ride on bike B-17 started at 08:00:00, ran past 20:00:00 and was ended automatically at the 12-hour mark, 20:00:00, for exceeding the maximum rental time, When the ride is priced, Then its hourly fees are PLN 96.00 (12 billing hours), its penalty is PLN 150.00 and its total fee is PLN 246.00 (Ride Priced).
- **E1.2** Given Ben's ride started at 08:00:00 and the overrun was noticed only at 20:20, so the ride was ended automatically at the 12-hour mark, 20:00:00, When the ride is priced, Then its hourly fees are PLN 96.00 for the 12 hours (not PLN 105.00 for 13 billing hours up to 20:20), its penalty is PLN 150.00 and its total fee is PLN 246.00 (Ride Priced) (DE-82).

### R2: An overdue rental pays no return fee, because the bike wasn't returned anywhere (DE-33)

- **E2.1** Given Ben's ride started at 08:00:00 and was ended automatically at the 12-hour mark, 20:00:00, and B-17 last signalled its position at (52.2500, 21.1000), outside every drop zone, When the ride is priced, Then its return fee is PLN 0.00 and its total fee is PLN 246.00 (Ride Priced).

### R3: A ride of exactly the maximum rental time is not overdue (DE-20)

- **E3.1** Given Anna's ride on bike B-22 started at 08:00:00 and ended with a station return at Central at 20:00:00, When the ride is priced, Then its hourly fees are PLN 96.00, there is no penalty and its total fee is PLN 96.00 (Ride Priced).

### R4: The penalty comes from the tariff valid when the ride started (vision)

- **E4.1** Given tariff T-2, valid from 2026-11-09 12:00, with a penalty of PLN 200.00 and every other price as in the launch tariff, and Ben's ride started at 08:00:00 and was ended automatically at the 12-hour mark, 20:00:00, When the ride is priced, Then its penalty is PLN 150.00 (launch tariff) and its total fee is PLN 246.00 (Ride Priced).

## Contract

### Commands

| Command | Issued by | Input | On success | Rejected when |
|---------|-----------|-------|------------|---------------|
| Price Ride | Finance, whenever a rental session ended (Rental Session Ended) | ride, rider, started at, ended at, ending | Ride Priced | none |

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
| Ride price | billing hours | Number | exactly 12 for an overdue rental, which ends at the 12-hour mark (R1, DE-82) |
| Ride price | penalty | Money ≥ 0 | the tariff's penalty when the ending is *maximum rental time exceeded*, else PLN 0.00 |
| Ride price | return fee | Money ≥ 0 | PLN 0.00 for an overdue rental (R2) |
| Ride price | total fee | Money ≥ 0 | hourly fees + return fee + penalty |

## Out of scope

- Ending the overdue rental and withdrawing the bike as not returned (`fleet-operations-ends-overdue-rental`).
- The hourly fees themselves (`finance-prices-ride-by-tiered-tariff`).
- Charging the fee (`finance-charges-finished-ride`).

## Open questions

- ~~**Q1** If an overdue rental is noticed some minutes after the 12 hours, when does it end and do the hourly fees stop at 12 hours?~~ It ends at the 12-hour mark whenever it is noticed; hourly fees count up to that mark, plus the penalty (role-played domain expert, DE-82, 2026-11-08)
- ~~**Q2** What is the maximum rental time, and is a ride of exactly that length overdue?~~ 12 hours; at 12:00:00 it is still a ride (role-played domain expert, DE-20, 2026-11-08)
- ~~**Q3** What does an overdue rental pay?~~ The hourly fees for the full 12 hours plus the penalty, no return fee (role-played domain expert, DE-33, 2026-11-08)
- ~~**Q4** How high is the penalty?~~ PLN 150.00 in the launch tariff (role-played domain expert, DE-30, 2026-11-08)

## Notes for the plan

- None.
