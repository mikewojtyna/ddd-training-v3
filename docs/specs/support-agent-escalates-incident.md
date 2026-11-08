---
name: support-agent-escalates-incident
intent: support-agent-escalates-incident
system: velocity
status: approved
date: 2026-11-08
---

# Spec: A support agent wants to escalate an accepted incident to a specialised team

Concrete rules, examples and contract for [the intent](../intents/support-agent-escalates-incident.md).

## Vocabulary

| Term | Meaning |
|------|---------|
| Accepted | The assigned agent agreed the report needs action. |
| Escalation | Handing an accepted incident to a specialised team for resolution (vision, "Domain Vocabulary"). |
| Team | One of *billing*, *fleet* or *safety* (DE-53). |
| Assigned agent | The support agent who picked the incident up; they keep it after escalating and later record the team's resolution (DE-51, DE-53). |

## Rules

### R1: The assigned agent can escalate an accepted incident to a team

- **E1.1** Given Anna's incident I-2 (*billing dispute*, "charged twice for one ride") was accepted by support agent Zoe, When Zoe escalates I-2 to the *billing* team at 10:00 on 2026-11-09, Then I-2 is escalated to *billing* and stays assigned to Zoe (Incident Escalated).

### R2: Only an accepted incident can be escalated, and at most once

- **E2.1** Given Ben's incident I-6 was rejected by Zoe, When Zoe escalates I-6 to the *billing* team, Then the escalation is rejected and I-6 stays rejected.
- **E2.2** Given Ben's incident I-4 (*safety concern*) is assigned to Zoe and not yet decided, When Zoe escalates I-4 to the *safety* team, Then the escalation is rejected and I-4 stays undecided.
- **E2.3** Given I-2 is escalated to the *billing* team, When Zoe escalates I-2 to the *fleet* team, Then the escalation is rejected and I-2 stays with the *billing* team (DE-97).

### R3: Only the assigned agent escalates

- **E3.1** Given I-2 was accepted by Zoe, When support agent Max escalates I-2 to the *billing* team, Then the escalation is rejected and I-2 stays accepted.

### R4: An incident is escalated only to the billing, fleet or safety team

- **E4.1** Given Ben's incident I-7 (*other*, "the app charged me in the wrong currency") was accepted by Zoe, When Zoe escalates I-7 to a *legal* team, Then the escalation is rejected and I-7 stays accepted.
- **E4.2** Given I-7 was accepted by Zoe, When Zoe escalates I-7 to the *billing* team at 11:15 on 2026-11-09, Then I-7 is escalated to *billing* (Incident Escalated), although its type is *other*; the team need not match the type (DE-97).

## Contract

### Commands

| Command | Issued by | Input | On success | Rejected when |
|---------|-----------|-------|------------|---------------|
| Escalate Incident | Support Agent | incident, agent, team | Incident Escalated | R2, R3, R4 |

### Queries

| Query | Asked by | Input | Result |
|-------|----------|-------|--------|
| None. | | | |

### Events

| Event | When | Data |
|-------|------|------|
| Incident Escalated | the assigned agent escalated an accepted incident | incident, agent, team, escalated at |

## Data

| Shape | Field | Type | Constraint |
|-------|-------|------|------------|
| Escalation | team | one of {billing, fleet, safety} | required (DE-53) |
| Escalation | agent | Identifier (Support Agent) | the incident's assigned agent (DE-51) |

## Out of scope

- Accepting an incident (`support-agent-decides-on-incident`).
- Recording the team's resolution (`support-agent-resolves-incident`).
- How the teams themselves work (not modelled).

## Open questions

- ~~**Q1** To whom is an incident escalated?~~ To one of the teams billing, fleet or safety; the assigned agent records the team's resolution (role-played domain expert, DE-53, 2026-11-08)
- ~~**Q2** Who may escalate?~~ Only the assigned agent (role-played domain expert, DE-51, 2026-11-08)
- ~~**Q3** Does the team have to match the incident's type?~~ No, any of the three teams (role-played domain expert, DE-97, 2026-11-08)
- ~~**Q4** Can an escalated incident be escalated again?~~ No, an accepted incident is escalated at most once (role-played domain expert, DE-97, 2026-11-08)
- ~~**Q5** Is a safety concern escalated automatically?~~ No (role-played domain expert, DE-97, 2026-11-08)

## Notes for the plan

- None.
