---
name: support-agent-decides-on-incident
sequence: 26
title: A support agent wants to take on an incident and accept or reject it
system: velocity
actor: Support Agent
activity: Get help
task: Decide on an incident
status: delivered
date: 2026-11-08
supersedes:
superseded_by:
source: docs/domains/velocity/velocity-vision.md "Who Uses the System" (Support Agents), "Customer Support & Incidents"; docs/bounded-contexts/velocity/domain-expert-answers.md DE-51, DE-54
---

# A support agent wants to take on an incident and accept or reject it

## Intent

As a **Support Agent**, I want **to pick up a reported incident and decide whether to accept or reject it** so that **every rider's report has one clear owner and a clear answer instead of drifting between agents**.

## Why

Support agents "handle incidents reported by riders, decide whether to accept or reject each report" ("Who Uses the System"). The incident flow has the incident "assigned to a support agent" who "either accepts or rejects" it ("Customer Support & Incidents"). The role-played domain expert decided that agents pick incidents up themselves and only the assigned agent works it (DE-51). Picking up is folded into this intent: on its own it has no value for the rider.

## Business outcome

- Incident Picked Up: the incident is assigned to the support agent who will work it.
- Incident Accepted: the agent agrees the rider's report needs action.
- Incident Rejected: the agent turned the report down, with a reason the rider can push back on.

## Success signal

- (suggested) Time from report to decision.
- (suggested) No incident stays without an assigned agent for more than a working day.

## Business rules mentioned

- "A support agent picks up an open incident; it is then assigned to them. Only the assigned agent accepts, rejects, escalates or resolves it." (DE-51)
- "A reopened incident can't be rejected again; it must be resolved." (DE-54)

## Scope

- **In:** picking up an open incident; accepting or rejecting an incident assigned to oneself.
- **Out:** reporting (`rider-reports-incident`); escalating an accepted incident (`support-agent-escalates-incident`); resolving (`support-agent-resolves-incident`); reopening a rejected incident (`rider-reopens-rejected-incident`); handing an incident over to another agent (candidate).

## Related intents

- builds on: `rider-reports-incident`
- enables: `support-agent-escalates-incident`
- enables: `support-agent-resolves-incident`
- enables: `rider-reopens-rejected-incident`

## Open questions

- Can an incident be handed over to another agent (for example when the assigned agent is away)?
- Must a rejection always carry a reason for the rider?

## Notes for the spec

- Accepted under the trainer's standing instruction of 2026-11-08 (autonomous run), to be reviewed in the PR.
