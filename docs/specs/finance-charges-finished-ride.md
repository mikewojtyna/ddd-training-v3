---
name: finance-charges-finished-ride
intent: finance-charges-finished-ride
system: velocity
status: approved
date: 2026-11-08
---

# Spec: Finance wants every finished ride charged to the rider's card

Concrete rules, examples and contract for [the intent](../intents/finance-charges-finished-ride.md).

## Vocabulary

| Term | Meaning |
|------|---------|
| Ride | A finished rental session with its total fee (Ride Priced). |
| Current card | The rider's newest verified card (DE-5); a rider whose current card was invalidated has none until they add a new one (DE-71). |
| Charge attempt | One try to take the ride's total fee from the rider's current card through the payment processor; it succeeds or fails. |
| Settled | The ride's fee was charged, or the fee was PLN 0.00 (DE-37). |

## Rules

All examples on 2026-11-09.

### R1: A finished ride is charged its total fee to the rider's current card right after the ride (DE-35)

- **E1.1** Given Anna's ride S-1 ended at 10:30 and was priced at PLN 5.00 (Ride Priced), and Anna's current card ends 4242, When S-1 is charged at 10:30 and the payment processor accepts PLN 5.00, Then S-1 is settled (Ride Settled).

### R2: A ride priced at PLN 0.00 is settled without charging the card (DE-37)

- **E2.1** Given Anna's ride S-2 started at 10:00, ended at 10:12 and was priced at PLN 0.00, When S-2 is charged, Then S-2 is settled at once and Anna's card isn't charged (Ride Settled).

### R3: A failed first charge leaves the ride unsettled, with the next attempt due 5 minutes later (DE-35)

- **E3.1** Given Ben's ride S-3 ended at 10:30 and was priced at PLN 43.00, and Ben's current card ends 0002, When S-3 is charged at 10:30 and the payment processor declines it, Then S-3 isn't settled, this was attempt 1 of 3 and the next attempt is due at 10:35 (Charge Attempt Failed).
- **E3.2** Given Carla's current card was invalidated and she has no current card, and her ride S-4 ended at 10:30 and was priced at PLN 3.00, When S-4 is charged at 10:30, Then the charge fails because Carla has no valid payment method, this was attempt 1 of 3 and the next attempt is due at 10:35 (Charge Attempt Failed) (DE-71).

### R4: A ride is charged at most once (DE-95)

- **E4.1** Given Anna's ride S-1 is settled, When the news that S-1 ended arrives a second time, Then Anna's card isn't charged again and S-1 stays settled.

## Contract

### Commands

| Command | Issued by | Input | On success | Rejected when |
|---------|-----------|-------|------------|---------------|
| Charge Ride | Finance, right after a ride is priced (Ride Priced) | ride, rider, total fee | Ride Settled, Charge Attempt Failed | none (a failed charge ends with Charge Attempt Failed, R3; an already settled ride is left as it is, R4) |

### Queries

| Query | Asked by | Input | Result |
|-------|----------|-------|--------|
| None. | | | |

### Events

| Event | When | Data |
|-------|------|------|
| Ride Priced | a finished ride's total fee is calculated (heard from `finance-prices-ride-by-tiered-tariff`) | ride, rider, total fee |
| Ride Settled | the fee was charged, or it was PLN 0.00 | ride, rider, fee, charged (yes/no), settled at |
| Charge Attempt Failed | a charge attempt for a ride failed | ride, rider, fee, attempt number, reason, failed at, next attempt due at |

## Data

| Shape | Field | Type | Constraint |
|-------|-------|------|------------|
| Ride settlement | ride | Identifier (Ride) | required; settled at most once (R4) |
| Ride settlement | fee | Money ≥ 0 | the ride's total fee |
| Ride settlement | state | one of {waiting for charge, settled, unpaid} | |
| Charge attempt | attempt number | Number | 1..3 (DE-35) |
| Charge attempt | reason | one of {declined, no valid payment method} | only for a failed attempt |
| Charge attempt | next attempt due at | Timestamp | failed at + 5 minutes, after attempt 1 or 2 (DE-35) |

## Out of scope

- Pricing the ride (`finance-prices-ride-by-tiered-tariff`).
- Retrying a failed charge and turning the fee into outstanding debt (`finance-retries-failed-charge`).
- Refunds after a billing dispute (not an intent yet).

## Open questions

- ~~**Q1** Is a ride ever charged twice, e.g. when the news that it ended arrives twice?~~ No, a ride is settled at most once; a repeated "session ended" fact is ignored (role-played domain expert, DE-95, 2026-11-08)
- ~~**Q2** When is the first charge attempt made, and how many follow?~~ Right after the ride; 3 attempts in total, 5 minutes apart (role-played domain expert, DE-35, 2026-11-08)
- ~~**Q3** Is a ride priced at PLN 0.00 charged?~~ No, it is settled at once without charging the card (role-played domain expert, DE-37, 2026-11-08)
- ~~**Q4** Which card is charged?~~ The rider's current verified card; with none, the charge fails (role-played domain expert, DE-71, 2026-11-08)

## Notes for the plan

- None.
