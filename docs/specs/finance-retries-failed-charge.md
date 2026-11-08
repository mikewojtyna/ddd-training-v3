---
name: finance-retries-failed-charge
intent: finance-retries-failed-charge
system: velocity
status: approved
date: 2026-11-08
---

# Spec: Finance wants a failed ride charge retried before it becomes outstanding debt

Concrete rules, examples and contract for [the intent](../intents/finance-retries-failed-charge.md).

## Vocabulary

| Term | Meaning |
|------|---------|
| Charge attempt | One try to take a ride's fee from the rider's current card. 3 attempts in total: right after the ride, then 5 minutes after each failed attempt (DE-35). |
| Current card | The rider's newest verified card (DE-5); none after it was invalidated, until a new one is verified (DE-71). |
| Rental fee unpaid | All 3 charge attempts for a ride failed; the fee is added to the rider's outstanding debt. |
| Outstanding debt | The sum the rider owes from unpaid rides. |
| Block for outstanding debt | The rider can't start new rentals until the debt is cleared; independent of a block for an invalid card (DE-6). |

## Rules

All examples on 2026-11-09.

### R1: A failed charge is retried 5 minutes after the failed attempt (DE-35)

- **E1.1** Given Ben's ride S-3 is priced at PLN 43.00 and attempt 1 failed at 10:30, When failed charges are retried at 10:35 and the payment processor accepts PLN 43.00 on Ben's current card, Then S-3 is settled (Ride Settled).
- **E1.2** Given attempt 1 for S-3 failed at 10:30, When failed charges are retried at 10:34, Then no attempt is made for S-3.
- **E1.3** Given Carla had no current card and attempt 1 for her ride S-4 (PLN 3.00) failed at 10:30, and at 10:33 Carla's new card ending 5555 was verified, When failed charges are retried at 10:35 and the payment processor accepts PLN 3.00, Then the card ending 5555 is charged and S-4 is settled (Ride Settled) (DE-5).

### R2: After the 3rd failed attempt the fee becomes outstanding debt and the rider is blocked (DE-35)

- **E2.1** Given attempt 1 for Ben's ride S-3 (PLN 43.00) failed at 10:30, When failed charges are retried at 10:35 and the charge fails again, Then this was attempt 2 of 3, the next attempt is due at 10:40 and Ben has no outstanding debt yet (Charge Attempt Failed).
- **E2.2** Given attempts 1 and 2 for S-3 failed at 10:30 and 10:35, and Ben owes nothing and isn't blocked, When failed charges are retried at 10:40 and the charge fails again, Then S-3's fee is unpaid, Ben's outstanding debt is PLN 43.00 (Rental Fee Unpaid), and Ben is blocked for outstanding debt and notified (Account Blocked) (DE-7).

### R3: A further unpaid fee adds to the rider's outstanding debt

- **E3.1** Given Ben owes PLN 43.00 from S-3 and has been blocked for outstanding debt since 10:40, and his ride S-5 started at 10:32 and ended at 10:50, priced at PLN 3.00, with attempts 1 and 2 failed at 10:50 and 10:55, When failed charges are retried at 11:00 and the charge fails again, Then S-5's fee is unpaid, Ben's outstanding debt is PLN 46.00 (Rental Fee Unpaid) and Ben stays blocked for outstanding debt without a new block notification (DE-94).

## Contract

### Commands

| Command | Issued by | Input | On success | Rejected when |
|---------|-----------|-------|------------|---------------|
| Retry Failed Charges | Finance, on a schedule | now | Ride Settled, Charge Attempt Failed, Rental Fee Unpaid, Account Blocked | none (each ride with a due retry ends in one of these, R1-R3) |

### Queries

| Query | Asked by | Input | Result |
|-------|----------|-------|--------|
| None. | | | |

### Events

| Event | When | Data |
|-------|------|------|
| Ride Settled | a retry charged the fee | ride, rider, fee, charged (yes/no), settled at |
| Charge Attempt Failed | attempt 1 or 2 for a ride failed | ride, rider, fee, attempt number, reason, failed at, next attempt due at |
| Rental Fee Unpaid | attempt 3 for a ride failed | rider, ride, unpaid amount, outstanding debt, unpaid at |
| Account Blocked | a rider not yet blocked for outstanding debt gets outstanding debt | rider, reason (outstanding debt), blocked at |

## Data

| Shape | Field | Type | Constraint |
|-------|-------|------|------------|
| Ride settlement | failed attempts | Number | 0..3; at 3 the ride is unpaid (DE-35) |
| Ride settlement | next attempt due at | Timestamp | last failed attempt + 5 minutes, while failed attempts < 3 |
| Rental fee unpaid | outstanding debt | Money | > PLN 0.00; previous outstanding debt + unpaid amount (R3) |

## Out of scope

- The first charge attempt (`finance-charges-finished-ride`).
- The rider paying the debt (`rider-pays-off-outstanding-debt`).
- Debt recovery (`collections-starts-debt-recovery`).
- Blocking a rider for an invalid card (`finance-blocks-rider-with-invalid-card`).

## Open questions

- ~~**Q1** When a rider already blocked for outstanding debt gets another unpaid fee, is a new block notification sent?~~ No, more debt while already blocked for debt sends no new notification (role-played domain expert, DE-94, 2026-11-08)
- ~~**Q2** How many charge attempts are made, and how far apart?~~ 3 in total: right after the ride, then 5 minutes after each failed attempt; after the 3rd failure the fee becomes outstanding debt (role-played domain expert, DE-35, 2026-11-08)
- ~~**Q3** Which card does a retry charge?~~ The rider's current verified card, so a card verified between attempts is used (role-played domain expert, DE-5 and DE-71, 2026-11-08)
- ~~**Q4** Who is notified of the block?~~ The rider, once per block placed (role-played domain expert, DE-7, 2026-11-08)

## Notes for the plan

- None.
