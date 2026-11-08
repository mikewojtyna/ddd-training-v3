---
name: support-agent-decides-on-incident
intent: support-agent-decides-on-incident
system: velocity
status: approved
date: 2026-11-08
---

# Spec: A support agent wants to take on an incident and accept or reject it

Concrete rules, examples and contract for [the intent](../intents/support-agent-decides-on-incident.md).

## Vocabulary

| Term | Meaning |
|------|---------|
| Open incident | Reported and not yet picked up by any support agent. |
| Pick up | A support agent takes an open incident; from then on it is assigned to them (DE-51). |
| Assigned agent | The support agent who picked the incident up; only they accept, reject, escalate or resolve it (DE-51). |
| Accepted | The assigned agent agreed the report needs action. |
| Rejected | The assigned agent turned the report down, with a reason; the rider may push back. |
| Reopened | A rejected incident the rider pushed back on; it can't be rejected again (DE-54). |

## Rules

### R1: A support agent can pick up an open incident, which is then assigned to them

- **E1.1** Given Anna's incident I-2 (*billing dispute*, "charged twice for one ride") is open, When support agent Zoe picks it up at 09:00 on 2026-11-09, Then I-2 is assigned to Zoe (Incident Picked Up).
- **E1.2** Given I-2 is assigned to Zoe, When support agent Max picks up I-2, Then the pick-up is rejected and I-2 stays assigned to Zoe (Q3).

### R2: The assigned agent accepts or rejects an incident they picked up

- **E2.1** Given I-2 is assigned to Zoe and not yet decided, When Zoe accepts I-2 at 09:30 on 2026-11-09, Then I-2 is accepted (Incident Accepted).
- **E2.2** Given Ben's incident I-6 (*billing dispute*, "charged PLN 1.00 when I added my card") is assigned to Zoe, When Zoe rejects I-6 at 10:00 on 2026-11-09 with the reason "the PLN 1.00 was a card check, released at once", Then I-6 is rejected with that reason (Incident Rejected).
- **E2.3** Given I-6 was rejected by Zoe, reopened by Ben and is assigned to Zoe again, When Zoe accepts I-6 at 09:00 on 2026-11-13, Then I-6 is accepted (Incident Accepted) (DE-97).

### R3: Only the assigned agent decides on an incident

- **E3.1** Given I-2 is assigned to Zoe and not yet decided, When Max accepts I-2, Then the acceptance is rejected and I-2 stays undecided with Zoe.
- **E3.2** Given Ben's incident I-4 (*safety concern*) is open, When Zoe accepts I-4 without picking it up, Then the acceptance is rejected and I-4 stays open.

### R4: An accepted incident can't be decided again, and a reopened incident can't be rejected again

- **E4.1** Given I-2 was accepted by Zoe, When Zoe rejects I-2, Then the rejection is rejected and I-2 stays accepted.
- **E4.2** Given I-6 was rejected by Zoe and Ben reopened it, so it is reopened and assigned to Zoe, When Zoe rejects I-6, Then the rejection is rejected and I-6 stays reopened; Zoe has to resolve it (DE-54).

## Contract

### Commands

| Command | Issued by | Input | On success | Rejected when |
|---------|-----------|-------|------------|---------------|
| Pick Up Incident | Support Agent | incident, agent | Incident Picked Up | R1 |
| Accept Incident | Support Agent | incident, agent | Incident Accepted | R3, R4 |
| Reject Incident | Support Agent | incident, agent, reason | Incident Rejected | R3, R4 |

### Queries

| Query | Asked by | Input | Result |
|-------|----------|-------|--------|
| Open Incidents | Support Agent | none | list of incident, incident type, category, description, reported at |

### Events

| Event | When | Data |
|-------|------|------|
| Incident Picked Up | a support agent picked up an open incident | incident, agent, picked up at |
| Incident Accepted | the assigned agent accepted the incident | incident, agent, accepted at |
| Incident Rejected | the assigned agent rejected the incident | incident, rider, agent, reason, rejected at |

## Data

| Shape | Field | Type | Constraint |
|-------|-------|------|------------|
| Incident | state | one of {open, picked up, accepted, rejected, escalated, reopened, resolved} | open when reported |
| Incident | assigned agent | Identifier (Support Agent) | set when picked up; never changes (Q3) |
| Rejection | reason | Text | required (DE-97) |

## Out of scope

- Reporting an incident (`rider-reports-incident`).
- Escalating an accepted incident (`support-agent-escalates-incident`).
- Resolving (`support-agent-resolves-incident`).
- Reopening a rejected incident (`rider-reopens-rejected-incident`).
- Handing an incident over to another agent (candidate, Q3).

## Open questions

- ~~**Q1** How are incidents assigned?~~ A support agent picks up an open incident and it is assigned to them; only the assigned agent accepts, rejects, escalates or resolves it (role-played domain expert, DE-51, 2026-11-08)
- ~~**Q2** Can a reopened incident be rejected again?~~ No, it must be resolved (role-played domain expert, DE-54, 2026-11-08)
- **Q3** Can an incident be handed over to another agent, for example when Zoe is away? Today E1.2 rejects a second pick-up. (E1.2, Data)
- ~~**Q4** Must a rejection always carry a reason?~~ Yes (role-played domain expert, DE-97, 2026-11-08)
- ~~**Q5** Can the agent accept a reopened incident?~~ Yes; the agent who rejected it may accept it (and then resolve or escalate it) or resolve it directly (role-played domain expert, DE-97, 2026-11-08)

## Notes for the plan

- None.
