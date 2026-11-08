---
name: support-agent-resolves-incident
sequence: 29
title: A support agent wants to resolve an incident
system: velocity
actor: Support Agent
activity: Get help
task: Resolve an incident
status: specified
date: 2026-11-08
supersedes:
superseded_by:
source: docs/domains/velocity/velocity-vision.md "Customer Support & Incidents", "Key Business Rules to Know"; docs/bounded-contexts/velocity/domain-expert-answers.md DE-51, DE-53, DE-54, DE-56
---

# A support agent wants to resolve an incident

## Intent

As a **Support Agent**, I want **to resolve an incident I'm working on and record how it was resolved** so that **the rider's problem is closed with a clear outcome and nothing stays open forever**.

## Why

"Every incident, no matter how it was routed, must end in a resolved state. Nothing stays open forever." ("Customer Support & Incidents"); "Every incident has a terminal state. Accepted, rejected, escalated, or reopened: the flow always converges on resolved." ("Key Business Rules to Know"). An accepted incident is resolved directly or from the team it was escalated to; a reopened one is "eventually resolved".

## Business outcome

- Incident Resolved: the incident reached its terminal state, with the resolution on record.

## Success signal

- (suggested) Time from report to resolution, per incident type.

## Business rules mentioned

- An accepted incident is resolved directly by the agent, or escalated and "resolved from there" (vision); the assigned agent records the team's resolution (DE-53).
- A reopened incident must be resolved (DE-54); it stays with the agent who rejected it (DE-56).
- Only the assigned agent resolves (DE-51).

## Scope

- **In:** resolving an accepted, escalated or reopened incident by its assigned agent, with a resolution.
- **Out:** the automatic resolution of a fleet-issue incident when its bike is fixed (`technician-returns-fixed-bike-to-circulation`, DE-55); escalating (`support-agent-escalates-incident`); reopening (`rider-reopens-rejected-incident`).

## Related intents

- builds on: `support-agent-decides-on-incident`
- follows: `support-agent-escalates-incident`
- follows: `rider-reopens-rejected-incident`

## Open questions

- How does a rejected incident that is never reopened reach the resolved state the vision demands?

## Notes for the spec

- Accepted under the trainer's standing instruction of 2026-11-08 (autonomous run), to be reviewed in the PR.
