---
name: pricing-manager-publishes-tariff
intent: pricing-manager-publishes-tariff
system: velocity
status: approved
date: 2026-11-08
---

# Spec: A pricing manager wants to publish a new tariff

Concrete rules, examples and contract for [the intent](../intents/pricing-manager-publishes-tariff.md).

## Vocabulary

| Term | Meaning |
|------|---------|
| Tariff | A complete set of prices valid from a moment on: free period, fees of hour 1, 2 and 3, fee of every further hour, drop zone fee, unauthorized zone return fee, penalty. |
| Valid from | The moment from which a tariff applies to newly started rides. |
| Launch tariff | Valid from launch: 15 min free; PLN 3.00, 5.00, 7.00, then 9.00 per hour; drop zone fee PLN 2.00; unauthorized zone return fee PLN 40.00; penalty PLN 150.00 (DE-30). |
| Tariff valid at a moment | The published tariff with the latest valid from that isn't after that moment. |

## Rules

The launch tariff is published. Tariff T-2, used below: free period 15 min; hour 1 PLN 3.50; hour 2 PLN 5.50; hour 3 PLN 7.50; every further hour PLN 9.50; drop zone fee PLN 2.50; unauthorized zone return fee PLN 45.00; penalty PLN 150.00 (example prices chosen by the pricing manager).

### R1: A pricing manager can publish a complete tariff valid from a future moment

- **E1.1** Given it is 2026-11-08 10:00, When pricing manager Paula publishes T-2 valid from 2026-12-01 00:00, Then T-2 is published and is listed among the tariffs with its valid from (Tariff Published).

### R2: A ride is priced with the tariff valid when it started, so a new tariff never changes a started ride (vision)

Given T-2 was published valid from 2026-12-01 00:00:

- **E2.1** Given Anna's ride started on 2026-11-30 at 23:50 and ended with a station return at Central on 2026-12-01 at 00:30, When the ride is priced, Then the launch tariff applies and its total fee is PLN 3.00.
- **E2.2** Given Ben's ride started on 2026-12-01 at 00:05 and ended with a station return at Central at 00:40, When the ride is priced, Then T-2 applies and its total fee is PLN 3.50.

### R3: A tariff's valid from must lie in the future when it is published (vision, DE-96)

- **E3.1** Given it is 2026-11-08 10:00 and Anna's ride started at 09:30, When Paula publishes tariff T-3 valid from 2026-11-08 09:00, Then the publication is rejected and Anna's ride keeps the launch tariff.
- **E3.2** Given it is 2026-11-08 10:00, When Paula publishes tariff T-3 valid from 2026-11-08 10:00, Then the publication is rejected, because its valid from isn't in the future (DE-96).

### R4: A tariff states every price (vision, DE-30)

- **E4.1** Given it is 2026-11-08 10:00, When Paula publishes tariff T-4 valid from 2026-12-01 00:00 with every price of T-2 except the penalty, Then the publication is rejected.
- **E4.2** Given it is 2026-11-08 10:00, When Paula publishes tariff T-4 valid from 2026-12-01 00:00 with every price of T-2 but a drop zone fee of PLN -1.00, Then the publication is rejected, because fees can't be negative (DE-96).

### R5: Two tariffs can't share the same valid from (DE-96)

- **E5.1** Given T-2 was published valid from 2026-12-01 00:00, When on 2026-11-08 at 11:00 Paula publishes tariff T-5 valid from 2026-12-01 00:00, Then the publication is rejected and T-2 stays the tariff valid from that moment.

## Contract

### Commands

| Command | Issued by | Input | On success | Rejected when |
|---------|-----------|-------|------------|---------------|
| Publish Tariff | Pricing Manager | tariff, valid from, free period, hour 1 fee, hour 2 fee, hour 3 fee, further hour fee, drop zone fee, unauthorized zone return fee, penalty | Tariff Published | R3, R4, R5 |

### Queries

| Query | Asked by | Input | Result |
|-------|----------|-------|--------|
| Tariffs | Pricing Manager | none | every published tariff with its valid from and prices |

### Events

| Event | When | Data |
|-------|------|------|
| Tariff Published | a tariff is accepted for publication | tariff, valid from, free period, hour 1 fee, hour 2 fee, hour 3 fee, further hour fee, drop zone fee, unauthorized zone return fee, penalty, published by, published at |

## Data

| Shape | Field | Type | Constraint |
|-------|-------|------|------------|
| Tariff | tariff | Identifier (Tariff) | unique; never withdrawn once published (DE-96) |
| Tariff | valid from | Timestamp | after the moment of publication (R3); no other tariff with the same valid from (R5) |
| Tariff | free period | Duration | required, ≥ 0 |
| Tariff | hour 1 fee, hour 2 fee, hour 3 fee, further hour fee | Money | required, ≥ PLN 0.00 |
| Tariff | drop zone fee, unauthorized zone return fee | Money | required, ≥ PLN 0.00 (DE-96, Q4) |
| Tariff | penalty | Money | required, ≥ PLN 0.00 |

## Out of scope

- Pricing a ride with the tariff (`finance-prices-ride-by-tiered-tariff`, `finance-prices-free-floating-return-by-zone`, `finance-penalizes-overdue-rental`).
- Withdrawing a tariff: published tariffs aren't withdrawn (DE-96).
- Changing the city's zone map.

## Open questions

- ~~**Q1** Can a published tariff be withdrawn or corrected before it becomes valid?~~ No, published tariffs aren't withdrawn (role-played domain expert, DE-96, 2026-11-08)
- ~~**Q2** May a tariff become valid at the very moment it is published?~~ No, valid from must lie in the future (role-played domain expert, DE-96, 2026-11-08)
- ~~**Q3** What happens when a second tariff has the same valid from as an earlier one?~~ It is rejected (role-played domain expert, DE-96, 2026-11-08)
- **Q4** Must the unauthorized zone return fee be higher than the drop zone fee? The vision calls one "small, but real" and the other "much higher". (R4, Data)
- ~~**Q5** What are the launch prices?~~ As in the vocabulary (role-played domain expert, DE-30, 2026-11-08)

## Notes for the plan

- None.
