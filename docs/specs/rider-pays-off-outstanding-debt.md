---
name: rider-pays-off-outstanding-debt
intent: rider-pays-off-outstanding-debt
system: velocity
status: approved
date: 2026-11-08
---

# Spec: A rider wants to pay off their outstanding debt

Concrete rules, examples and contract for [the intent](../intents/rider-pays-off-outstanding-debt.md).

## Vocabulary

| Term | Meaning |
|------|---------|
| Outstanding debt | The sum the rider owes from rides whose fee stayed unpaid after all charge attempts. |
| Paying off | The rider asks to pay; the whole outstanding debt is charged to their current card in one charge (DE-36). |
| Current card | The rider's newest verified card; none after it was invalidated, until a new one is verified (DE-71). |
| Block for outstanding debt | Lifted only when the outstanding debt is cleared; independent of a block for an invalid card (DE-6). |

## Rules

All examples on 2026-11-09.

### R1: A rider pays off the whole outstanding debt in one charge, and the debt block is lifted (DE-36, DE-6)

- **E1.1** Given Anna owes PLN 43.00 from ride S-3, is blocked for outstanding debt and her current card ends 4242, When Anna pays off her debt at 18:00 and the payment processor accepts PLN 43.00, Then Anna owes PLN 0.00 (Outstanding Debt Cleared) and her block for outstanding debt is lifted (Account Unblocked).
- **E1.2** Given Ben owes PLN 46.00 from rides S-3 (PLN 43.00) and S-5 (PLN 3.00) and is blocked for outstanding debt, When Ben pays off his debt and the payment processor accepts PLN 46.00, Then one charge of PLN 46.00 is made, Ben owes PLN 0.00 (Outstanding Debt Cleared) and his block for outstanding debt is lifted (Account Unblocked).
- **E1.3** Given Ben owes PLN 43.00 from ride S-3, and attempt 1 for his ride S-5 (PLN 3.00) failed at 10:50, When Ben pays off his debt at 10:52 and the payment processor accepts PLN 43.00, Then only PLN 43.00 is charged, Ben owes PLN 0.00 (Outstanding Debt Cleared), his block for outstanding debt is lifted (Account Unblocked) and S-5 stays in its charge retries (DE-95).

### R2: A failed payment leaves the debt and the block as they were

- **E2.1** Given Anna owes PLN 43.00 and is blocked for outstanding debt, When Anna pays off her debt and the payment processor declines PLN 43.00, Then Anna still owes PLN 43.00 and stays blocked.
- **E2.2** Given Carla owes PLN 9.00, is blocked for outstanding debt and for an invalid card, and has no current card, When Carla pays off her debt, Then the payment fails, Carla still owes PLN 9.00 and both blocks stay (DE-6, DE-71).

### R3: Only a rider with outstanding debt can pay it off

- **E3.1** Given Eva owes PLN 0.00, When Eva pays off her debt, Then the payment is rejected and Eva's card isn't charged.

## Contract

### Commands

| Command | Issued by | Input | On success | Rejected when |
|---------|-----------|-------|------------|---------------|
| Pay Off Outstanding Debt | Rider | rider | Outstanding Debt Cleared, Account Unblocked | R3; a declined charge leaves the debt as it was (R2) |

### Queries

| Query | Asked by | Input | Result |
|-------|----------|-------|--------|
| Rider's Outstanding Debt | Rider | rider | outstanding debt, unpaid rides (ride, unpaid amount) |

### Events

| Event | When | Data |
|-------|------|------|
| Outstanding Debt Cleared | the rider's outstanding debt went down to PLN 0.00 | rider, amount paid, cleared at |
| Account Unblocked | the rider's block for outstanding debt is lifted | rider, reason (outstanding debt), eligible to rent now (yes/no), unblocked at |

## Data

| Shape | Field | Type | Constraint |
|-------|-------|------|------------|
| Debt payment | amount | Money | = the rider's whole outstanding debt, > PLN 0.00 (R1, R3) |
| Rider's outstanding debt | unpaid rides | list of (ride, unpaid amount) | sum = outstanding debt |

## Out of scope

- Collection attempts by debt recovery (`collections-attempts-debt-collection`).
- Paying in instalments (out of scope, DE-43).
- Adding a new card (`rider-adds-payment-method`) and lifting a block for an invalid card (`finance-blocks-rider-with-invalid-card`).

## Open questions

- ~~**Q1** Does a ride whose charge attempts are still running belong to what the rider pays off?~~ No, a payoff covers only the outstanding debt (role-played domain expert, DE-95, 2026-11-08)
- ~~**Q2** Can a rider pay their debt themselves, and can they pay only part of it?~~ Yes, from the app; the full outstanding debt is charged to their current card (role-played domain expert, DE-36, 2026-11-08)
- ~~**Q3** Does clearing the debt lift a block for an invalid card?~~ No, the two blocks are independent (role-played domain expert, DE-6, 2026-11-08)

## Notes for the plan

- None.
