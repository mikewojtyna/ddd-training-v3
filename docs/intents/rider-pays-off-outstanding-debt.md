---
name: rider-pays-off-outstanding-debt
sequence: 19
title: A rider wants to pay off their outstanding debt
system: velocity
actor: Rider
activity: Pay for the ride
task: Pay off outstanding debt
status: specified
date: 2026-11-08
supersedes:
superseded_by:
source: docs/domains/velocity/velocity-vision.md "Account Blocks & Debt Recovery", "Key Business Rules to Know"; domain-expert answers DE-6, DE-36, DE-71
---

# A rider wants to pay off their outstanding debt

## Intent

As a **Rider**, I want to **pay off my outstanding debt myself** so that **my account is unblocked and I can ride again without waiting for debt recovery to reach me**.

## Why

A rider with outstanding debt is blocked and "stays blocked until the underlying reason is cleared" (vision, "Account Blocks & Debt Recovery"). Debt recovery asks the rider "to pay or update their card"; a rider who wants to ride again should be able to do it on their own, at once (DE-36).

## Business outcome

- Outstanding Debt Cleared: the rider owes VeloCity nothing anymore.
- Account Unblocked: the block for outstanding debt is lifted.

## Success signal

- (suggested) A share of blocked riders clears their debt themselves before the first collection attempt.

## Business rules mentioned

- The rider pays from the app; the full outstanding debt is charged to their current card (DE-36).
- "Blocks are lifted only when the reason behind them is cleared." (vision); a debt block is lifted only when the debt is cleared, and it never lifts a card block (DE-6).
- A rider whose card was invalidated has no card to charge (DE-71).

## Scope

- **In:** the rider paying the whole outstanding debt at once, the debt being cleared and the debt block lifted.
- **Out:** collection attempts by debt recovery (`collections-attempts-debt-collection`); paying in instalments (out of scope, DE-43); adding a new card (`rider-adds-payment-method`); lifting a card block (`finance-blocks-rider-with-invalid-card`).

## Related intents

- builds on: `finance-retries-failed-charge`
- alternative to: `collections-attempts-debt-collection`

## Open questions

- Can a rider pay only part of the debt? (DE-36 says the full debt is charged.)

## Notes for the spec

- Accepted under the trainer's standing instruction of 2026-11-08 (autonomous run), to be reviewed in the PR.
