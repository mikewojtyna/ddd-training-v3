---
name: support-agent-resolves-incident
intent: support-agent-resolves-incident
spec: support-agent-resolves-incident
system: velocity
status: approved
date: 2026-11-08
spec_date: 2026-11-08
---

# Implementation plan: A support agent wants to resolve an incident

<!-- Reviewers: edit this file or answer in the conversation, then set status to "approved" (or say so).
     Invoking implementation-planner again resumes from this file. -->

## Summary

The intent lands entirely in **CustomerSupport** (canvas UL "Resolved: the terminal state every incident reaches").
It adds Resolve Incident with its REST resource `POST /customer-support/incidents/{incidentId}/resolution`, and the
time-driven Resolve Uncontested Rejections, run by a `@Scheduled` job in CustomerSupport (DE-87, integration-contracts
"Customer Support"). Prerequisites: plans `support-agent-decides-on-incident`, `support-agent-escalates-incident` and
`rider-reopens-rejected-incident` (the states an incident is resolved from, and the reopening window). The
automatic "bike fixed" resolution (DE-55) belongs to plan `technician-returns-fixed-bike-to-circulation`. No
integration, no context map change.

## Placement

| Spec element | Owner | Why |
|---|---|---|
| R1 The assigned agent can resolve an accepted incident directly | CustomerSupport | canvas UL "Resolved"; canvas Inbound "Support agent / Resolve Incident (resolution)" |
| R2 An escalated incident is resolved by its agent recording the team's outcome | CustomerSupport | canvas decision "An escalated incident is resolved by the team's outcome" (DE-53) |
| R3 A reopened incident is resolved by the agent who rejected it | CustomerSupport | canvas UL "Reopened: it must be resolved, never rejected again" (DE-54, DE-56, DE-97) |
| R4 Only the assigned agent resolves | CustomerSupport | canvas UL "Assigned agent: only they work it" (DE-51) |
| R5 Only an accepted, escalated or reopened incident can be resolved, and only once | CustomerSupport | guards the incident's state |
| R6 A rejected incident not reopened in its window is resolved automatically | CustomerSupport | canvas decision "An uncontested rejection is resolved when the window closes" (DE-87); canvas Inbound "Support (clock) / Resolve Uncontested Rejections" |
| Command: Resolve Incident | CustomerSupport | CustomerSupport owns R4, R5, the rules that reject it |
| Command: Resolve Uncontested Rejections | CustomerSupport | CustomerSupport's own clock; only it knows which rejections are uncontested |
| Event: Incident Resolved | CustomerSupport | CustomerSupport decides the resolution; no listener outside (canvas Outbound) |
| Query: none in the spec | n/a | tests observe via `GET /customer-support/incidents/{incidentId}` (`status`, `resolution`, `team`) |

**Outcome:** one existing context.

## Context map changes

- None.

## Translation

| Spec term | Context | Context term | Where translated |
|---|---|---|---|
| Resolution (text) | CustomerSupport | Resolution | canvas UL (same) |
| Resolved | CustomerSupport | Resolved (`RESOLVED`) | canvas UL; query response |
| Reopening window ends | CustomerSupport | reopenable until has passed | canvas UL "Reopenable until" (added by `rider-reopens-rejected-incident`) |
| "rejection not contested" | CustomerSupport | the fixed resolution of an uncontested rejection | domain constant |
| Support agent | CustomerSupport | Agent (`AgentId`) | REST adapter |

## Contract mapping

| Spec element | Context | Code |
|---|---|---|
| Command: Resolve Incident | CustomerSupport | `ResolveIncidentUseCase` + record `ResolveIncident(incidentId, agentId, resolution, resolvedAt)`; `POST /customer-support/incidents/{incidentId}/resolution` `{agentId, resolution}`; `409` with `NotAssignedAgent` (R4) or `IncidentNotResolvable` (R5); `400` for a blank resolution |
| Command: Resolve Uncontested Rejections | CustomerSupport | `ResolveUncontestedRejectionsUseCase` + record `ResolveUncontestedRejections(now)`; triggered by `infra/adapters/primary/scheduling/UncontestedRejectionsJob` (`@Scheduled`, off when `velocity.scheduling.enabled=false`) with `Instant.now(velocityClock)`; no REST resource |
| Event: Incident Resolved | CustomerSupport | domain event `IncidentResolved(incidentId, riderId, agentId?, resolution, team?, resolvedAt)`; no integration event (no listener outside CustomerSupport) |
| Query: none in the spec | CustomerSupport | observation via `GET /customer-support/incidents/{incidentId}` |

Secondary port: `Incidents` gains a finder for rejected incidents (used by the uncontested-rejections use case).

## Canvas changes

### CustomerSupport (`docs/bounded-contexts/velocity/customersupport/canvas.md`)

Already in the canvas, tag only:
- "An escalated incident is resolved by the team's outcome" ← E2.1.
- "A reopened incident can't be rejected again" ← E3.1 (its "Zoe has to accept or resolve it").
- "An uncontested rejection is resolved when the window closes" ← E6.1.

Missing, add:
- **Business decisions, add:**
  - `An accepted incident can be resolved directly (spec: support-agent-resolves-incident#E1.1)`: Given Ben's incident I-4 (*safety concern*) was accepted by Zoe, When Zoe resolves I-4 at 15:00 on 2026-11-09 with "talked to Ben, crossing reported to the city", Then I-4 is resolved with that resolution (Incident Resolved).
  - `Only the assigned agent resolves (spec: support-agent-resolves-incident#E4.1)`: Given I-2 is escalated and assigned to Zoe, When Max resolves I-2 with "refunded", Then the resolution is rejected and I-2 stays escalated.
  - `Only an accepted, escalated or reopened incident is resolved, once (spec: support-agent-resolves-incident#E5.1, #E5.2, #E5.3)`: Given I-6 was rejected by Zoe and not reopened, When Zoe resolves I-6, Then the resolution is rejected and I-6 stays rejected; an undecided (E5.1) or already resolved (E5.3) incident can't be resolved either.
  - `A reopened incident isn't resolved by the clock (spec: support-agent-resolves-incident#E6.2)`: Given I-6 was rejected at 10:00 on 2026-11-09 and Ben reopened it at 09:00 on 2026-11-12, When the end of 2026-11-23 passes, Then nothing happens and I-6 stays reopened with Zoe.

## Decisions

| # | Decision | Choice | Reason |
|---|---|---|---|
| D1 | TDD checkpoint loop | **off** | the trainer explicitly asked for a fully autonomous run (2026-11-08); the tactical skill's pushback is recorded in `docs/bounded-contexts/velocity/conversation.md`. Domain still built test-first, one example per cycle |
| D2 | Aggregate style | state-based | simplest; no requirement for an event history |
| D3 | ACLs | one per outbound query/command, in `@AntiCorruptionLayer` packages, calling REST via `RestClient` + `VelocityServiceUrls`; inbound events via `@Async @EventListener` ACL listeners | integration-contracts.md "Integration style" (no ACL needed by this intent) |
| D4 | Persistence | in-memory repositories with `Version` optimistic locking, after the domain is green | memory "aggregate persistence after domain"; repo convention. A reopening racing the job for the same incident: one of them gets a version conflict; the job skips the incident and retries on its next run, where it finds it reopened |
| D5 | When the window has "ended" | from 00:00 local time (`Europe/Warsaw`) of the 15th day after the rejection day, the same window that rejects a reopening (`rider-reopens-rejected-incident` D5) | E6.1 "at the end of 2026-11-23" and the reopening's E1.2 must agree; one domain notion, one zone |
| D6 | Spec Q3 (open) | the assigned agent can't resolve a rejected incident before its window ends (E5.2 as written) | the spec reads DE-87/DE-97 literally; recorded as open for the trainer |

## Steps

### S1: Update the CustomerSupport canvas
- **Status:** todo
- **Context:** CustomerSupport
- **Skill:** none (apply the approved canvas diff above)
- **Covers:** E1.1, E4.1, E5.1, E5.2, E5.3, E6.2 (tags for E2.1, E3.1, E6.1)
- **Done when:** canvas contains the four new decisions, tagged.

### S2: Api records (6a)
- **Status:** todo
- **Context:** CustomerSupport
- **Skill:** bounded-context-scaffolder (update mode, 6a)
- **Covers:** Resolve Incident, Resolve Uncontested Rejections
- **Done when:** records `ResolveIncident`, `ResolveUncontestedRejections`; architecture test green; `./mvnw test` green.

### S3: Resolving by the agent, test-first (run 1 of 3)
- **Status:** todo
- **Context:** CustomerSupport
- **Skill:** ddd-tactical-building-blocks (D1: loop off; one spec example per cycle, verbatim)
- **Covers:** R1, R2, R3, E1.1, E2.1, E3.1, Incident Resolved
- **Done when:** one domain test per example; `IncidentResolved` carries the team for an escalated incident; `./mvnw test` green.

### S4: Who may resolve and from which state, test-first (run 2 of 3)
- **Status:** todo
- **Context:** CustomerSupport
- **Skill:** ddd-tactical-building-blocks (D1: loop off)
- **Covers:** R4, R5, E4.1, E5.1, E5.2, E5.3
- **Done when:** one domain test per example; `./mvnw test` green.

### S5: Uncontested rejections, test-first (run 3 of 3)
- **Status:** todo
- **Context:** CustomerSupport
- **Skill:** ddd-tactical-building-blocks (D1: loop off)
- **Covers:** R6, E6.1, E6.2
- **Done when:** one domain test per example, plus the window boundary from D5 (still rejected at 23:59 local on day 14, resolved from 00:00 on day 15, as in the canvas variant); `./mvnw test` green.

### S6: Use cases, REST, scheduled job (6b)
- **Status:** todo
- **Context:** CustomerSupport
- **Skill:** bounded-context-scaffolder (6b)
- **Covers:** Resolve Incident, Resolve Uncontested Rejections, Incident Resolved
- **Done when:** `ResolveIncidentUseCase`, `POST /customer-support/incidents/{incidentId}/resolution` with the 409/400 mapping, `ResolveUncontestedRejectionsUseCase`, `UncontestedRejectionsJob`, `resolution` in the Incident query; `./mvnw test` green.

### S7: Acceptance tests
- **Status:** todo
- **Context:** CustomerSupport
- **Skill:** none (implementation-planner)
- **Covers:** all examples
- **Done when:** `src/test/java/pl/wojtyna/dddtrainingv3/velocity/customersupport/SupportAgentResolvesIncidentSpecTest.java`, `@VelocitySpringTest`, one method per example over REST, arranged with Report, Pick Up, Accept, Reject, Escalate, Reopen (prerequisites: the 6b steps of `support-agent-decides-on-incident`, `support-agent-escalates-incident`, `rider-reopens-rejected-incident`); E6.1/E6.2 set the `MutableClock` to 2026-11-24 00:00 Warsaw time and call `ResolveUncontestedRejectionsUseCase` directly (scheduling is off in tests); `./mvnw test` green.

## Acceptance tests

| Example | Test |
|---|---|
| E1.1 | `SupportAgentResolvesIncidentSpecTest#e1_1_assigned_agent_resolves_accepted_incident` |
| E2.1 | `SupportAgentResolvesIncidentSpecTest#e2_1_agent_records_team_outcome_for_escalated_incident` |
| E3.1 | `SupportAgentResolvesIncidentSpecTest#e3_1_rejecting_agent_resolves_reopened_incident` |
| E4.1 | `SupportAgentResolvesIncidentSpecTest#e4_1_other_agent_cannot_resolve` |
| E5.1 | `SupportAgentResolvesIncidentSpecTest#e5_1_undecided_incident_cannot_be_resolved` |
| E5.2 | `SupportAgentResolvesIncidentSpecTest#e5_2_rejected_incident_cannot_be_resolved_by_agent` |
| E5.3 | `SupportAgentResolvesIncidentSpecTest#e5_3_resolved_incident_keeps_its_first_resolution` |
| E6.1 | `SupportAgentResolvesIncidentSpecTest#e6_1_uncontested_rejection_is_resolved_when_window_ends` |
| E6.2 | `SupportAgentResolvesIncidentSpecTest#e6_2_reopened_incident_is_not_resolved_by_the_clock` |

## Traceability

| Spec ID | Step | Files |
|---|---|---|
| R1 / E1.1 | S1, S3, S6, S7 | |
| R2 / E2.1 | S1 (tag), S3, S6, S7 | |
| R3 / E3.1 | S1 (tag), S3, S7 | |
| R4 / E4.1 | S1, S4, S7 | |
| R5 / E5.1 | S1, S4, S7 | |
| R5 / E5.2 | S1, S4, S7 | |
| R5 / E5.3 | S1, S4, S7 | |
| R6 / E6.1 | S1 (tag), S5, S6, S7 | |
| R6 / E6.2 | S1, S5, S6, S7 | |

## Risks and open questions

- **Spec Q3 (open):** can the agent resolve a rejected incident before its window ends? Planned as no (D6, E5.2). A yes would move E5.2 and change R5 only.
- **Time zone (spec `rider-reopens-rejected-incident` Q2, open):** the window end uses `Europe/Warsaw` (D5); affects E6.1/E6.2.
- **"Bike fixed" resolution (DE-55)** resolves from any open state, unlike R5; it is a separate command (`Resolve Fleet Issue`, `POST /customer-support/incidents/{incidentId}/fleet-resolution`) planned by `technician-returns-fixed-bike-to-circulation`. The tactical skill must keep the two paths apart, so R5 doesn't block DE-55.
- **The job's granularity:** with a fixed-delay job, an uncontested rejection is resolved shortly after midnight, not exactly at it. No business consequence.
- No differences from the reference context map.

## Review notes

- 2026-11-08: draft written (autonomous run); self-review against plan-critic.md checklist: AGREE (all elements in CustomerSupport per canvas/DE-87; three tactical runs of at most 3 rules, grouped by agent resolution / guards / clock; all 9 examples covered; the window shared with the reopen plan; DE-55 left to its owner plan).

## Changelog

### 2026-11-08: planned
- Built from spec dated 2026-11-08.
