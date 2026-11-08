---
name: collections-attempts-debt-collection
sequence: 22
title: Collections wants to remind riders and try to collect their debt on a schedule
system: velocity
actor: Collections
activity: Recover unpaid fees
task: Attempt a collection
status: delivered
date: 2026-11-08
supersedes:
superseded_by:
source: docs/domains/velocity/velocity-vision.md "Account Blocks & Debt Recovery"; docs/bounded-contexts/velocity/domain-expert-answers.md DE-40, DE-41, DE-45
---

# Collections wants to remind riders and try to collect their debt on a schedule

## Intent

As a **Collections** team, I want **a rider in debt recovery to be reminded and asked to pay their whole outstanding debt at regular intervals until it is paid or we give up** so that **owed money is recovered without endless chasing of riders who will never pay**.

## Why

The vision describes debt recovery as "an active pursuit of the money: reminding the rider, asking them to pay or update their card", which can **succeed** ("the rider pays and the account is unblocked") or **fail** ("recovery is exhausted, all attempts have been tried and rejected") ("Account Blocks & Debt Recovery"). The role-played domain expert fixed the rhythm and the limit (DE-40, DE-41).

## Business outcome

- Collection Attempted: the rider was reminded and the whole outstanding debt was asked for.
- Recovery Succeeded: the rider's outstanding debt was cleared and the pursuit ended.
- Recovery Exhausted: every allowed attempt failed; the pursuit ended without the money.

## Success signal

- (suggested) Share of recoveries that succeed.
- (suggested) Average number of days from the start of recovery to success.

## Business rules mentioned

- "A collection attempt sends the rider a reminder and tries to collect the whole outstanding debt. The first attempt happens when recovery starts, then every 3 days." (DE-40)
- "After 5 failed collection attempts" recovery is exhausted; the rider stays blocked and the debt stays outstanding (DE-41).
- A clearance after exhaustion still counts: the recovery is recorded as succeeded. A second clearance changes nothing (DE-45).

## Scope

- **In:** making due collection attempts; ending recovery as succeeded when the debt is cleared (by an attempt or by the rider paying); ending it as exhausted after the last failed attempt.
- **Out:** starting recovery (`collections-starts-debt-recovery`); pausing and resuming (`collections-agent-pauses-debt-recovery`, `collections-agent-resumes-debt-recovery`); charging the card and unblocking the account (`rider-pays-off-outstanding-debt`, `finance-retries-failed-charge`); handing exhausted debt to an external agency (DE-41: out of scope); payment-plan instalments (DE-43: out of scope).

## Related intents

- builds on: `collections-starts-debt-recovery`
- follows: `rider-pays-off-outstanding-debt`

## Open questions

- Is "every 3 days" counted to the minute from the previous attempt, or by calendar day?

## Notes for the spec

- Accepted under the trainer's standing instruction of 2026-11-08 (autonomous run), to be reviewed in the PR.
