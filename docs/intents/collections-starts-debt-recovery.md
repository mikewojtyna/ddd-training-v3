---
name: collections-starts-debt-recovery
sequence: 21
title: Collections wants debt recovery to start as soon as a rental fee stays unpaid
system: velocity
actor: Collections
activity: Recover unpaid fees
task: Start debt recovery
status: planned
date: 2026-11-08
supersedes:
superseded_by:
source: docs/domains/velocity/velocity-vision.md "Account Blocks & Debt Recovery", "Key Business Rules to Know"; docs/bounded-contexts/velocity/domain-expert-answers.md DE-40, DE-44
---

# Collections wants debt recovery to start as soon as a rental fee stays unpaid

## Intent

As a **Collections** team, I want **debt recovery to start on its own the moment a rider's rental fee stays unpaid** so that **owed money is actively pursued from day one and nobody has to remember to chase it**.

## Why

The vision says the quick, silent charge retries after a ride are not enough: once a fee still can't be collected it becomes outstanding debt, the account is blocked, and "the debt recovery process begins". "Debt recovery is initiated automatically" ("Account Blocks & Debt Recovery"; "Debt collection runs automatically, but can be paused" in "Key Business Rules to Know"). Without an automatic start, unpaid fees would sit on blocked accounts with nobody pursuing them.

## Business outcome

- Recovery Started: a rider's outstanding debt is being actively pursued, with the first collection attempt due at once.

## Success signal

- (suggested) No rider has outstanding debt for more than a day without a running or paused recovery.
- (suggested) Share of unpaid fees that are eventually recovered.

## Business rules mentioned

- "Debt recovery is initiated automatically" when the block is due to owed money (vision).
- Recovery starts when a rental fee is unpaid; the first collection attempt happens when recovery starts (DE-40).
- A second unpaid fee while recovery runs joins the running recovery; the debt grows and the attempt count isn't reset (DE-44).

## Scope

- **In:** starting a recovery for a rider whose rental fee stays unpaid; adding a further unpaid fee to a rider's open recovery.
- **Out:** the charge retries that come before (`finance-retries-failed-charge`); blocking the account (`finance-retries-failed-charge`); making collection attempts and ending recovery (`collections-attempts-debt-collection`); pausing and resuming (`collections-agent-pauses-debt-recovery`, `collections-agent-resumes-debt-recovery`); the rider paying the debt themselves (`rider-pays-off-outstanding-debt`); handing debt to an external agency (DE-41: out of scope).

## Related intents

- follows: `finance-retries-failed-charge`
- enables: `collections-attempts-debt-collection`
- enables: `collections-agent-pauses-debt-recovery`

## Open questions

- Does a further unpaid fee also join a recovery that is paused, or only one that is running? (DE-44 speaks of a running recovery.)
- After a recovery ended (succeeded or exhausted), does a new unpaid fee start a new recovery?

## Notes for the spec

- Accepted under the trainer's standing instruction of 2026-11-08 (autonomous run), to be reviewed in the PR.
