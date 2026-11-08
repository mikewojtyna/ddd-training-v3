---
name: collections-agent-resumes-debt-recovery
sequence: 24
title: A collections agent wants to resume a paused debt recovery
system: velocity
actor: Collections Agent
activity: Recover unpaid fees
task: Resume debt recovery
status: specified
date: 2026-11-08
supersedes:
superseded_by:
source: docs/domains/velocity/velocity-vision.md "Account Blocks & Debt Recovery"; docs/bounded-contexts/velocity/domain-expert-answers.md DE-41, DE-42
---

# A collections agent wants to resume a paused debt recovery

## Intent

As a **Collections Agent**, I want **to resume a paused debt recovery once the reason for the pause is gone** so that **the pursuit of the money continues where it stopped and eventually reaches success or failure**.

## Why

"Once the pause is lifted, recovery resumes and eventually reaches success or failure" ("Account Blocks & Debt Recovery"). Without resuming, a paused recovery would be a quiet way of abandoning the debt, which the vision explicitly wants to avoid.

## Business outcome

- Recovery Resumed: collection attempts continue for the rider, the next one at once.

## Success signal

- (suggested) No recovery stays paused after its dispute, payment-plan talk or regulatory hold has ended.

## Business rules mentioned

- "On resume the next attempt is due at once; the failed-attempt count is kept." (DE-42)
- Recovery is exhausted after 5 failed collection attempts (DE-41).

## Scope

- **In:** resuming a paused recovery; the next attempt being due at once; the failed-attempt count carried over.
- **Out:** pausing (`collections-agent-pauses-debt-recovery`); making the attempts (`collections-attempts-debt-collection`).

## Related intents

- builds on: `collections-agent-pauses-debt-recovery`
- enables: `collections-attempts-debt-collection`

## Open questions

- Can any collections agent resume a recovery, or only the one who paused it?

## Notes for the spec

- Accepted under the trainer's standing instruction of 2026-11-08 (autonomous run), to be reviewed in the PR.
