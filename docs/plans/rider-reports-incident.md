---
name: rider-reports-incident
intent: rider-reports-incident
spec: rider-reports-incident
system: velocity
status: done
date: 2026-11-08
spec_date: 2026-11-08
---

# Implementation plan: A rider wants to report an incident

<!-- Reviewers: edit this file or answer in the conversation, then set status to "approved" (or say so).
     Invoking implementation-planner again resumes from this file. -->

## Summary

The intent lands in **CustomerSupport**, a new code module for an existing context (canvas
`docs/bounded-contexts/velocity/customersupport/canvas.md`, CML responsibility "Incident handling"). Reporting an
incident and detecting its type are CustomerSupport rules; a fleet issue is handed to **FleetMaintenance** as
CustomerSupport's published language `FleetIssueReported`, over the existing
`CustomerSupport [U,OHS,PL]->[D,ACL] FleetMaintenance` relationship. Making the bike broken and withdrawing it (the
second half of E3.1 and E3.3) is built by plan `fleet-operations-takes-broken-bike-out-of-circulation`; this plan
only asserts that hand-off end to end once that plan is delivered. This is the first CustomerSupport intent, so it
scaffolds the module. No context map change.

## Placement

| Spec element | Owner | Why |
|---|---|---|
| R1 A rider can report an incident at any time; open until picked up | CustomerSupport | canvas purpose "every reported incident is routed by its type, worked by a support agent…"; CML responsibility "Incident handling"; DE-97 (blocked riders can report) |
| R2 The incident type is detected from the category | CustomerSupport | canvas UL "Incident type … detected from the category (DE-50)"; routing is CustomerSupport's key question |
| R3 A fleet issue makes its bike broken at once and hands it over to technicians | CustomerSupport (detect + hand over) / FleetMaintenance (bike broken, withdrawn) | CustomerSupport decides "this is a fleet issue" and publishes the fact; "broken" is FleetMaintenance's language and state (CML FleetMaintenance "Broken bikes"), withdrawal is Rentals' (CML Rentals "Bike availability"). The downstream half is plan `fleet-operations-takes-broken-bike-out-of-circulation` (its R2) |
| Command: Report Incident | CustomerSupport | issued by the rider app to CustomerSupport; no rule rejects it, CustomerSupport owns the incident it creates |
| Event: Incident Reported | CustomerSupport | CustomerSupport decides the incident and its type; no listener outside CustomerSupport |
| Event: Fleet Issue Reported | CustomerSupport | CustomerSupport decides the type *fleet issue*; FleetMaintenance listens (integration-contracts.md) |
| Query: none in the spec | n/a | the Incident query from integration-contracts.md is built here only as an observation point (D5) |

**Outcome:** one existing context (CustomerSupport, new module), publishing one integration event to an existing downstream.

## Context map changes

- None. `CustomerSupport [U,OHS,PL]->[D,ACL] FleetMaintenance : FleetIssueIncident` (DE-52) carries Fleet Issue Reported.

## Translation

| Spec term | Context | Context term | Where translated |
|---|---|---|---|
| Rider | CustomerSupport | Rider (`RiderId`) | REST adapter (`riderId` → `RiderId`) |
| Category *broken bike* / *can't unlock* / *wrongful charge* / *safety* / *other* | CustomerSupport | Category `BROKEN_BIKE` / `CANT_UNLOCK` / `WRONGFUL_CHARGE` / `SAFETY` / `OTHER` | REST adapter (string → category; unknown → 400) |
| Incident type *fleet issue* / *billing dispute* / *safety concern* / *other* | CustomerSupport | Incident type `FLEET_ISSUE` / `BILLING_DISPUTE` / `SAFETY_CONCERN` / `OTHER` | canvas UL; REST response `type` |
| Bike | CustomerSupport | bike reference (`BikeId`), not checked against Rentals | REST adapter |
| Open | CustomerSupport | Open (reported, no assigned agent) | canvas UL "Assigned agent" (add "Open", see Canvas changes) |
| Fleet issue handed over to technicians | CustomerSupport → FleetMaintenance | published language `FleetIssueReported {incidentId, bikeId, description, reportedAt}` | `api/events` mapping in CustomerSupport; FleetMaintenance's ACL listener translates into its own language (other plan) |
| Broken | FleetMaintenance | Broken bike | FleetMaintenance (other plan) |

## Contract mapping

| Spec element | Context | Code |
|---|---|---|
| Command: Report Incident | CustomerSupport | `ReportIncidentUseCase` + record `ReportIncident(riderId, category, description, bikeId?, reportedAt)` in `customersupport/api/usecases`; `POST /customer-support/incidents` `{riderId, category, description, bikeId?}` → `201 {incidentId, type}` |
| Event: Incident Reported | CustomerSupport | domain event `IncidentReported(incidentId, riderId, category, type, description, bikeId?, reportedAt)` in `DomainEvents`, published by the use case through `DomainEventPublisher`; no integration event (no listener outside CustomerSupport) |
| Event: Fleet Issue Reported | CustomerSupport | domain event `FleetIssueReported` (domain package) mapped in `infra` to the published-language record `customersupport.api.events.FleetIssueReported {incidentId, bikeId, description, reportedAt}` (exists) and published through `ApplicationEventPublisher`; listener in FleetMaintenance (plan `fleet-operations-takes-broken-bike-out-of-circulation`) |
| Query: none in the spec (observation point) | CustomerSupport | `IncidentQuery(incidentId)` + `IncidentQueryHandler` in `api/queries`; `GET /customer-support/incidents/{incidentId}` → `{incidentId, riderId, type, status, assignedAgentId?, team?, resolution?}`, status one of `OPEN`, `PICKED_UP`, `ACCEPTED`, `REJECTED`, `ESCALATED`, `REOPENED`, `RESOLVED`; `404` for an unknown incident |

Secondary ports: `Incidents` (repository), in-memory adapter in `infra/adapters/secondary/persistence`; incident IDs generated by the use case (UUID strings; the spec's I-n are labels).

## Canvas changes

### CustomerSupport (`docs/bounded-contexts/velocity/customersupport/canvas.md`)

Already in the canvas, tag only:
- "A broken-bike report is a fleet issue handed to Fleet Maintenance" ← E3.1 (and E3.3: the canvas' FleetMaintenance side carries "the ride goes on", DE-86).
- "A wrongful-charge report is a billing dispute" ← E1.1, E3.2 (E3.2 names a bike; the type still follows the category).

Missing, add:
- **Ubiquitous language, add:** **Open**: reported and not yet picked up by any support agent.
- **Business decisions, add:**
  - `The incident type follows the category (spec: rider-reports-incident#E2.1, #E2.2, #E2.3)`: *can't unlock* for B-22 → *fleet issue* (Fleet Issue Reported); *safety* → *safety concern*; *broken bike* without a bike → *other*, nothing handed over (DE-50).
  - `A blocked rider can report an incident (spec: rider-reports-incident#E1.2)`: Given Ben's account is blocked for *outstanding debt*, When Ben reports *other* with "the app shows a ride I didn't take", Then incident I-8 is reported for Ben with type *other* (Incident Reported) (DE-97).

## Decisions

| # | Decision | Choice | Reason |
|---|---|---|---|
| D1 | TDD checkpoint loop | **off** | the trainer explicitly asked for a fully autonomous run (2026-11-08); the tactical skill's pushback is recorded in `docs/bounded-contexts/velocity/conversation.md`. Domain still built test-first, one example per cycle |
| D2 | Aggregate style | state-based | simplest; no requirement for an event history |
| D3 | ACLs | one per outbound query/command, in `@AntiCorruptionLayer` packages, calling REST via `RestClient` + `VelocityServiceUrls`; inbound events via `@Async @EventListener` ACL listeners | integration-contracts.md "Integration style" (this intent has no outbound call; it publishes) |
| D4 | Persistence | in-memory repositories with `Version` optimistic locking, after the domain is green | memory "aggregate persistence after domain"; repo convention |
| D5 | Observation point | build `GET /customer-support/incidents/{incidentId}` here, although the spec has no query | every example's Then is incident state; later CustomerSupport plans extend it |
| D6 | No check of the bike or the rider | CustomerSupport doesn't ask Rentals whether the bike exists, nor Accounts about the rider | CML has no Rentals→CustomerSupport or Accounts→CustomerSupport relationship; DE-97 lets blocked riders report; a wrong bike ID is FleetMaintenance's problem |
| D7 | Integration-event assertion in tests | a test-only synchronous `@EventListener` recorder for `customersupport.api.events.FleetIssueReported` | the event is published inside the request; a recorder sees it before the `201`, so absence (E2.3, E3.2) is deterministic |

## Steps

### S1: Update the CustomerSupport canvas
- **Status:** done
- **Context:** CustomerSupport
- **Skill:** none (apply the approved canvas diff above)
- **Covers:** E1.2, E2.1, E2.2, E2.3 (tags for E1.1, E3.1, E3.2, E3.3)
- **Done when:** canvas contains "Open" and the two new decisions, tagged.

### S2: Scaffold the CustomerSupport module (6a)
- **Status:** done
- **Context:** CustomerSupport
- **Skill:** bounded-context-scaffolder (6a), told that this plan settles the structural decisions and D1
- **Covers:** Report Incident (api record), Fleet Issue Reported (published language)
- **Done when:** `customersupport/package-info.java` with `@BoundedContext(name = "CustomerSupport")` + `@Module`; record `ReportIncident`; `IncidentQuery`; the existing `api/events/FleetIssueReported` kept; `CustomerSupportArchitectureTest` green; `./mvnw test` green.

### S3: Reporting and type detection, test-first
- **Status:** done
- **Context:** CustomerSupport
- **Skill:** ddd-tactical-building-blocks (D1: loop off; one spec example per cycle, verbatim)
- **Covers:** R1, R2, R3, E1.1, E1.2, E2.1, E2.2, E2.3, E3.1, E3.2, E3.3, Incident Reported, Fleet Issue Reported
- **Done when:** one domain test per example for CustomerSupport's part (type and the Fleet Issue Reported event, or its absence); `./mvnw test` green.

### S4: Use case, REST, query, published language, wiring (6b)
- **Status:** done
- **Context:** CustomerSupport
- **Skill:** bounded-context-scaffolder (6b)
- **Covers:** Report Incident, Incident Reported, Fleet Issue Reported
- **Done when:** `ReportIncidentUseCase`, `POST /customer-support/incidents`, `IncidentQueryHandler` + `GET /customer-support/incidents/{incidentId}`, mapping of the domain `FleetIssueReported` to `api/events/FleetIssueReported`, `infra/config/CustomerSupportConfig` with the `velocityClock`; then the in-memory `Incidents` adapter with `Version` (D4); `./mvnw test` green.

### S5: Acceptance tests inside CustomerSupport
- **Status:** done
- **Context:** CustomerSupport
- **Skill:** none (implementation-planner)
- **Covers:** E1.1, E1.2, E2.1, E2.2, E2.3, E3.2
- **Done when:** `src/test/java/pl/wojtyna/dddtrainingv3/velocity/customersupport/RiderReportsIncidentSpecTest.java`, `@VelocitySpringTest`, one method per example over REST; Fleet Issue Reported (E2.1) and its absence (E2.3, E3.2) asserted with the recorder (D7); E1.2's blocked rider is arranged only as a label (CustomerSupport doesn't read blocks, D6); `./mvnw test` green.

### S6: Cross-context acceptance for fleet issues
- **Status:** done
- **Context:** CustomerSupport → FleetMaintenance → Rentals
- **Skill:** none (implementation-planner)
- **Covers:** E3.1, E3.3
- **Done when:** in the same `RiderReportsIncidentSpecTest`, E3.1 asserts with Awaitility that `GET /rentals/bikes/{bikeId}` shows `WITHDRAWN` and a rider's `POST /rentals/bikes/{bikeId}/reservations` is rejected with `409`; E3.3 asserts B-17 is withdrawn while `GET /rentals/riders/{riderId}/active-rental` still shows Anna's `SESSION`. Prerequisites: plan `fleet-operations-takes-broken-bike-out-of-circulation` (its FleetIssueReported listener and withdrawal steps), plans `fleet-manager-adds-bike-to-fleet`, `rider-reserves-bike` and `rider-unlocks-reserved-bike` (to arrange B-17 and Anna's ride); until they're delivered both methods exist and are `@Disabled("needs <plan>")`. `./mvnw test` green.

## Acceptance tests

| Example | Test |
|---|---|
| E1.1 | `RiderReportsIncidentSpecTest#e1_1_wrongful_charge_report_is_open_billing_dispute` |
| E1.2 | `RiderReportsIncidentSpecTest#e1_2_blocked_rider_can_report_incident` |
| E2.1 | `RiderReportsIncidentSpecTest#e2_1_cant_unlock_with_bike_is_fleet_issue` |
| E2.2 | `RiderReportsIncidentSpecTest#e2_2_safety_report_is_safety_concern` |
| E2.3 | `RiderReportsIncidentSpecTest#e2_3_broken_bike_without_bike_is_other_and_not_handed_over` |
| E3.1 | `RiderReportsIncidentSpecTest#e3_1_fleet_issue_makes_bike_broken_and_unreservable` (Customer Support half green; Rentals half in S6) |
| E3.2 | `RiderReportsIncidentSpecTest#e3_2_wrongful_charge_naming_bike_leaves_bike_available` |
| E3.3 | `RiderReportsIncidentSpecTest#e3_3_fleet_issue_during_ride_breaks_bike_but_ride_goes_on` (Customer Support half green; Rentals half in S6) |

## Traceability

| Spec ID | Step | Files |
|---|---|---|
| R1 / E1.1 | S1 (tag), S3, S4, S5 | `domain/incident/Incident.java`, `IncidentType.java`, `ReportIncidentUseCase`, `IncidentController`, `IntegrationEventsPublisher`; tests `IncidentTest.Reporting`, `RiderReportsIncidentSpecTest` |
| R1 / E1.2 | S1, S3, S5 | `domain/incident/Incident.java`, `IncidentType.java`, `ReportIncidentUseCase`, `IncidentController`, `IntegrationEventsPublisher`; tests `IncidentTest.Reporting`, `RiderReportsIncidentSpecTest` |
| R2 / E2.1 | S1, S3, S4, S5 | `domain/incident/Incident.java`, `IncidentType.java`, `ReportIncidentUseCase`, `IncidentController`, `IntegrationEventsPublisher`; tests `IncidentTest.Reporting`, `RiderReportsIncidentSpecTest` |
| R2 / E2.2 | S1, S3, S5 | `domain/incident/Incident.java`, `IncidentType.java`, `ReportIncidentUseCase`, `IncidentController`, `IntegrationEventsPublisher`; tests `IncidentTest.Reporting`, `RiderReportsIncidentSpecTest` |
| R2 / E2.3 | S1, S3, S5 | `domain/incident/Incident.java`, `IncidentType.java`, `ReportIncidentUseCase`, `IncidentController`, `IntegrationEventsPublisher`; tests `IncidentTest.Reporting`, `RiderReportsIncidentSpecTest` |
| R3 / E3.1 | S1 (tag), S3, S4, S6 | `domain/incident/Incident.java`, `IncidentType.java`, `ReportIncidentUseCase`, `IncidentController`, `IntegrationEventsPublisher`; tests `IncidentTest.Reporting`, `RiderReportsIncidentSpecTest` |
| R3 / E3.2 | S1 (tag), S3, S5 | `domain/incident/Incident.java`, `IncidentType.java`, `ReportIncidentUseCase`, `IncidentController`, `IntegrationEventsPublisher`; tests `IncidentTest.Reporting`, `RiderReportsIncidentSpecTest` |
| R3 / E3.3 | S1 (tag), S3, S6 | `domain/incident/Incident.java`, `IncidentType.java`, `ReportIncidentUseCase`, `IncidentController`, `IntegrationEventsPublisher`; tests `IncidentTest.Reporting`, `RiderReportsIncidentSpecTest` |

## Risks and open questions

- **Spec scope note:** E3.1 and E3.3 state outcomes owned by FleetMaintenance and Rentals ("B-17 is broken", "no rider can reserve it", "the ride goes on"), while the spec's Out of scope sends them to `fleet-operations-takes-broken-bike-out-of-circulation`. Planned as a cross-context acceptance step (S6) depending on that plan, not duplicated here.
- **E3.2 "B-17 stays available":** asserted as "no Fleet Issue Reported" in S5; checking Rentals' availability as well would need B-17 in Rentals (plan `fleet-manager-adds-bike-to-fleet`); added to S6's prerequisites if the reviewer wants it.
- **Unknown bike IDs** in a fleet-issue report aren't rejected (D6).
- **The canvas open question "Is a safety concern escalated automatically?"** is answered by DE-97 (no); the canvas still lists it.
- No differences from the reference context map.

## Review notes

- 2026-11-08: draft written (autonomous run); self-review against plan-critic.md checklist: AGREE (type detection and the hand-off in CustomerSupport per canvas and DE-50/52; "broken" left to FleetMaintenance per CML, so no Brain Context; integration direction matches `CustomerSupport [U,OHS,PL]->[D,ACL] FleetMaintenance`; all 8 examples covered; cross-context examples isolated in S6 with explicit prerequisites).
- 2026-11-08: built in the autonomous run (CustomerSupport worktree); decisions recorded in the changelog.
- 2026-11-08 (Fleet Maintenance wave, autonomous run): S6 built. The TODOs in `RiderReportsIncidentSpecTest` are
  resolved: E3.1 adds B-17 to Rentals first, then awaits (Awaitility) `GET /rentals/bikes/{bikeId}` `WITHDRAWN` and
  checks a rider's reservation is rejected with `409`; E3.3 starts Anna's ride first, then awaits B-17 `WITHDRAWN`
  while her active rental is still a `SESSION`. The tests were never `@Disabled` (they asserted Customer Support's half
  until now), so no re-enabling was needed.

## Changelog

### 2026-11-08: planned
- Built from spec dated 2026-11-08.

### 2026-11-08: built (autonomous run)
- S1-S5 done: canvas updated; CustomerSupport module scaffolded (`src/main/java/pl/wojtyna/dddtrainingv3/velocity/customersupport`, plan `docs/bounded-contexts/velocity/customersupport/plan.md`); `Incident` aggregate built test-first (8 domain tests, `IncidentTest.Reporting`); use case, REST, Incident query, `FleetIssueReported` publication, wiring; `RiderReportsIncidentSpecTest` green.
- D7 implemented as a test-only `ApplicationListener` added to the shared context by `CustomerSupportTestApi` (no extra test configuration, so the cached context is reused).
- S6 left todo (needs Fleet Maintenance and Rentals): `e3_1_…` and `e3_3_…` exist and are green on Customer Support's half (Fleet Issue Reported published, asserted with Awaitility); the Rentals assertions are TODOs in the test.
- Added queries beyond the plan, from integration-contracts.md "Additions": `GET /customer-support/incidents?riderId=`.

### 2026-11-08: Fleet Maintenance wave (autonomous run)
- S6 done: cross-context acceptance of E3.1/E3.3 (Customer Support → Fleet Maintenance → Rentals) in
  `src/test/java/pl/wojtyna/dddtrainingv3/velocity/customersupport/RiderReportsIncidentSpecTest.java`; full suite green.
