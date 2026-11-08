---
name: support-agent-resolves-incident
intent: support-agent-resolves-incident
system: velocity
status: approved
date: 2026-11-08
---

# Spec: A support agent wants to resolve an incident

Concrete rules, examples and contract for [the intent](../intents/support-agent-resolves-incident.md).

## Vocabulary

| Term | Meaning |
|------|---------|
| Resolved | The terminal state every incident must reach (vision, "Customer Support & Incidents"). |
| Resolution | The agent's short record of how the incident was resolved; for an escalated incident, the team's outcome (DE-53). |
| Assigned agent | The support agent who picked the incident up; only they resolve it (DE-51). |
| Escalated | An accepted incident handed to the billing, fleet or safety team; still assigned to its agent. |
| Reopened | A rejected incident the rider pushed back on; still assigned to the agent who rejected it, and it must be resolved (DE-54, DE-56). |

## Rules

### R1: The assigned agent can resolve an accepted incident directly

- **E1.1** Given Ben's incident I-4 (*safety concern*) was accepted by support agent Zoe, When Zoe resolves I-4 at 15:00 on 2026-11-09 with "talked to Ben, crossing reported to the city", Then I-4 is resolved with that resolution (Incident Resolved).

### R2: An escalated incident is resolved by its assigned agent recording the team's outcome

- **E2.1** Given Anna's incident I-2 was escalated by Zoe to the *billing* team, When Zoe resolves I-2 at 16:00 on 2026-11-10 with "billing team refunded the double charge", Then I-2 is resolved with that resolution, escalated to *billing* (Incident Resolved).

### R3: A reopened incident is resolved by the agent who rejected it

- **E3.1** Given Ben's incident I-6 was rejected by Zoe and reopened by Ben, and is assigned to Zoe, When Zoe resolves I-6 at 11:00 on 2026-11-13 with "explained the PLN 1.00 card check again, Ben agreed", Then I-6 is resolved (Incident Resolved).

### R4: Only the assigned agent resolves an incident

- **E4.1** Given I-2 is escalated and assigned to Zoe, When support agent Max resolves I-2 with "refunded", Then the resolution is rejected and I-2 stays escalated.

### R5: Only an accepted, escalated or reopened incident can be resolved, and only once

- **E5.1** Given Ben's incident I-4 is assigned to Zoe and not yet decided, When Zoe resolves I-4, Then the resolution is rejected and I-4 stays undecided.
- **E5.2** Given I-6 was rejected by Zoe and not reopened, When Zoe resolves I-6, Then the resolution is rejected and I-6 stays rejected (Q3).
- **E5.3** Given I-4 was resolved at 15:00 on 2026-11-09, When Zoe resolves I-4 again with "follow-up call", Then the resolution is rejected and I-4 keeps its first resolution.

### R6: A rejected incident not reopened within its window is resolved automatically when the window ends (DE-87)

- **E6.1** Given Ben's incident I-6 was rejected by Zoe at 10:00 on 2026-11-09 and Ben did not reopen it, When its reopening window ends at the end of 2026-11-23, Then I-6 is resolved with "rejection not contested" (Incident Resolved).
- **E6.2** Given I-6 was rejected at 10:00 on 2026-11-09 and Ben reopened it at 09:00 on 2026-11-12, When the end of 2026-11-23 passes, Then nothing happens and I-6 stays reopened with Zoe.

## Contract

### Commands

| Command | Issued by | Input | On success | Rejected when |
|---------|-----------|-------|------------|---------------|
| Resolve Incident | Support Agent | incident, agent, resolution | Incident Resolved | R4, R5 |
| Resolve Uncontested Rejections | Support Agent team, when reopening windows end | now | Incident Resolved | none (only rejected incidents whose window ended are resolved, R6) |

### Queries

| Query | Asked by | Input | Result |
|-------|----------|-------|--------|
| None. | | | |

### Events

| Event | When | Data |
|-------|------|------|
| Incident Resolved | the assigned agent resolved an accepted, escalated or reopened incident, or a rejection's window ended uncontested | incident, rider, agent (if resolved by one), resolution, team (if escalated), resolved at |

## Data

| Shape | Field | Type | Constraint |
|-------|-------|------|------------|
| Resolution | resolution | Text | required |
| Resolution | agent | Identifier (Support Agent) | the incident's assigned agent (DE-51) |

## Out of scope

- The automatic resolution of a fleet-issue incident with "bike fixed" when its bike is fixed, whatever state the incident is in (`technician-returns-fixed-bike-to-circulation`, DE-55).
- Escalating (`support-agent-escalates-incident`) and reopening (`rider-reopens-rejected-incident`).
- Refunds or other actions a resolution describes; the resolution only records them.

## Open questions

- ~~**Q1** Who records the resolution of an escalated incident, and who resolves a reopened one?~~ The assigned agent records the team's resolution; a reopened incident stays with the agent who rejected it and must be resolved (role-played domain expert, DE-53, DE-54, DE-56, 2026-11-08)
- ~~**Q2** How does a rejected incident that is never reopened reach the resolved state?~~ It is resolved automatically with "rejection not contested" when the 14-day window ends (role-played domain expert, DE-87, DE-56, 2026-11-08)
- **Q3** Can the assigned agent resolve a rejected incident directly, before its window ends? E5.2 reads DE-87 and DE-97 literally: no. (E5.2)

## Notes for the plan

- None.
