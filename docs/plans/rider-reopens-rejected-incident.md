---
name: rider-reopens-rejected-incident
intent: rider-reopens-rejected-incident
spec: rider-reopens-rejected-incident
system: velocity
status: done
date: 2026-11-08
spec_date: 2026-11-08
---

# Implementation plan: A rider wants to reopen an incident that was rejected

<!-- Reviewers: edit this file or answer in the conversation, then set status to "approved" (or say so).
     Invoking implementation-planner again resumes from this file. -->

## Summary

The intent lands entirely in **CustomerSupport** (canvas UL "Reopened", CML key language "reopen"). It adds Reopen
Incident with its REST resource `POST /customer-support/incidents/{incidentId}/reopening` and extends the Incident
query with the rider's view of a rejection (reason, rejected at, reopenable until). The 14-calendar-day window is
counted in the city's time zone (D5, spec Q2 still open). Prerequisite: plan `support-agent-decides-on-incident`
(reject). No integration, no context map change.

## Placement

| Spec element | Owner | Why |
|---|---|---|
| R1 The rider can reopen a rejected incident within 14 calendar days; it stays with the agent who rejected it | CustomerSupport | canvas decision "A rider can reopen a rejected incident within 14 days" (DE-54, DE-56); the window guards incident state only CustomerSupport holds |
| R2 Only the rider who reported the incident can reopen it | CustomerSupport | the incident records its reporting rider (from `rider-reports-incident`); vision |
| R3 Only a rejected incident can be reopened | CustomerSupport | guards the incident's state |
| Command: Reopen Incident | CustomerSupport | CustomerSupport owns R1-R3, the rules that reject it |
| Query: Rider's Incident | CustomerSupport | a view of CustomerSupport's own incident (canvas Inbound "Rider app, Support agent / Query / Incident") |
| Event: Incident Reopened | CustomerSupport | CustomerSupport decides the reopening; no listener outside |

**Outcome:** one existing context.

## Context map changes

- None.

## Translation

| Spec term | Context | Context term | Where translated |
|---|---|---|---|
| Reopening window (14 calendar days, until the end of the 14th day) | CustomerSupport | reopenable until | canvas UL "Reopened" (add "Reopenable until", see Canvas changes); computed in the domain from the rejection instant and the city's zone (D5) |
| Reopened | CustomerSupport | Reopened (assigned to the rejecting agent) | canvas UL (same) |
| Rider's Incident (query) | CustomerSupport | Incident (query), rider's view | REST adapter (`riderId` request parameter) |
| Rider | CustomerSupport | Rider (`RiderId`) | REST adapter |

## Contract mapping

| Spec element | Context | Code |
|---|---|---|
| Command: Reopen Incident | CustomerSupport | `ReopenIncidentUseCase` + record `ReopenIncident(incidentId, riderId, reopenedAt)`; `POST /customer-support/incidents/{incidentId}/reopening` `{riderId}`; `409` with `ReopeningWindowClosed` (R1), `NotReportingRider` (R2), `IncidentNotRejected` (R3) |
| Query: Rider's Incident | CustomerSupport | `IncidentQuery` + `IncidentQueryHandler` (from `rider-reports-incident`) extended with an optional `riderId`; `GET /customer-support/incidents/{incidentId}?riderId=` → `{incidentId, riderId, type, status, assignedAgentId?, team?, resolution?, rejectionReason?, rejectedAt?, reopenableUntil?}`; `404` when `riderId` is given and isn't the reporting rider |
| Event: Incident Reopened | CustomerSupport | domain event `IncidentReopened(incidentId, riderId, assignedAgentId, reopenedAt)`; no integration event |

## Canvas changes

### CustomerSupport (`docs/bounded-contexts/velocity/customersupport/canvas.md`)

Already in the canvas, tag only:
- "A rider can reopen a rejected incident within 14 days" ← E1.1, E1.2 (same rule, the spec's dates are 2026-11-09 → 11-23).

Missing, add:
- **Ubiquitous language, add:** **Reopenable until**: the end of the 14th calendar day after the day of the rejection, in the city's local time (DE-54, DE-56; spec Q2 open).
- **Business decisions, add:**
  - `Only the reporting rider reopens (spec: rider-reopens-rejected-incident#E2.1)`: Given Ben's incident I-6 was rejected at 10:00 on 2026-11-09, When Anna reopens I-6 at 12:00 on 2026-11-10, Then the reopening is rejected and I-6 stays rejected.
  - `Only a rejected incident is reopened (spec: rider-reopens-rejected-incident#E3.1, #E3.2)`: Given Ben reopened I-6 at 09:00 on 2026-11-12, When Ben reopens I-6 again at 09:30, Then the reopening is rejected and I-6 stays reopened; an accepted incident (E3.1) can't be reopened either.

## Decisions

| # | Decision | Choice | Reason |
|---|---|---|---|
| D1 | TDD checkpoint loop | **off** | the trainer explicitly asked for a fully autonomous run (2026-11-08); the tactical skill's pushback is recorded in `docs/bounded-contexts/velocity/conversation.md`. Domain still built test-first, one example per cycle |
| D2 | Aggregate style | state-based | simplest; no requirement for an event history |
| D3 | ACLs | one per outbound query/command, in `@AntiCorruptionLayer` packages, calling REST via `RestClient` + `VelocityServiceUrls`; inbound events via `@Async @EventListener` ACL listeners | integration-contracts.md "Integration style" (no ACL needed by this intent) |
| D4 | Persistence | in-memory repositories with `Version` optimistic locking, after the domain is green | memory "aggregate persistence after domain"; repo convention |
| D5 | Time zone of "the end of the day" (spec Q2, open) | the city's local time, `Europe/Warsaw`, as CustomerSupport configuration (`velocity.customer-support.zone`, default `Europe/Warsaw`) handed to the domain; acceptance tests express the examples' times as Warsaw local time (2026-11-23 23:59 CET = `2026-11-23T22:59:00Z`) | the spec assumes city local time in E1.1/E1.2; the shared clock is UTC (`MutableClock`), so the zone must be explicit. The same window is used by `support-agent-resolves-incident` R6 |
| D6 | Reopening replaces the minimal reopened state | the reopened state built minimally by `support-agent-decides-on-incident` S4 (for its E2.3, E4.2) is reached through the real Reopen command from now on | one way into the state |

## Steps

### S1: Update the CustomerSupport canvas
- **Status:** done
- **Context:** CustomerSupport
- **Skill:** none (apply the approved canvas diff above)
- **Covers:** E2.1, E3.1, E3.2 (tags for E1.1, E1.2)
- **Done when:** canvas contains "Reopenable until" and the two new decisions, tagged.

### S2: Reopen Incident api record (6a)
- **Status:** done
- **Context:** CustomerSupport
- **Skill:** bounded-context-scaffolder (update mode, 6a)
- **Covers:** Reopen Incident
- **Done when:** record `ReopenIncident`; architecture test green; `./mvnw test` green.

### S3: Reopening rules, test-first
- **Status:** done
- **Context:** CustomerSupport
- **Skill:** ddd-tactical-building-blocks (D1: loop off; one spec example per cycle, verbatim)
- **Covers:** R1, R2, R3, E1.1, E1.2, E2.1, E3.1, E3.2, Incident Reopened
- **Done when:** one domain test per example, the window boundary tested at 23:59 and 00:00 local time (D5); the decide plan's domain tests for E2.3/E4.2 use the real reopening (D6); `./mvnw test` green.

### S4: Use case, REST, rider's view of the incident (6b)
- **Status:** done
- **Context:** CustomerSupport
- **Skill:** bounded-context-scaffolder (6b)
- **Covers:** Reopen Incident, Rider's Incident
- **Done when:** `ReopenIncidentUseCase`, `POST /customer-support/incidents/{incidentId}/reopening` with the 409 mapping, the extended Incident query, the zone configuration (D5); `./mvnw test` green.

### S5: Acceptance tests
- **Status:** done
- **Context:** CustomerSupport
- **Skill:** none (implementation-planner)
- **Covers:** all examples
- **Done when:** `src/test/java/pl/wojtyna/dddtrainingv3/velocity/customersupport/RiderReopensRejectedIncidentSpecTest.java`, `@VelocitySpringTest`, one method per example over REST, `MutableClock` set to the examples' local times (D5); the rider's view (`reopenableUntil`) asserted in E1.1/E1.2; enable the `@Disabled` E2.3 and E4.2 of `SupportAgentDecidesOnIncidentSpecTest`; `./mvnw test` green.

## Acceptance tests

| Example | Test |
|---|---|
| E1.1 | `RiderReopensRejectedIncidentSpecTest#e1_1_rider_reopens_on_last_minute_of_window` |
| E1.2 | `RiderReopensRejectedIncidentSpecTest#e1_2_reopening_after_window_is_rejected` |
| E2.1 | `RiderReopensRejectedIncidentSpecTest#e2_1_other_rider_cannot_reopen` |
| E3.1 | `RiderReopensRejectedIncidentSpecTest#e3_1_accepted_incident_cannot_be_reopened` |
| E3.2 | `RiderReopensRejectedIncidentSpecTest#e3_2_reopened_incident_cannot_be_reopened_again` |

## Traceability

| Spec ID | Step | Files |
|---|---|---|
| R1 / E1.1 | S1 (tag), S3, S4, S5 | `domain/incident/Incident.java` (`reopenBy`), `ReopeningWindow.java`, `ReopenIncidentUseCase`, `IncidentController`, `CustomerSupportContextConfig` (`customerSupportCityZone`); tests `IncidentTest.Reopening`, `ReopeningWindowTest`, `RiderReopensRejectedIncidentSpecTest` |
| R1 / E1.2 | S1 (tag), S3, S4, S5 | `domain/incident/Incident.java` (`reopenBy`), `ReopeningWindow.java`, `ReopenIncidentUseCase`, `IncidentController`, `CustomerSupportContextConfig` (`customerSupportCityZone`); tests `IncidentTest.Reopening`, `ReopeningWindowTest`, `RiderReopensRejectedIncidentSpecTest` |
| R2 / E2.1 | S1, S3, S5 | `domain/incident/Incident.java` (`reopenBy`), `ReopeningWindow.java`, `ReopenIncidentUseCase`, `IncidentController`, `CustomerSupportContextConfig` (`customerSupportCityZone`); tests `IncidentTest.Reopening`, `ReopeningWindowTest`, `RiderReopensRejectedIncidentSpecTest` |
| R3 / E3.1 | S1, S3, S5 | `domain/incident/Incident.java` (`reopenBy`), `ReopeningWindow.java`, `ReopenIncidentUseCase`, `IncidentController`, `CustomerSupportContextConfig` (`customerSupportCityZone`); tests `IncidentTest.Reopening`, `ReopeningWindowTest`, `RiderReopensRejectedIncidentSpecTest` |
| R3 / E3.2 | S1, S3, S5 | `domain/incident/Incident.java` (`reopenBy`), `ReopeningWindow.java`, `ReopenIncidentUseCase`, `IncidentController`, `CustomerSupportContextConfig` (`customerSupportCityZone`); tests `IncidentTest.Reopening`, `ReopeningWindowTest`, `RiderReopensRejectedIncidentSpecTest` |

## Risks and open questions

- **CONTRACT GAP:** the Incident query in integration-contracts.md has no rider's view: it lacks `rejectionReason`, `rejectedAt`, `reopenableUntil` and the spec's input *rider*. Planned as `GET /customer-support/incidents/{incidentId}?riderId=` with those optional fields, `404` for another rider.
- **Spec Q2 (open):** time zone of "the end of the day"; planned as `Europe/Warsaw` (D5). Affects E1.1, E1.2 and `support-agent-resolves-incident` E6.1/E6.2. If the answer is UTC, only the configuration and the tests' instants change.
- **Spec Q4 (open):** does the rider give a reason when pushing back? Not built; the request has no reason field. Adding one later is additive.
- **No authentication:** the rider ID comes from the request body, so R2 is only as strong as the rider app's identity.
- No differences from the reference context map.

## Review notes

- 2026-11-08: draft written (autonomous run); self-review against plan-critic.md checklist: AGREE (all elements in CustomerSupport; one tactical run of 3 rules guarding the incident's rejected/reopened state; all 5 examples covered; the open time-zone question is recorded with the steps it affects).
- 2026-11-08: built in the autonomous run (CustomerSupport worktree); decisions recorded in the changelog.

## Changelog

### 2026-11-08: planned
- Built from spec dated 2026-11-08.

### 2026-11-08: built (autonomous run)
- All steps done. The window is the value object `ReopeningWindow` (rejected at, closes at = 00:00 Warsaw on the 15th day); `reopenableUntil` in the query is that exclusive end (`2026-11-23T23:00:00Z` for a rejection at 10:00 on 2026-11-09).
- Rider's view: `GET /customer-support/incidents/{id}?riderId=` answers 404 for another rider (asserted in E2.1).
- The decide plan's E2.3/E4.2 acceptance tests were never disabled (D6 applied from the start).
