---
name: support-agent-decides-on-incident
intent: support-agent-decides-on-incident
spec: support-agent-decides-on-incident
system: velocity
status: done
date: 2026-11-08
spec_date: 2026-11-08
---

# Implementation plan: A support agent wants to take on an incident and accept or reject it

<!-- Reviewers: edit this file or answer in the conversation, then set status to "approved" (or say so).
     Invoking implementation-planner again resumes from this file. -->

## Summary

The intent lands entirely in **CustomerSupport** (CML responsibility "Incident handling"). It adds Pick Up, Accept
and Reject Incident with their REST resources from integration-contracts.md, and the Open Incidents query for the
support agent. Prerequisite: plan `rider-reports-incident` (module, incidents, Incident query). E2.3 and E4.2 need a
reopened incident, so their acceptance tests depend on plan `rider-reopens-rejected-incident`. No integration, no
context map change.

## Placement

| Spec element | Owner | Why |
|---|---|---|
| R1 A support agent can pick up an open incident, which is then assigned to them | CustomerSupport | canvas UL "Assigned agent: the support agent who picked the incident up"; DE-51 |
| R2 The assigned agent accepts or rejects an incident they picked up | CustomerSupport | canvas UL "Accepted / Rejected: the agent's decision on an assigned incident" |
| R3 Only the assigned agent decides | CustomerSupport | canvas decision "Only the assigned agent decides"; guards the same incident state |
| R4 An accepted incident can't be decided again; a reopened one can't be rejected | CustomerSupport | canvas decision "A reopened incident can't be rejected again" (DE-54, DE-97) |
| Command: Pick Up Incident | CustomerSupport | CustomerSupport owns R1, which rejects a second pick-up |
| Command: Accept Incident | CustomerSupport | owns R3, R4 |
| Command: Reject Incident | CustomerSupport | owns R3, R4 and the required reason (DE-97) |
| Query: Open Incidents | CustomerSupport | a view of CustomerSupport's own incidents |
| Event: Incident Picked Up | CustomerSupport | CustomerSupport decides the assignment; no listener outside |
| Event: Incident Accepted | CustomerSupport | the agent's decision recorded in CustomerSupport; no listener outside |
| Event: Incident Rejected | CustomerSupport | same; no listener outside |

**Outcome:** one existing context.

## Context map changes

- None.

## Translation

| Spec term | Context | Context term | Where translated |
|---|---|---|---|
| Support agent | CustomerSupport | Agent (`AgentId`) | REST adapter (`agentId` → `AgentId`) |
| Pick up | CustomerSupport | Pick up → assigned agent | canvas UL |
| Picked up (state) | CustomerSupport | `PICKED_UP` (assigned, undecided) | query response |
| Accepted / Rejected (reason) | CustomerSupport | Accepted / Rejected (rejection reason) | canvas UL (same) |
| Open incident | CustomerSupport | Open | canvas UL ("Open" added by `rider-reports-incident`) |

## Contract mapping

| Spec element | Context | Code |
|---|---|---|
| Command: Pick Up Incident | CustomerSupport | `PickUpIncidentUseCase` + record `PickUpIncident(incidentId, agentId, pickedUpAt)`; `POST /customer-support/incidents/{incidentId}/assignment` `{agentId}`; `409 {"error": "IncidentAlreadyAssigned"}` |
| Command: Accept Incident | CustomerSupport | `AcceptIncidentUseCase` + record `AcceptIncident(incidentId, agentId, acceptedAt)`; `POST /customer-support/incidents/{incidentId}/acceptance` `{agentId}`; `409` with `NotAssignedAgent` (R3) or `IncidentAlreadyDecided` (R4) |
| Command: Reject Incident | CustomerSupport | `RejectIncidentUseCase` + record `RejectIncident(incidentId, agentId, reason, rejectedAt)`; `POST /customer-support/incidents/{incidentId}/rejection` `{agentId, reason}`; `409` with `NotAssignedAgent`, `IncidentAlreadyDecided` or `ReopenedIncidentCannotBeRejected`; `400` for a blank reason |
| Query: Open Incidents | CustomerSupport | `OpenIncidentsQuery` + `OpenIncidentsQueryHandler` in `api/queries`; `GET /customer-support/incidents?status=OPEN` → list of `{incidentId, type, category, description, reportedAt}` (see CONTRACT GAP) |
| Event: Incident Picked Up | CustomerSupport | domain event `IncidentPickedUp(incidentId, agentId, pickedUpAt)`; no integration event |
| Event: Incident Accepted | CustomerSupport | domain event `IncidentAccepted(incidentId, agentId, acceptedAt)`; no integration event |
| Event: Incident Rejected | CustomerSupport | domain event `IncidentRejected(incidentId, riderId, agentId, reason, rejectedAt)`; no integration event |

## Canvas changes

### CustomerSupport (`docs/bounded-contexts/velocity/customersupport/canvas.md`)

Already in the canvas, tag only:
- "Only the assigned agent decides" ← E3.1, E2.1 (and E1.1: Zoe picked I-1 up).
- "A reopened incident can't be rejected again" ← E4.2.

Missing, add:
- **Business decisions, add:**
  - `An incident is picked up once (spec: support-agent-decides-on-incident#E1.2)`: Given I-2 is assigned to Zoe, When support agent Max picks up I-2, Then the pick-up is rejected and I-2 stays assigned to Zoe.
  - `Nobody decides on an incident before it's picked up (spec: support-agent-decides-on-incident#E3.2)`: Given Ben's incident I-4 (*safety concern*) is open, When Zoe accepts I-4 without picking it up, Then the acceptance is rejected and I-4 stays open.
  - `A rejection carries a reason (spec: support-agent-decides-on-incident#E2.2)`: Given I-6 is assigned to Zoe, When Zoe rejects it with "the PLN 1.00 was a card check, released at once", Then I-6 is rejected with that reason (Incident Rejected) (DE-97).
  - `An accepted incident isn't decided again (spec: support-agent-decides-on-incident#E4.1)`: Given I-2 was accepted by Zoe, When Zoe rejects I-2, Then the rejection is rejected and I-2 stays accepted.
  - `A reopened incident can be accepted (spec: support-agent-decides-on-incident#E2.3)`: Given I-6 was rejected by Zoe, reopened by Ben and is assigned to Zoe again, When Zoe accepts I-6, Then I-6 is accepted (Incident Accepted) (DE-97).

## Decisions

| # | Decision | Choice | Reason |
|---|---|---|---|
| D1 | TDD checkpoint loop | **off** | the trainer explicitly asked for a fully autonomous run (2026-11-08); the tactical skill's pushback is recorded in `docs/bounded-contexts/velocity/conversation.md`. Domain still built test-first, one example per cycle |
| D2 | Aggregate style | state-based | simplest; no requirement for an event history |
| D3 | ACLs | one per outbound query/command, in `@AntiCorruptionLayer` packages, calling REST via `RestClient` + `VelocityServiceUrls`; inbound events via `@Async @EventListener` ACL listeners | integration-contracts.md "Integration style" (no ACL needed by this intent) |
| D4 | Persistence | in-memory repositories with `Version` optimistic locking, after the domain is green | memory "aggregate persistence after domain"; repo convention. Two agents picking up the same incident at once: the version conflict makes one of them fail like E1.2 |
| D5 | Hand-over between agents (spec Q3, open) | not built; a second pick-up is rejected (E1.2) | the spec's Q3 stays open for the trainer; the assigned agent never changes |

## Steps

### S1: Update the CustomerSupport canvas
- **Status:** done
- **Context:** CustomerSupport
- **Skill:** none (apply the approved canvas diff above)
- **Covers:** E1.2, E2.2, E2.3, E3.2, E4.1 (tags for E1.1, E2.1, E3.1, E4.2)
- **Done when:** canvas contains the five new decisions, tagged.

### S2: Api records and query (6a)
- **Status:** done
- **Context:** CustomerSupport
- **Skill:** bounded-context-scaffolder (update mode, 6a)
- **Covers:** Pick Up Incident, Accept Incident, Reject Incident, Open Incidents
- **Done when:** records `PickUpIncident`, `AcceptIncident`, `RejectIncident`, `OpenIncidentsQuery`; architecture test green; `./mvnw test` green.

### S3: Pick-up and assignment rules, test-first (run 1 of 2)
- **Status:** done
- **Context:** CustomerSupport
- **Skill:** ddd-tactical-building-blocks (D1: loop off; one spec example per cycle, verbatim)
- **Covers:** R1, R3, E1.1, E1.2, E3.1, E3.2, Incident Picked Up
- **Done when:** one domain test per example; `./mvnw test` green.

### S4: Accept and reject rules, test-first (run 2 of 2)
- **Status:** done
- **Context:** CustomerSupport
- **Skill:** ddd-tactical-building-blocks (D1: loop off)
- **Covers:** R2, R4, E2.1, E2.2, E2.3, E4.1, E4.2, Incident Accepted, Incident Rejected
- **Done when:** one domain test per example; E2.3 and E4.2 use a minimal reopened state (Reopen comes with `rider-reopens-rejected-incident`, which then replaces it); `./mvnw test` green.

### S5: Use cases, REST, query (6b)
- **Status:** done
- **Context:** CustomerSupport
- **Skill:** bounded-context-scaffolder (6b)
- **Covers:** Pick Up Incident, Accept Incident, Reject Incident, Open Incidents
- **Done when:** the three use cases, their REST resources with the 409/400 mapping, `OpenIncidentsQueryHandler` + `GET /customer-support/incidents?status=OPEN`; `./mvnw test` green.

### S6: Acceptance tests
- **Status:** done
- **Context:** CustomerSupport
- **Skill:** none (implementation-planner)
- **Covers:** all examples
- **Done when:** `src/test/java/pl/wojtyna/dddtrainingv3/velocity/customersupport/SupportAgentDecidesOnIncidentSpecTest.java`, `@VelocitySpringTest`, one method per example over REST (incidents reported through `POST /customer-support/incidents`); E2.3 and E4.2 need `POST …/reopening` (prerequisite: plan `rider-reopens-rejected-incident`, its 6b step) and are `@Disabled("needs rider-reopens-rejected-incident")` until then; `./mvnw test` green.

## Acceptance tests

| Example | Test |
|---|---|
| E1.1 | `SupportAgentDecidesOnIncidentSpecTest#e1_1_agent_picks_up_open_incident` |
| E1.2 | `SupportAgentDecidesOnIncidentSpecTest#e1_2_second_pick_up_is_rejected` |
| E2.1 | `SupportAgentDecidesOnIncidentSpecTest#e2_1_assigned_agent_accepts_incident` |
| E2.2 | `SupportAgentDecidesOnIncidentSpecTest#e2_2_assigned_agent_rejects_incident_with_reason` |
| E2.3 | `SupportAgentDecidesOnIncidentSpecTest#e2_3_assigned_agent_accepts_reopened_incident` |
| E3.1 | `SupportAgentDecidesOnIncidentSpecTest#e3_1_other_agent_cannot_accept` |
| E3.2 | `SupportAgentDecidesOnIncidentSpecTest#e3_2_open_incident_cannot_be_accepted_without_pick_up` |
| E4.1 | `SupportAgentDecidesOnIncidentSpecTest#e4_1_accepted_incident_cannot_be_rejected` |
| E4.2 | `SupportAgentDecidesOnIncidentSpecTest#e4_2_reopened_incident_cannot_be_rejected_again` |
| (DE-97, reason required) | `SupportAgentDecidesOnIncidentSpecTest#rejection_without_reason_is_bad_request` |

## Traceability

| Spec ID | Step | Files |
|---|---|---|
| R1 / E1.1 | S1 (tag), S3, S5, S6 | `domain/incident/Incident.java` (`pickUpBy`, `acceptBy`, `rejectBy`), use cases, `IncidentController`; tests `IncidentTest.PickingUp`/`Deciding`/`DecidingReopened`, `SupportAgentDecidesOnIncidentSpecTest` |
| R1 / E1.2 | S1, S3, S5, S6 | `domain/incident/Incident.java` (`pickUpBy`, `acceptBy`, `rejectBy`), use cases, `IncidentController`; tests `IncidentTest.PickingUp`/`Deciding`/`DecidingReopened`, `SupportAgentDecidesOnIncidentSpecTest` |
| R2 / E2.1 | S1 (tag), S4, S5, S6 | `domain/incident/Incident.java` (`pickUpBy`, `acceptBy`, `rejectBy`), use cases, `IncidentController`; tests `IncidentTest.PickingUp`/`Deciding`/`DecidingReopened`, `SupportAgentDecidesOnIncidentSpecTest` |
| R2 / E2.2 | S1, S4, S5, S6 | `domain/incident/Incident.java` (`pickUpBy`, `acceptBy`, `rejectBy`), use cases, `IncidentController`; tests `IncidentTest.PickingUp`/`Deciding`/`DecidingReopened`, `SupportAgentDecidesOnIncidentSpecTest` |
| R2 / E2.3 | S1, S4, S6 | `domain/incident/Incident.java` (`pickUpBy`, `acceptBy`, `rejectBy`), use cases, `IncidentController`; tests `IncidentTest.PickingUp`/`Deciding`/`DecidingReopened`, `SupportAgentDecidesOnIncidentSpecTest` |
| R3 / E3.1 | S1 (tag), S3, S6 | `domain/incident/Incident.java` (`pickUpBy`, `acceptBy`, `rejectBy`), use cases, `IncidentController`; tests `IncidentTest.PickingUp`/`Deciding`/`DecidingReopened`, `SupportAgentDecidesOnIncidentSpecTest` |
| R3 / E3.2 | S1, S3, S6 | `domain/incident/Incident.java` (`pickUpBy`, `acceptBy`, `rejectBy`), use cases, `IncidentController`; tests `IncidentTest.PickingUp`/`Deciding`/`DecidingReopened`, `SupportAgentDecidesOnIncidentSpecTest` |
| R4 / E4.1 | S1, S4, S6 | `domain/incident/Incident.java` (`pickUpBy`, `acceptBy`, `rejectBy`), use cases, `IncidentController`; tests `IncidentTest.PickingUp`/`Deciding`/`DecidingReopened`, `SupportAgentDecidesOnIncidentSpecTest` |
| R4 / E4.2 | S1 (tag), S4, S6 | `domain/incident/Incident.java` (`pickUpBy`, `acceptBy`, `rejectBy`), use cases, `IncidentController`; tests `IncidentTest.PickingUp`/`Deciding`/`DecidingReopened`, `SupportAgentDecidesOnIncidentSpecTest` |

## Risks and open questions

- **CONTRACT GAP:** integration-contracts.md has no Open Incidents query. Planned as `GET /customer-support/incidents?status=OPEN` → list of `{incidentId, type, category, description, reportedAt}`.
- **Spec Q3 (open):** handing an incident over to another agent; not built (D5). Affects E1.2 only if answered differently.
- **Circular build order between intents:** E2.3/E4.2 need Reopen, while Reopen needs Reject from this plan. Resolved by building the domain with a minimal reopened state here (S4) and enabling the two acceptance tests when `rider-reopens-rejected-incident` is delivered.
- **No authentication:** agent IDs come from the request body.
- No differences from the reference context map.

## Review notes

- 2026-11-08: draft written (autonomous run); self-review against plan-critic.md checklist: AGREE (all elements in CustomerSupport per canvas/CML; tactical runs of 2 rules each, grouped by assignment vs decision state; all 9 examples covered; the dependency on the reopen plan is explicit).
- 2026-11-08: built in the autonomous run (CustomerSupport worktree); decisions recorded in the changelog.

## Changelog

### 2026-11-08: planned
- Built from spec dated 2026-11-08.

### 2026-11-08: built (autonomous run)
- All steps done; 9 spec examples as acceptance tests plus `rejection_without_reason_is_bad_request` (400, DE-97).
- S4's minimal reopened state was not needed: Reopen (`rider-reopens-rejected-incident` S3) was built before E2.3/E4.2, so both use the real reopening and are enabled from the start (reopen plan D6).
- Derived domain test `acceptedIncidentCannotBeAcceptedAgain` (R4: an accepted incident isn't decided again, also by a second acceptance).
- E3.2 (accept without pick-up) answers `409 NotAssignedAgent`, as the contract mapping allows.
