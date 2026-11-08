---
name: support-agent-escalates-incident
sequence: 27
title: A support agent wants to escalate an accepted incident to a specialised team
system: velocity
actor: Support Agent
activity: Get help
task: Escalate an incident
status: planned
date: 2026-11-08
supersedes:
superseded_by:
source: docs/domains/velocity/velocity-vision.md "Customer Support & Incidents", "Domain Vocabulary" (Escalation); docs/bounded-contexts/velocity/domain-expert-answers.md DE-51, DE-53
---

# A support agent wants to escalate an accepted incident to a specialised team

## Intent

As a **Support Agent**, I want **to escalate an accepted incident to the billing, fleet or safety team** so that **problems I can't solve myself are handled by the people who can, without losing track of them**.

## Why

"If accepted, the agent either resolves it directly, or escalates it to a specialized team and it is resolved from there" ("Customer Support & Incidents"). Escalation is "handing an accepted incident to a specialized team for resolution" ("Domain Vocabulary"). The teams are billing, fleet and safety, and the assigned agent keeps the incident and later records the team's resolution (DE-53).

## Business outcome

- Incident Escalated: an accepted incident is in the hands of the billing, fleet or safety team.

## Success signal

- (suggested) Time from escalation to resolution, per team.

## Business rules mentioned

- Escalation is for accepted incidents (vision).
- Escalated to one of the teams billing, fleet or safety; the assigned agent records the team's resolution (DE-53).
- Only the assigned agent escalates (DE-51).

## Scope

- **In:** escalating an accepted incident to a team.
- **Out:** accepting (`support-agent-decides-on-incident`); recording the team's resolution (`support-agent-resolves-incident`); how the teams themselves work (not modelled).

## Related intents

- builds on: `support-agent-decides-on-incident`
- enables: `support-agent-resolves-incident`

## Open questions

- Is a safety concern escalated to the safety team automatically?
- Does the team have to match the incident's type (a billing dispute to the billing team)?

## Notes for the spec

- Accepted under the trainer's standing instruction of 2026-11-08 (autonomous run), to be reviewed in the PR.
