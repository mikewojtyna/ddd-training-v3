---
name: support-agent-escalates-incident
intent: support-agent-escalates-incident
spec: support-agent-escalates-incident
system: velocity
status: approved
date: 2026-11-08
spec_date: 2026-11-08
---

# Implementation plan: A support agent wants to escalate an accepted incident to a specialised team

<!-- Reviewers: edit this file or answer in the conversation, then set status to "approved" (or say so).
     Invoking implementation-planner again resumes from this file. -->

## Summary

The intent lands entirely in **CustomerSupport** (canvas UL "Escalated: an accepted incident handed to the billing,
fleet or safety team"). It adds Escalate Incident with its REST resource
`POST /customer-support/incidents/{incidentId}/escalation`. The teams aren't modelled as contexts: escalation only
records the team on the incident (DE-53). Prerequisite: plan `support-agent-decides-on-incident` (pick up, accept,
reject). No integration, no context map change.

## Placement

| Spec element | Owner | Why |
|---|---|---|
| R1 The assigned agent can escalate an accepted incident to a team | CustomerSupport | canvas UL "Escalated"; canvas Inbound "Support agent / Escalate Incident (team)"; CML key language "escalate" |
| R2 Only an accepted incident can be escalated, and at most once | CustomerSupport | guards the incident's state (DE-97) |
| R3 Only the assigned agent escalates | CustomerSupport | canvas UL "Assigned agent: only they work it" (DE-51) |
| R4 Only to the billing, fleet or safety team | CustomerSupport | closed list from DE-53; the team need not match the type (DE-97) |
| Command: Escalate Incident | CustomerSupport | CustomerSupport owns R2-R4, the rules that reject it |
| Event: Incident Escalated | CustomerSupport | CustomerSupport records the escalation; no listener outside (canvas Outbound); the teams aren't modelled (spec Out of scope) |
| Query: none in the spec | n/a | tests observe via `GET /customer-support/incidents/{incidentId}` (`team` field) |

**Outcome:** one existing context.

## Context map changes

- None. Escalating to the *fleet* team does not notify FleetMaintenance: fleet issues are handed over at report time (Fleet Issue Reported).

## Translation

| Spec term | Context | Context term | Where translated |
|---|---|---|---|
| Team *billing* / *fleet* / *safety* | CustomerSupport | Team `BILLING` / `FLEET` / `SAFETY` | REST adapter (string → team; outside the list → 400, D5) |
| Escalation | CustomerSupport | Escalated (team recorded on the incident) | canvas UL |
| Support agent | CustomerSupport | Agent (`AgentId`) | REST adapter |

## Contract mapping

| Spec element | Context | Code |
|---|---|---|
| Command: Escalate Incident | CustomerSupport | `EscalateIncidentUseCase` + record `EscalateIncident(incidentId, agentId, team, escalatedAt)`; `POST /customer-support/incidents/{incidentId}/escalation` `{agentId, team}`; `409` with `NotAssignedAgent` (R3) or `IncidentNotAccepted` (R2, also for an already escalated incident); `400 {"error": "UnknownTeam"}` (R4) |
| Event: Incident Escalated | CustomerSupport | domain event `IncidentEscalated(incidentId, agentId, team, escalatedAt)`; no integration event |
| Query: none in the spec | CustomerSupport | observation via `GET /customer-support/incidents/{incidentId}` (`status: "ESCALATED"`, `team`) |

## Canvas changes

### CustomerSupport (`docs/bounded-contexts/velocity/customersupport/canvas.md`)

Already in the canvas, tag only:
- "An escalated incident is resolved by the team's outcome" ← E1.1 (its first half).

Missing, add:
- **Business decisions, add:**
  - `Only an accepted incident is escalated, at most once (spec: support-agent-escalates-incident#E2.1, #E2.2, #E2.3)`: Given I-2 is escalated to the *billing* team, When Zoe escalates I-2 to the *fleet* team, Then the escalation is rejected and I-2 stays with *billing* (DE-97). A rejected (E2.1) or undecided (E2.2) incident can't be escalated either.
  - `Only the assigned agent escalates (spec: support-agent-escalates-incident#E3.1)`: Given I-2 was accepted by Zoe, When Max escalates I-2 to *billing*, Then the escalation is rejected and I-2 stays accepted.
  - `Escalation goes to billing, fleet or safety, whatever the type (spec: support-agent-escalates-incident#E4.1, #E4.2)`: Given I-7 (*other*) was accepted by Zoe, When Zoe escalates it to a *legal* team, Then the escalation is rejected; to *billing* it is escalated (Incident Escalated) (DE-53, DE-97).

## Decisions

| # | Decision | Choice | Reason |
|---|---|---|---|
| D1 | TDD checkpoint loop | **off** | the trainer explicitly asked for a fully autonomous run (2026-11-08); the tactical skill's pushback is recorded in `docs/bounded-contexts/velocity/conversation.md`. Domain still built test-first, one example per cycle |
| D2 | Aggregate style | state-based | simplest; no requirement for an event history |
| D3 | ACLs | one per outbound query/command, in `@AntiCorruptionLayer` packages, calling REST via `RestClient` + `VelocityServiceUrls`; inbound events via `@Async @EventListener` ACL listeners | integration-contracts.md "Integration style" (no ACL needed by this intent) |
| D4 | Persistence | in-memory repositories with `Version` optimistic locking, after the domain is green | memory "aggregate persistence after domain"; repo convention |
| D5 | Where R4 is enforced | the team is a closed type in the domain; an unknown team is rejected by the REST adapter with `400 UnknownTeam` | integration-contracts.md: 400 for malformed input; E4.1 is therefore an acceptance test only, E4.2 the domain test for R4 |

## Steps

### S1: Update the CustomerSupport canvas
- **Status:** todo
- **Context:** CustomerSupport
- **Skill:** none (apply the approved canvas diff above)
- **Covers:** E2.1, E2.2, E2.3, E3.1, E4.1, E4.2 (tag for E1.1)
- **Done when:** canvas contains the three new decisions, tagged.

### S2: Escalate Incident api record (6a)
- **Status:** todo
- **Context:** CustomerSupport
- **Skill:** bounded-context-scaffolder (update mode, 6a)
- **Covers:** Escalate Incident
- **Done when:** record `EscalateIncident`; architecture test green; `./mvnw test` green.

### S3: Escalation rules, test-first (run 1 of 2)
- **Status:** todo
- **Context:** CustomerSupport
- **Skill:** ddd-tactical-building-blocks (D1: loop off; one spec example per cycle, verbatim)
- **Covers:** R1, R2, R3, E1.1, E2.1, E2.2, E2.3, E3.1, Incident Escalated
- **Done when:** one domain test per example; `IncidentEscalated` returned in `DomainEvents`; `./mvnw test` green.

### S4: The three teams, test-first (run 2 of 2)
- **Status:** todo
- **Context:** CustomerSupport
- **Skill:** ddd-tactical-building-blocks (D1: loop off)
- **Covers:** R4, E4.2
- **Done when:** domain test for E4.2 (a team that doesn't match the type is accepted); the team is a closed type, so E4.1 is unrepresentable in the domain (D5); `./mvnw test` green.

### S5: Use case and REST (6b)
- **Status:** todo
- **Context:** CustomerSupport
- **Skill:** bounded-context-scaffolder (6b)
- **Covers:** Escalate Incident, E4.1
- **Done when:** `EscalateIncidentUseCase`, `POST /customer-support/incidents/{incidentId}/escalation` with the 409/400 mapping, `team` in the Incident query response; `./mvnw test` green.

### S6: Acceptance tests
- **Status:** todo
- **Context:** CustomerSupport
- **Skill:** none (implementation-planner)
- **Covers:** all examples
- **Done when:** `src/test/java/pl/wojtyna/dddtrainingv3/velocity/customersupport/SupportAgentEscalatesIncidentSpecTest.java`, `@VelocitySpringTest`, one method per example over REST, arranged with Report, Pick Up, Accept, Reject (prerequisite: `support-agent-decides-on-incident` S5); `./mvnw test` green.

## Acceptance tests

| Example | Test |
|---|---|
| E1.1 | `SupportAgentEscalatesIncidentSpecTest#e1_1_assigned_agent_escalates_accepted_incident_to_billing` |
| E2.1 | `SupportAgentEscalatesIncidentSpecTest#e2_1_rejected_incident_cannot_be_escalated` |
| E2.2 | `SupportAgentEscalatesIncidentSpecTest#e2_2_undecided_incident_cannot_be_escalated` |
| E2.3 | `SupportAgentEscalatesIncidentSpecTest#e2_3_escalated_incident_cannot_be_escalated_again` |
| E3.1 | `SupportAgentEscalatesIncidentSpecTest#e3_1_other_agent_cannot_escalate` |
| E4.1 | `SupportAgentEscalatesIncidentSpecTest#e4_1_escalation_to_unknown_team_is_rejected` |
| E4.2 | `SupportAgentEscalatesIncidentSpecTest#e4_2_team_need_not_match_incident_type` |

## Traceability

| Spec ID | Step | Files |
|---|---|---|
| R1 / E1.1 | S1 (tag), S3, S5, S6 | |
| R2 / E2.1 | S1, S3, S6 | |
| R2 / E2.2 | S1, S3, S6 | |
| R2 / E2.3 | S1, S3, S6 | |
| R3 / E3.1 | S1, S3, S6 | |
| R4 / E4.1 | S1, S5 (REST 400), S6 | |
| R4 / E4.2 | S1, S4, S6 | |

## Risks and open questions

- **Escalating to the *fleet* team** doesn't reach FleetMaintenance; fleet issues reach it at report time. If the business wants escalations to notify teams, that's a new intent (spec Out of scope: "How the teams themselves work").
- **Escalating a reopened-then-accepted incident** is allowed: it is accepted (DE-97). No example covers it; the tactical skill shouldn't add a rule against it.
- No differences from the reference context map.

## Review notes

- 2026-11-08: draft written (autonomous run); self-review against plan-critic.md checklist: AGREE (all elements in CustomerSupport; R4 enforced as a closed type at the edge, recorded in D5; tactical runs of 3 and 1 rules; all 7 examples covered; prerequisite on the decide plan explicit).

## Changelog

### 2026-11-08: planned
- Built from spec dated 2026-11-08.
