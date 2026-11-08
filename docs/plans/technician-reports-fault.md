---
name: technician-reports-fault
intent: technician-reports-fault
spec: technician-reports-fault
system: velocity
status: done
date: 2026-11-08
spec_date: 2026-11-08
---

# Implementation plan: A technician wants to report a bike broken during a routine check

<!-- Reviewers: edit this file or answer in the conversation, then set status to "approved" (or say so).
     Invoking implementation-planner again resumes from this file. -->

## Summary

The intent lands in **FleetMaintenance**: a technician's fault report is one more fault source of the broken-bike
register (canvas inbound "Technician app / Report Fault", DE-62). The withdrawal from riders reuses Rentals'
*Withdraw Bike* on request, built by `fleet-operations-takes-broken-bike-out-of-circulation` (S3, S4), which already
cancels a rider's reservation (R2) and never interrupts a ride (E1.3). No context map change. Order: FM canvas tag →
FM rule (technician fault source, description) → use case and REST → acceptance tests.

## Placement

| Spec element | Owner | Why |
|---|---|---|
| R1 A technician's fault report makes the bike broken and withdraws it from riders at once | FleetMaintenance (broken + asking for the withdrawal); Rentals (performing the withdrawal) | FM owns the broken-bike register and the technician's commands (CML "Broken bikes"; canvas inbound "Report Fault", DE-62); Rentals owns availability (Rentals canvas "A client can withdraw a bike") |
| R2 A rider's reservation on the reported bike is cancelled | Rentals | Rentals holds reservations; the cancellation is part of Rentals' withdrawal (Rentals canvas "A client can withdraw a bike"), built in `fleet-operations-takes-broken-bike-out-of-circulation` S3 |
| R3 A fault report on a bike that is already broken changes nothing | FleetMaintenance | reads the broken-bike register (canvas decision "A duplicate fault report changes nothing") |
| Command: Report Fault | FleetMaintenance | the technician's command; FM's register decides (R3); no rule rejects it |
| Event: Bike Reported Broken | FleetMaintenance | the fact happens in FM |
| Event: Bike Withdrawn | Rentals | the fact happens in Rentals (`rentals.api.events.BikeWithdrawn`, reason `REQUESTED`) |

**Outcome:** several existing contexts (FleetMaintenance owns the intent; Rentals' existing *Withdraw Bike* is reused).

Hand-off: FleetMaintenance (D, ACL) → Rentals OHS *Withdraw Bike* (`POST /rentals/bikes/{bikeId}/withdrawal`), the
port `BikeWithdrawals` from the first FM plan. Rentals' resulting `BikeWithdrawn` (`REQUESTED`) is ignored by FM (D7
of that plan).

## Context map changes

- None.

## Translation

| Spec term | Context | Context term | Where translated |
|---|---|---|---|
| Fault report | FleetMaintenance | Fault report with fault source `TECHNICIAN` and a description | REST adapter (`technicianId`, `description`) |
| Technician | FleetMaintenance | Technician (`TechnicianId`) | REST adapter |
| Broken bike | Rentals | Withdrawn bike (reason *requested*) | FM `rentalsctxacl` adapter |
| Routine check | – | not modelled (spec vocabulary) | – |

## Contract mapping

| Spec element | Context | Code |
|---|---|---|
| Command: Report Fault | FleetMaintenance | `ReportFaultUseCase` + record `ReportFault(bikeId, technicianId, description)`; `POST /fleet-maintenance/bikes/{bikeId}/fault-reports` `{technicianId, description}` (400 blank description, 404 unknown bike as answered by Rentals) |
| Event: Bike Reported Broken | FleetMaintenance | domain event `BikeReportedBroken` (fault source `TECHNICIAN`, technician, description); no integration event |
| Event: Bike Withdrawn | Rentals | domain `BikeWithdrawn` → `rentals.api.events.BikeWithdrawn` (`REQUESTED`), from `fleet-operations-takes-broken-bike-out-of-circulation` S4; FM side: port `BikeWithdrawals`, adapter `infra/adapters/secondary/rentalsctxacl/RentalsBikeWithdrawalsAdapter` |

## Canvas changes

### FleetMaintenance (`docs/bounded-contexts/velocity/fleetmaintenance/canvas.md`)

- **Business decisions, add:** *A technician's fault report makes the bike broken and withdraws it*: **Given** B-22 is available at Central and isn't broken, **When** technician Tom reports a fault on B-22 ("loose handlebar"), **Then** B-22 is broken with fault source *technician* (Bike Reported Broken) and Rentals is asked to withdraw it (spec: technician-reports-fault#E1.1, DE-62).
- **Business decisions, tag:** "A duplicate fault report changes nothing" → `(spec: technician-reports-fault#E3.1)`.
- None beyond tagging for E1.2, E1.3, E2.1: they are Rentals behaviour already in the Rentals canvas ("Only a technician can reserve a withdrawn bike", "A client can withdraw a bike" and its *During a ride* line added by the first FM plan).

## Decisions

| # | Decision | Choice | Reason |
|---|---|---|---|
| D1 | TDD checkpoint loop | **off** | the trainer explicitly asked for a fully autonomous run (2026-11-08); the tactical skill's pushback is recorded in `docs/bounded-contexts/velocity/conversation.md`. Domain still built test-first, one example per cycle |
| D2 | Aggregate style | state-based | simplest; no requirement for an event history |
| D3 | ACLs | one per outbound query/command, in `@AntiCorruptionLayer` packages, calling REST via `RestClient` + `VelocityServiceUrls`; inbound events via `@Async @EventListener` ACL listeners | integration-contracts.md, CML `[D,ACL]` |
| D4 | Persistence | in-memory repositories with `Version` optimistic locking, after the domain is green | trainer's standing instruction |
| D5 | Unknown bike (spec Data: "a bike known to VeloCity") | the use case asks Rentals to withdraw first; Rentals' 404 rejects the report as `UnknownBike` (404) and nothing is recorded | FM keeps no fleet register; Rentals knows the fleet. Differs from D6 of the first FM plan (event-driven, record first) because here a technician waits for the answer |
| D6 | Description on an already broken bike | not recorded (E3.1), spec Q3 left open | spec example is explicit; Q3 is a later refinement |

## Steps

### S1: Canvas update for FleetMaintenance
- **Status:** done
- **Context:** FleetMaintenance
- **Skill:** none (apply the "Canvas changes" diff exactly)
- **Prerequisite:** `fleet-operations-takes-broken-bike-out-of-circulation` delivered (FM context, `BikeWithdrawals`, Rentals *Withdraw Bike*).
- **Covers:** E1.1, E3.1 (canvas only)
- **Done when:** the FM canvas carries the addition and tag.

### S2: FleetMaintenance: technician fault reports
- **Status:** done
- **Context:** FleetMaintenance
- **Skill:** ddd-tactical-building-blocks (checkpoint loop off, D1), via bounded-context-scaffolder update mode
- **Covers:** R1, R3, E1.1, E3.1
- **Done when:** one domain test per example (technician report → broken with source *technician* and description, withdrawal asked for; report on an already broken bike → nothing, source stays *lock failed*); `./mvnw test` green.

### S3: FleetMaintenance: Report Fault use case and REST (6b)
- **Status:** done
- **Context:** FleetMaintenance
- **Skill:** bounded-context-scaffolder (6b)
- **Covers:** Report Fault, Bike Reported Broken, Bike Withdrawn
- **Done when:** `ReportFaultUseCase` + `ReportFault`, `POST /fleet-maintenance/bikes/{bikeId}/fault-reports` (400 blank description, 404 unknown bike, D5), wiring; `./mvnw test` green.

### S4: Acceptance tests for the spec examples
- **Status:** done
- **Context:** FleetMaintenance, Rentals
- **Skill:** none (this skill)
- **Prerequisite:** `rider-reserves-bike` (E1.2), `rider-unlocks-reserved-bike` (session for E1.3) delivered.
- **Covers:** all examples
- **Done when:** `TechnicianReportsFaultSpecTest` (`@VelocitySpringTest`, real beans, `MutableClock`) has one test per example; the FM → Rentals withdrawal (and the cancelled reservation in E2.1) is asserted on `GET /rentals/bikes/{bikeId}` with Awaitility; `./mvnw test` green.

## Acceptance tests

| Example | Test |
|---|---|
| E1.1 | `TechnicianReportsFaultSpecTest#e1_1_fault_report_makes_bike_broken_and_withdrawn` |
| E1.2 | `TechnicianReportsFaultSpecTest#e1_2_rider_cannot_reserve_reported_bike` |
| E1.3 | `TechnicianReportsFaultSpecTest#e1_3_fault_report_during_ride_lets_ride_go_on` |
| E2.1 | `TechnicianReportsFaultSpecTest#e2_1_fault_report_cancels_riders_reservation` |
| E3.1 | `TechnicianReportsFaultSpecTest#e3_1_fault_report_on_broken_bike_changes_nothing` |

## Traceability

| Spec ID | Step | Files |
|---|---|---|
| R1 / E1.1 | S1, S2, S3, S4 | FleetMaintenance: `src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/domain/bike/Bike.java` (`reportFault`), `src/test/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/domain/bike/FaultReportTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/api/usecases/ReportFaultUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/infra/adapters/primary/FleetMaintenanceController.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/infra/adapters/secondary/rentalsctxacl/RentalsBikeWithdrawalsAdapter.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/TechnicianReportsFaultSpecTest.java` |
| R1 / E1.2 | S4 (Rentals rule from `rider-reserves-bike`) | FleetMaintenance: `src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/domain/bike/Bike.java` (`reportFault`), `src/test/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/domain/bike/FaultReportTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/api/usecases/ReportFaultUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/infra/adapters/primary/FleetMaintenanceController.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/infra/adapters/secondary/rentalsctxacl/RentalsBikeWithdrawalsAdapter.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/TechnicianReportsFaultSpecTest.java` |
| R1 / E1.3 | S4 (Rentals rule from `fleet-operations-takes-broken-bike-out-of-circulation` S3) | FleetMaintenance: `src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/domain/bike/Bike.java` (`reportFault`), `src/test/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/domain/bike/FaultReportTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/api/usecases/ReportFaultUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/infra/adapters/primary/FleetMaintenanceController.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/infra/adapters/secondary/rentalsctxacl/RentalsBikeWithdrawalsAdapter.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/TechnicianReportsFaultSpecTest.java` |
| R2 / E2.1 | S4 (Rentals rule from `fleet-operations-takes-broken-bike-out-of-circulation` S3) | FleetMaintenance: `src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/domain/bike/Bike.java` (`reportFault`), `src/test/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/domain/bike/FaultReportTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/api/usecases/ReportFaultUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/infra/adapters/primary/FleetMaintenanceController.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/infra/adapters/secondary/rentalsctxacl/RentalsBikeWithdrawalsAdapter.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/TechnicianReportsFaultSpecTest.java` |
| R3 / E3.1 | S1, S2, S4 | FleetMaintenance: `src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/domain/bike/Bike.java` (`reportFault`), `src/test/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/domain/bike/FaultReportTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/api/usecases/ReportFaultUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/infra/adapters/primary/FleetMaintenanceController.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/infra/adapters/secondary/rentalsctxacl/RentalsBikeWithdrawalsAdapter.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/TechnicianReportsFaultSpecTest.java` |

## Risks and open questions

- Spec Q3 (add the description to an already broken bike) is open; the plan follows E3.1 (nothing recorded). Affects S2 if answered differently.
- The spec's Data constraint "a bike known to VeloCity" has no example; D5 handles it through Rentals' 404 and gets a REST-level test in S3, not an acceptance test.
- CONTRACT GAP (shared with the first FM plan): `GET /fleet-maintenance/bikes/{bikeId}` needs `faultSource`, `reportedAt`, `faultDescription?` to assert E1.1 and E3.1.

## Review notes

- 2026-11-08: draft written (autonomous run); self-review against plan-critic.md checklist: AGREE (FM owns the command and the register; withdrawal reuses an existing Rentals command in the map's direction; every example covered; no building blocks decided).
- 2026-11-08 (Fleet Maintenance wave, autonomous run): FleetMaintenance steps built; decisions:
  - D5 as planned: `ReportFaultUseCase` asks Rentals to withdraw before saving; Rentals' `404` → `404 UnknownBike`, nothing recorded (REST test `fault_report_on_unknown_bike_is_rejected`). A blank description → `400`. `POST .../fault-reports` answers `200` with an empty body (the contract names no answer).

## Changelog

### 2026-11-08: planned
- Built from spec dated 2026-11-08.

### 2026-11-08: Fleet Maintenance wave (autonomous run)
- FleetMaintenance steps built test-first (TDD checkpoint loop off, D1): domain tests `FaultReportTest (7, of which 2 for this spec)`, red seen before
  green; per-cycle log in `docs/bounded-contexts/velocity/fleetmaintenance/plan.md`. Canvas diff applied to the
  FleetMaintenance canvas. One acceptance test per example (Acceptance tests table), every cross-context hand-off
  asserted with Awaitility; full suite green.
