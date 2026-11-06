---
name: crowdsorcery-investor-withdraws-money
sequence: 4
title: An investor wants to withdraw money from their account
system: crowdsorcery
actor: Investor
activity: Manage my money
task: Withdraw money
status: accepted
date: 2026-11-03
supersedes:
superseded_by:
source: product ticket CS-142
---

# An investor wants to withdraw money from their account

## Intent

As an **Investor**, I want to **withdraw money from my CrowdSorcery account to my bank account** so that **I can use money I'm not investing**.

## Why

Investors can deposit but can't take money out; support handles withdrawals by email (CS-142).

## Business outcome

- Withdrawal Requested: the investor asked to take an amount out of their account.
- Money Paid Out: the amount reached the investor's bank account.

## Success signal

- (suggested) No more withdrawal requests by email to support.

## Business rules mentioned

- "Payouts go only to the investor's verified bank account."
- "You can't withdraw more than your available balance."

## Scope

- **In:** requesting a withdrawal and the payout to the bank.
- **Out:** depositing money (`crowdsorcery-investor-makes-deposit`, candidate).

## Related intents

- None.

## Open questions

- None.

## Notes for the spec

- Ticket suggests a `POST /api/withdrawals` endpoint, a `withdrawals` table in Postgres and a `WithdrawalRequested` Kafka event.
