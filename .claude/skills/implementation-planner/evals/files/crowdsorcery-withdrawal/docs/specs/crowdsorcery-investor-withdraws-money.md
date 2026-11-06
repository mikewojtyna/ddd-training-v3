---
name: crowdsorcery-investor-withdraws-money
intent: crowdsorcery-investor-withdraws-money
system: crowdsorcery
status: approved
date: 2026-11-06
---

# Spec: An investor wants to withdraw money from their account

Concrete rules, examples and contract for [the intent](../intents/crowdsorcery-investor-withdraws-money.md).

## Vocabulary

| Term | Meaning |
|------|---------|
| Available balance | Money in the investor's CrowdSorcery account that isn't invested and isn't held by a withdrawal still waiting to be paid out (Q3). |
| Withdrawal | An investor's request to move an amount from their CrowdSorcery account to their verified bank account. |
| Verified bank account | The investor's own bank account that has been verified and is the only allowed payout destination. |
| Payout | The transfer of a withdrawal's money to the investor's verified bank account. |
| Withdrawal fee | PLN 2.00 charged once per withdrawal (Finance). |
| Manual approval | A Finance team member's decision to let a withdrawal above PLN 10 000.00 be paid out, or to reject it. |

## Rules

### R1: An investor can withdraw money from their available balance to their verified bank account

- **E1.1** Given investor Anna has PLN 1 000.00 available and a verified bank account at mBank, When Anna withdraws PLN 300.00, Then the withdrawal is accepted (Withdrawal Requested) and the money is paid out to her mBank account (Money Paid Out; amount after the fee: see R6).

### R2: An investor can't withdraw more than their available balance

- **E2.1** Given Anna has PLN 1 000.00 available, When Anna withdraws PLN 1 200.00, Then the withdrawal is rejected and her available balance stays PLN 1 000.00.
- **E2.2** Given Anna has PLN 1 000.00 available, When Anna withdraws exactly PLN 1 000.00, Then ? (accepted if the fee comes out of the payout, rejected if the fee is charged on top, Q1).
- **E2.3** Given Anna has PLN 1 000.00 available and requested a PLN 700.00 withdrawal that isn't paid out yet, When Anna withdraws PLN 400.00, Then the withdrawal is rejected, because PLN 700.00 is already held (Q3).

### R3: Payouts go only to the investor's verified bank account

- **E3.1** Given investor Dawid has PLN 800.00 available and no verified bank account, When Dawid withdraws PLN 200.00, Then the withdrawal is rejected and nothing is paid out.

### R4: A withdrawal above PLN 10 000.00 is paid out only after a Finance team member approves it

- **E4.1** Given investor Ben has PLN 20 000.00 available and a verified bank account, When Ben withdraws exactly PLN 10 000.00, Then it is paid out without manual approval, because PLN 10 000.00 isn't above the limit (Money Paid Out; whether the fee counts towards the limit: Q4).
- **E4.2** Given Ben has PLN 20 000.00 available and a verified bank account, When Ben withdraws PLN 12 000.00, Then the withdrawal is accepted (Withdrawal Requested), waits for manual approval and nothing is paid out yet.
- **E4.3** Given Ben's PLN 12 000.00 withdrawal waits for manual approval, When Carla from Finance approves it, Then it is approved (Withdrawal Approved) and the money is paid out to Ben's bank account (Money Paid Out).

### R5: A withdrawal that Finance rejects is never paid out

- **E5.1** Given Ben's PLN 12 000.00 withdrawal waits for manual approval, When Carla from Finance rejects it, Then nothing is paid out (Withdrawal Rejected) and Ben's available balance is ? (back to PLN 20 000.00, or reduced by the fee, Q2).

### R6: Every withdrawal costs a PLN 2.00 fee

- **E6.1** Given Anna has PLN 1 000.00 available, When Anna withdraws PLN 300.00 and it is paid out, Then ? (PLN 298.00 reaches her bank and her balance drops to PLN 700.00, or PLN 300.00 reaches her bank and her balance drops to PLN 698.00, Q1).

## Contract

### Commands

| Command | Issued by | Input | On success | Rejected when |
|---------|-----------|-------|------------|---------------|
| Withdraw Money | Investor | investor, amount | Withdrawal Requested | R2, R3 |
| Approve Withdrawal | Finance | withdrawal, approver | Withdrawal Approved | |
| Reject Withdrawal | Finance | withdrawal, approver, reason (Q6) | Withdrawal Rejected | |

### Queries

| Query | Asked by | Input | Result |
|-------|----------|-------|--------|
| Investor's Available Balance | Investor | investor | available balance |
| Withdrawals Awaiting Approval | Finance | none | list of withdrawal (investor, amount, requested at) |

### Events

| Event | When | Data |
|-------|------|------|
| Withdrawal Requested | an investor's withdrawal is accepted | withdrawal, investor, amount, fee, needs manual approval (Yes/No), requested at |
| Withdrawal Approved | a Finance team member approves a withdrawal above PLN 10 000.00 | withdrawal, approver, approved at |
| Withdrawal Rejected | a Finance team member rejects a withdrawal above PLN 10 000.00 | withdrawal, approver, reason (Q6), rejected at |
| Money Paid Out | the withdrawal's money reached the investor's verified bank account | withdrawal, investor, amount paid out, bank account, paid out at |

## Data

| Shape | Field | Type | Constraint |
|-------|-------|------|------------|
| Withdrawal | investor | Identifier (Investor) | required; has a verified bank account (R3) |
| Withdrawal | amount | Money (PLN, Q5) | > 0; ≤ available balance (R2, Q1) |
| Withdrawal | fee | Money (PLN) | PLN 2.00 (R6) |
| Withdrawal | needs manual approval | Yes/No | Yes when amount > PLN 10 000.00 (R4, Q4) |
| Withdrawal | state | one of {awaiting approval, approved, rejected, paid out} | awaiting approval only when needs manual approval |
| Money Paid Out | amount paid out | Money (PLN) | amount, or amount − fee (Q1) |

## Out of scope

- Depositing money (`crowdsorcery-investor-makes-deposit`, candidate).
- Verifying an investor's bank account (how an account becomes verified).
- A payout that fails or is returned by the bank.
- Cancelling a withdrawal by the investor.

## Open questions

- **Q1** Is the PLN 2.00 fee deducted from the amount paid out (withdraw PLN 300.00, receive PLN 298.00) or charged on top of it (withdraw PLN 300.00, PLN 302.00 leaves the balance)? This decides whether an investor can withdraw their whole available balance. (E2.2, E6.1, Data)
- **Q2** When Finance rejects a withdrawal, does the investor get the whole amount back to their available balance, and is the fee still charged? (E5.1)
- **Q3** Does a withdrawal that isn't paid out yet (including one waiting for manual approval) already reduce the available balance? The current deposit-account behaviour takes the money off the balance at once. (Vocabulary, E2.3)
- **Q4** Is the PLN 10 000.00 limit compared with the amount requested or with the amount including the fee? And is it per single withdrawal, or do several smaller withdrawals in a short time (e.g. two of PLN 6 000.00 on the same day) add up? (R4, E4.1)
- **Q5** Are all CrowdSorcery accounts held in PLN? Finance gave the limit and the fee in PLN, but the existing deposit accounts default to USD and the vision names no currency. If accounts can be in other currencies, how do the limit and fee apply? (Data)
- **Q6** Must Finance give a reason when rejecting, and does the investor see it? Is there a time limit for Finance to decide? (R5, Reject Withdrawal)
- **Q7** Can an investor have more than one verified bank account, so they must choose where the payout goes? (R3, Withdraw Money input)

## Notes for the plan

- Ticket CS-142 suggests a `POST /api/withdrawals` endpoint, a `withdrawals` table in Postgres and a `WithdrawalRequested` Kafka event. These are suggestions from the ticket, not decisions; the planner decides transport and storage after placing the intent on the context map.
- Existing code: the `deposit` context already has `WithdrawUseCase`, `DepositAccount.withdraw(Money)` (rejects when the balance is lower than the amount, deducts immediately, emits an internal `WithdrawalMade`), a stub `WithdrawRestApi`/`WithdrawRequest`, and accounts default to `CurrencyUnit.USD`. No fee, no manual approval, no payout, no bank account verification yet.
