---
name: rider-reopens-rejected-incident
sequence: 28
title: A rider wants to reopen an incident that was rejected
system: velocity
actor: Rider
activity: Get help
task: Reopen a rejected incident
status: delivered
date: 2026-11-08
supersedes:
superseded_by:
source: docs/domains/velocity/velocity-vision.md "Customer Support & Incidents", "Domain Vocabulary" (Reopening); docs/bounded-contexts/velocity/domain-expert-answers.md DE-54, DE-56
---

# A rider wants to reopen an incident that was rejected

## Intent

As a **Rider**, I want **to push back on a rejected incident and have it reopened** so that **a rejection I disagree with isn't the end of my case and it still gets resolved**.

## Why

"If rejected, the case does not disappear. The rider can push back, and the incident may be reopened. A reopened incident is eventually resolved." ("Customer Support & Incidents"). Reopening is "reactivating a rejected incident so it can be worked again toward a resolution" ("Domain Vocabulary").

## Business outcome

- Incident Reopened: the rejected incident is back with its agent and has to be resolved.

## Success signal

- (suggested) Share of rejected incidents that get reopened (a high share hints at hasty rejections).

## Business rules mentioned

- A rider can push back for "14 days from the rejection" (DE-54).
- "The 14-day window counts calendar days: rejected on 1 November, reopening is possible until the end of 15 November." (DE-56)
- A reopened incident "stays assigned to the agent who rejected it" (DE-56) and "can't be rejected again; it must be resolved" (DE-54).

## Scope

- **In:** reopening a rejected incident within the window; who works it afterwards.
- **Out:** rejecting (`support-agent-decides-on-incident`); resolving the reopened incident (`support-agent-resolves-incident`).

## Related intents

- builds on: `support-agent-decides-on-incident`
- enables: `support-agent-resolves-incident`

## Open questions

- What happens to a rejected incident the rider never reopens? The vision says every incident ends resolved and "nothing stays open forever".

## Notes for the spec

- Accepted under the trainer's standing instruction of 2026-11-08 (autonomous run), to be reviewed in the PR.
