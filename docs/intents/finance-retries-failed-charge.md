---
name: finance-retries-failed-charge
sequence: 18
title: Finance wants a failed ride charge retried before it becomes outstanding debt
system: velocity
actor: Finance
activity: Pay for the ride
task: Retry failed charges
status: planned
date: 2026-11-08
supersedes:
superseded_by:
source: docs/domains/velocity/velocity-vision.md "Settling the Ride", "Account Blocks & Debt Recovery"; domain-expert answers DE-5, DE-6, DE-7, DE-35, DE-71
---

# Finance wants a failed ride charge retried before it becomes outstanding debt

## Intent

As a **Finance** team, I want **a failed ride charge retried a few times shortly after the ride, and the fee turned into outstanding debt only when all attempts failed** so that **a short card hiccup doesn't block a rider, while money that really can't be collected is never forgotten**.

## Why

"If a charge fails, we automatically try again a few times shortly after the ride. If the money still cannot be collected, the unpaid amount stays on the rider's account as outstanding debt, and the account is blocked." (vision, "Settling the Ride"). The quick retries are "silent charge attempts on the card", unlike debt recovery ("Account Blocks & Debt Recovery").

## Business outcome

- Ride Settled: a retry charged the fee.
- Charge Attempt Failed: a retry failed and another attempt is due.
- Rental Fee Unpaid: all attempts failed; the fee was added to the rider's outstanding debt.
- Account Blocked: the rider can't start new rentals because of outstanding debt.

## Success signal

- (suggested) A high share of rides whose first charge failed is settled by a retry.

## Business rules mentioned

- 3 charge attempts in total: right after the ride, then 5 minutes after each failed attempt; after the 3rd failure the fee becomes outstanding debt (DE-35).
- "The account is blocked" when the fee becomes outstanding debt (vision); the rider gets a block notification (vision, DE-7).
- A rider whose card was invalidated has no card to charge until they add a new one (DE-71); a newly verified card becomes the one charged (DE-5).

## Scope

- **In:** retrying failed charges on schedule; settling the ride when a retry succeeds; turning the fee into outstanding debt and blocking the rider after the last failure.
- **Out:** the first charge attempt (`finance-charges-finished-ride`); the rider paying the debt (`rider-pays-off-outstanding-debt`); debt recovery (`collections-starts-debt-recovery`); blocking for an invalid card (`finance-blocks-rider-with-invalid-card`).

## Related intents

- builds on: `finance-charges-finished-ride`
- enables: `rider-pays-off-outstanding-debt`
- enables: `collections-starts-debt-recovery`

## Open questions

- None blocking.

## Notes for the spec

- Accepted under the trainer's standing instruction of 2026-11-08 (autonomous run), to be reviewed in the PR.
