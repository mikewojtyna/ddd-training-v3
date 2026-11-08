# CustomerSupport bounded context: implementation plan

- **System / package**: `velocity.customersupport`
- **Mode**: scaffold
- **Source canvas**: `docs/bounded-contexts/velocity/customersupport/canvas.md`
- **Last updated**: 2026-11-08
- **Status**: implemented

## Review notes
- 2026-11-08: approved via docs/plans/rider-reports-incident.md, docs/plans/support-agent-decides-on-incident.md,
  docs/plans/support-agent-escalates-incident.md, docs/plans/rider-reopens-rejected-incident.md,
  docs/plans/support-agent-resolves-incident.md and docs/plans/technician-returns-fixed-bike-to-circulation.md
  (Customer Support steps S4, S5) (autonomous run, 2026-11-08).

## Canvas summary
Supporting, engagement-driven, custom-built Execution Model: every incident a rider reports is routed by its type,
worked by one support agent and driven to resolution. Fleet issues are handed to Fleet Maintenance at once.

## Decisions
| Decision | Choice | Reason |
|---|---|---|
| Modules | 1 module = 1 context | default |
| ACL towards other contexts | none needed: Customer Support only offers an OHS and publishes `FleetIssueReported` | CML `CustomerSupport [U,OHS,PL]->[D,ACL] FleetMaintenance`; no outbound calls (plan D6 of rider-reports-incident: rider and bike not checked) |
| Boundary exceptions | none | |
| Domain layer | delegated to `ddd-tactical-building-blocks` (always) | |
| TDD checkpoint loop | off (trainer's explicit autonomy request, 2026-11-08; D1 of every plan) | still test-first, one example per test with red seen before green |
| Aggregate style | state-based (`Incident`), memento `Snapshot` for persistence | plans D2 |
| Read side | `InMemoryIncidentReadModel`, a projection fed synchronously by the incident domain events | keeps fields no invariant needs (category, description, reported at, rejection reason) out of the aggregate |
| Reopening window zone | `velocity.customer-support.zone`, default `Europe/Warsaw`, bean `customerSupportCityZone` handed to `RejectIncidentUseCase` → `ReopeningWindow` | plan rider-reopens-rejected-incident D5 |
| Error names | exception class names equal the plans' error codes (`ErrorResponse.of`) | `NotAssignedAgent`, `IncidentAlreadyAssigned`, ... |

## Mapping: canvas → code
### Inbound
| Collaborator | Type | Message | Code |
|---|---|---|---|
| Rider app | Command | Report Incident | `ReportIncidentUseCase`, `POST /customer-support/incidents` → `201 {incidentId, type}` |
| Support agent | Command | Pick Up Incident | `PickUpIncidentUseCase`, `POST …/{id}/assignment` |
| Support agent | Command | Accept / Reject Incident | `AcceptIncidentUseCase`, `RejectIncidentUseCase`, `POST …/{id}/acceptance` / `rejection` |
| Support agent | Command | Escalate Incident | `EscalateIncidentUseCase`, `POST …/{id}/escalation` (`400 UnknownTeam`) |
| Support agent | Command | Resolve Incident | `ResolveIncidentUseCase`, `POST …/{id}/resolution` |
| Rider app | Command | Reopen Incident | `ReopenIncidentUseCase`, `POST …/{id}/reopening` |
| Fleet Maintenance | Command | Resolve Fleet Issue | `ResolveFleetIssueUseCase`, `POST …/{id}/fleet-resolution` `{bikeId}` (idempotent) |
| Support (clock) | Command | Resolve Uncontested Rejections | `ResolveUncontestedRejectionsUseCase`, `infra/adapters/primary/scheduling/UncontestedRejectionsJob` |
| Rider app, Support agent | Query | Incident | `IncidentQueryHandler`, `GET …/{id}[?riderId=]` (404 for another rider) |
| Support agent | Query | Open Incidents | `OpenIncidentsQueryHandler`, `GET /customer-support/incidents?status=OPEN` |
| Rider app | Query | Rider's incidents | `RiderIncidentsQueryHandler`, `GET /customer-support/incidents?riderId=` |

### Outbound
| Message | Type | Collaborator | Port | Adapter (ACL package) |
|---|---|---|---|---|
| Fleet Issue Reported | Event | Fleet Maintenance | domain `IncidentEvent.FleetIssueReported` | `infra/adapters/secondary/IntegrationEventsPublisher` → `api.events.FleetIssueReported` (PL, no ACL) |
| Incident Reported/Picked Up/Accepted/Rejected/Escalated/Reopened/Resolved | Event | none outside | `IncidentEvent` | consumed only by the read model |

### Ubiquitous language
| Term | Code type | Where it appears |
|---|---|---|
| Incident | `Incident` (aggregate root), `IncidentId` | domain, api records |
| Category | `Category` | domain, `ReportIncident.category` |
| Incident type | `IncidentType` (`detect`) | domain, `IncidentReportReceipt.type`, `IncidentView.type` |
| Open, picked up, accepted, rejected, escalated, reopened, resolved | `IncidentStatus` | domain, `IncidentView.status` |
| Assigned agent | `AgentId`, `Incident.assignedAgent()` | domain, `IncidentView.assignedAgentId` |
| Team | `Team` | domain, `EscalateIncident.team` |
| Reopenable until | `ReopeningWindow` | domain, `IncidentView.reopenableUntil` |
| Resolution | `Resolution` (`REJECTION_NOT_CONTESTED`, `BIKE_FIXED`) | domain, `IncidentView.resolution` |

### Business rules → delegated to `ddd-tactical-building-blocks`
| Rule (GWT) | Status | Building block | Test |
|---|---|---|---|
| Incident type follows the category (DE-50) | green | `Incident.report`, `IncidentType.detect` | `IncidentTest.Reporting` (8 tests) |
| Picked up once; only the assigned agent decides | green | `Incident` | `IncidentTest.PickingUp` (4) |
| Accept / reject with reason; accepted not decided again | green | `Incident` | `IncidentTest.Deciding` (3), `DecidingReopened#acceptedIncidentCannotBeAcceptedAgain` |
| Reopened can be accepted, not rejected again | green | `Incident` | `IncidentTest.DecidingReopened` |
| Escalation: accepted only, once, any of three teams, assigned agent only | green | `Incident`, `Team` | `IncidentTest.Escalating` (6) |
| Reopen within 14 calendar days, reporting rider, rejected only | green | `Incident`, `ReopeningWindow` | `IncidentTest.Reopening` (5), `ReopeningWindowTest` (2) |
| Resolve by assigned agent from accepted/escalated/reopened, once | green | `Incident` | `IncidentTest.Resolving` (7 of 10) |
| Uncontested rejections resolved when the window closes | green | `Incident.resolveIfUncontestedAt` | `IncidentTest.Resolving` (3 of 10) |
| Fixed bike resolves its fleet issues from any open state | green | `Incident.resolveFleetIssueOf` | `IncidentTest.ResolvingFleetIssues` (5) |

## Differences from the reference context map
| Canvas says | Reference map says | Covered by "Accepted Alternatives"? |
|---|---|---|
| none | | |

## Files
```
src/main/java/pl/wojtyna/dddtrainingv3/velocity/customersupport/
  package-info.java
  api/events/FleetIssueReported.java                 (existed)
  api/usecases/  ReportIncident(+UseCase), IncidentReportReceipt, PickUpIncident(+UseCase), AcceptIncident(+UseCase),
                 RejectIncident(+UseCase), EscalateIncident(+UseCase), ResolveIncident(+UseCase),
                 ReopenIncident(+UseCase), ResolveFleetIssue(+UseCase), ResolveUncontestedRejections(+UseCase)
  api/queries/   IncidentQuery, IncidentView, IncidentQueryHandler, OpenIncidentsQuery, OpenIncidentView,
                 OpenIncidentsQueryHandler, RiderIncidentsQuery, RiderIncidentsQueryHandler, IncidentReadModel
  domain/incident/ Incident, IncidentEvent, Incidents, IncidentId, RiderId, AgentId, BikeId, Description,
                 RejectionReason, Resolution, Category, IncidentType, IncidentStatus, Team, IncidentReport,
                 ReopeningWindow, FluentLanguage, IncidentNotFound + rule exceptions (IncidentAlreadyAssigned,
                 IncidentAlreadyResolved, NotAssignedAgent, IncidentAlreadyDecided, ReopenedIncidentCannotBeRejected,
                 IncidentNotAccepted, NotReportingRider, IncidentNotRejected, ReopeningWindowClosed,
                 IncidentNotResolvable, NotAFleetIssueOfBike)
  infra/adapters/primary/ IncidentController, IncidentRequests, scheduling/UncontestedRejectionsJob
  infra/adapters/secondary/ IntegrationEventsPublisher, persistence/InMemoryIncidents, persistence/InMemoryIncidentReadModel
  infra/config/CustomerSupportContextConfig.java
src/test/java/pl/wojtyna/dddtrainingv3/velocity/customersupport/
  CustomerSupportArchitectureTest, CustomerSupportTestApi, RiderReportsIncidentSpecTest,
  SupportAgentDecidesOnIncidentSpecTest, SupportAgentEscalatesIncidentSpecTest, RiderReopensRejectedIncidentSpecTest,
  SupportAgentResolvesIncidentSpecTest, ResolveFleetIssueRestTest,
  domain/incident/IncidentTest, domain/incident/ReopeningWindowTest,
  infra/adapters/secondary/persistence/InMemoryIncidentsTest
```

## Stubs and follow-ups
- No stubs. Fleet Maintenance listens to `FleetIssueReported` and calls *Resolve Fleet Issue* (other plans).
- Cross-context acceptance of rider-reports-incident E3.1/E3.3 (its S6) waits for Fleet Maintenance and Rentals;
  the tests currently assert Customer Support's half (FleetIssueReported published) and carry a TODO.
- No CML change.

## Changelog
### 2026-11-08: scaffolded and implemented
- Added: the whole module listed under Files.
- Triggered by: intents rider-reports-incident, support-agent-decides-on-incident, support-agent-escalates-incident,
  rider-reopens-rejected-incident, support-agent-resolves-incident, technician-returns-fixed-bike-to-circulation (S4, S5).
- Decision: the decide plan's "minimal reopened state" (its S4) was not needed: Reopen was built before E2.3/E4.2,
  so those tests use the real reopening (reopen plan D6).
- Decision: derived tests beyond the spec examples, each guarding a hole the state machine would otherwise have:
  `acceptedIncidentCannotBeAcceptedAgain` (R4), `rejectionIsStillContestableOnTheLastMinuteOfTheWindow` (D5 boundary),
  `onlyFleetIssueOfTheFixedBikeIsResolvedWhenBikeIsFixed` (409 `NotAFleetIssueOfBike` on fleet-resolution),
  `resolvedIncidentCannotBePickedUp` (409 `IncidentAlreadyResolved`).

#### TDD log (checkpoint loop off; cycles run one rule group at a time, each test's red observed before its implementation; all implemented by Claude)
| # | Scenario | Block | Test | Red seen | Green |
|---|---|---|---|---|---|
| 1-8 | reports E1.1, E1.2, E2.1, E2.2, E2.3, E3.1, E3.2, E3.3 | `Incident.report`, `IncidentType.detect` | `IncidentTest.Reporting#*` | yes, 8 assertion failures (no events) | yes |
| 9-12 | decides E1.1, E1.2, E3.1, E3.2 | `Incident.pickUpBy`, `acceptBy` | `IncidentTest.PickingUp#*` | yes, 4 assertion failures | yes |
| 13 | decides E2.1 | `acceptBy` | `assignedAgentAcceptsIncident` | no: passed at once (accept built in cycle 11-12), kept | yes |
| 14-15 | decides E2.2, E4.1 | `rejectBy` | `assignedAgentRejectsIncidentWithReason`, `acceptedIncidentCannotBeRejected` | yes | yes |
| 16-22 | reopens E1.1, E1.2, E2.1, E3.1, E3.2 + 2 window examples | `reopenBy`, `ReopeningWindow` | `IncidentTest.Reopening#*`, `ReopeningWindowTest#*` | yes, 7 failures | yes |
| 23 | decides E2.3 | `acceptBy` | `assignedAgentAcceptsReopenedIncident` | no: passed at once, kept | yes |
| 24-25 | decides E4.2, R4 accept twice | `rejectBy`, `acceptBy` | `reopenedIncidentCannotBeRejectedAgain`, `acceptedIncidentCannotBeAcceptedAgain` | yes | yes |
| 26-31 | escalates E1.1, E2.1, E2.2, E2.3, E3.1, E4.2 | `escalateBy` | `IncidentTest.Escalating#*` | yes, 6 failures | yes |
| 32-39 | resolves E1.1, E2.1, E3.1, E4.1, E5.1, E5.2, E5.3, E6.1 | `resolveBy`, `resolveIfUncontestedAt` | `IncidentTest.Resolving#*` | yes, 8 failures | yes |
| 40-41 | resolves 23:59 boundary, E6.2 | `resolveIfUncontestedAt` | `rejectionIsStillContestableOnTheLastMinuteOfTheWindow`, `reopenedIncidentIsNotResolvedByTheClock` | no: passed at once (empty body), kept | yes |
| 42-45 | fleet E3.1 (accepted, reported), wrong bike/type, resolved not picked up | `resolveFleetIssueOf`, `pickUpBy` | `IncidentTest.ResolvingFleetIssues#*` | yes, 4 failures | yes |
| 46 | fleet E3.2 | `resolveFleetIssueOf` | `alreadyResolvedFleetIssueKeepsItsResolution` | no: passed at once (empty body), kept | yes |
- Step 5 (after all domain tests green): `Version` + `Snapshot` on `Incident`, port `Incidents`, `InMemoryIncidents`
  with optimistic locking, `InMemoryIncidentsTest` (2 tests).
