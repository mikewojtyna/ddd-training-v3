---
name: collections-agent-pauses-debt-recovery
sequence: 23
title: A collections agent wants to pause a rider's debt recovery
system: velocity
actor: Collections Agent
activity: Recover unpaid fees
task: Pause debt recovery
status: specified
date: 2026-11-08
supersedes:
superseded_by:
source: docs/domains/velocity/velocity-vision.md "Account Blocks & Debt Recovery", "Key Business Rules to Know"; docs/bounded-contexts/velocity/domain-expert-answers.md DE-42, DE-43
---

# A collections agent wants to pause a rider's debt recovery

## Intent

As a **Collections Agent**, I want **to pause a rider's debt recovery for a dispute, a payment plan being negotiated or a regulatory hold** so that **we stop chasing the rider while the case is legitimately on hold, without giving up on the money**.

## Why

"The ability to pause it is important: it lets the business handle disputes and exceptional cases without abandoning the process entirely" ("Account Blocks & Debt Recovery"). "Disputes, regulatory holds, and negotiated payment plans are legitimate reasons to pause without giving up on recovery" ("Key Business Rules to Know"). Payment plans are negotiated by phone; recovery only has to be paused meanwhile (DE-43).

## Business outcome

- Recovery Paused: no collection attempts are made for the rider until the recovery is resumed.

## Success signal

- (suggested) No reminders sent to riders whose dispute or payment-plan talk is ongoing.

## Business rules mentioned

- Recovery is paused "for a dispute, a payment plan being negotiated, or a regulatory hold. Nothing is attempted while paused." (DE-42; the vision lists these "for example".)
- The failed-attempt count is kept (DE-42).

## Scope

- **In:** pausing a running recovery with a reason; no collection attempts while paused.
- **Out:** resuming (`collections-agent-resumes-debt-recovery`); a debt cleared while paused (`collections-attempts-debt-collection`); billing disputes raised with support pausing recovery on their own (reference map: deferred, candidate); negotiating and tracking payment-plan instalments (DE-43: out of scope).

## Related intents

- builds on: `collections-starts-debt-recovery`
- enables: `collections-agent-resumes-debt-recovery`

## Open questions

- Is a pause limited in time, or does it last until an agent resumes?
- What happens when an agent pauses a recovery that is already paused, for another reason?

## Notes for the spec

- Accepted under the trainer's standing instruction of 2026-11-08 (autonomous run), to be reviewed in the PR.
