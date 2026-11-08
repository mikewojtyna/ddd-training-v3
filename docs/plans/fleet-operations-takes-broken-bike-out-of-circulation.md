---
name: fleet-operations-takes-broken-bike-out-of-circulation
intent: fleet-operations-takes-broken-bike-out-of-circulation
spec: fleet-operations-takes-broken-bike-out-of-circulation
system: velocity
status: done
date: 2026-11-08
spec_date: 2026-11-08
---

# Implementation plan: Fleet operations wants every faulty bike known as broken and kept away from riders

<!-- Reviewers: edit this file or answer in the conversation, then set status to "approved" (or say so).
     Invoking implementation-planner again resumes from this file. -->

## Summary

The intent lands in **FleetMaintenance** (the broken-bike register: R1, R3, the "broken" half of R2) with a hand-off
to **Rentals** (the withdrawal itself: the "withdrawn" half of R2, and R4). FleetMaintenance listens to Rentals'
`BikeWithdrawn` and Customer Support's `FleetIssueReported` through ACL listeners, and asks Rentals to withdraw a
bike over Rentals' OHS (`POST /rentals/bikes/{bikeId}/withdrawal`). Both relationships already exist in the CML
(`Rentals [U,OHS,PL]->[D,ACL] FleetMaintenance`, `CustomerSupport [U,OHS,PL]->[D,ACL] FleetMaintenance`), so the
map doesn't change. This is the first FleetMaintenance plan: it removes the old FleetMaintenance code and builds the
context fresh from its canvas. Order: old code out → Rentals' *Withdraw Bike* on request (no Rentals intent covers
it) → FleetMaintenance scaffold, rules, use cases and listeners → acceptance tests.

## Placement

| Spec element | Owner | Why |
|---|---|---|
| R1 A bike withdrawn because of a detected fault becomes broken | FleetMaintenance | "broken" is FleetMaintenance's word and state (CML responsibility "Broken bikes"; canvas decision "A bike withdrawn by Rentals is registered broken"); Rentals only knows *withdrawn* (Rentals canvas purpose) |
| R2 A fleet-issue incident makes the bike broken and withdraws it from riders at once | FleetMaintenance (broken + asking for the withdrawal); Rentals (performing the withdrawal: cancelling a reservation, never interrupting a ride) | rule owner = who holds the state: broken-bike register in FM (canvas decision "A fleet-issue incident makes the bike broken and withdraws it"), bike holds in Rentals (Rentals canvas "A client can withdraw a bike", DE-86) |
| R3 A bike that is already broken isn't reported broken again; a new fleet-issue incident is only linked to it | FleetMaintenance | reads the broken-bike register (canvas decision "A duplicate fault report changes nothing", "Incident link") |
| R4 A broken bike can't be reserved by a rider | Rentals | "who can say no?": Rentals rejects a rider's reservation of a withdrawn bike (Rentals canvas "Only a technician can reserve a withdrawn bike"); delivered by `rider-reserves-bike` (its R5/E5.2), this plan only proves it end to end |
| Command: Report Bike Broken | FleetMaintenance | no rule rejects it; the register it changes is FM's (canvas inbound Bike Withdrawn / Fleet Issue Reported) |
| Command: Withdraw Bike | Rentals | Rentals owns availability (CML "Bike availability"; Rentals canvas inbound "Fleet Maintenance / Command / Withdraw Bike") |
| Command: Reserve Bike (rider) | Rentals | owns reservations (CML "Reserve and unlock"); planned in `rider-reserves-bike` |
| Event: Bike Reported Broken | FleetMaintenance | the fact happens in FM (canvas outbound, internal) |
| Event: Bike Withdrawn | Rentals | the fact happens in Rentals (published language `rentals.api.events.BikeWithdrawn`) |
| Event: Bike Reserved | Rentals | the fact happens in Rentals; planned in `rider-reserves-bike` |

**Outcome:** several existing contexts (FleetMaintenance, Rentals; Customer Support as the upstream of `FleetIssueReported`).

Hand-offs:
1. Rentals (U, OHS/PL) → FleetMaintenance (D, ACL): event `BikeWithdrawn` for every reason except `REQUESTED`.
2. Customer Support (U, OHS/PL) → FleetMaintenance (D, ACL): event `FleetIssueReported`.
3. FleetMaintenance (D, ACL) calls Rentals' OHS command *Withdraw Bike*. Direction FM → Rentals is a call to the upstream's OHS, matching the CML.

## Context map changes

- None.

## Translation

| Spec term | Context | Context term | Where translated |
|---|---|---|---|
| Broken bike | FleetMaintenance | Broken bike | – |
| Broken bike | Rentals | Withdrawn bike | FM's `rentalsctxacl` adapters and listener (FM never sends "broken" to Rentals) |
| Withdrawal reason *lock failed / fault reported at return / fault signalled / not returned* | FleetMaintenance | Fault source `LOCK_FAILED` / `FAULT_REPORTED_AT_RETURN` / `FAULT_SIGNALLED` / `NOT_RETURNED` | `rentalsctxacl` listener (`BikeWithdrawn.reason` → our fault source) |
| Withdrawal reason *requested* | FleetMaintenance | not a fault report (ignored) | `rentalsctxacl` listener |
| Fleet-issue incident | FleetMaintenance | Incident link (fault source `FLEET_ISSUE`) | `customersupportctxacl` listener |
| Fleet operations learns of the withdrawal | FleetMaintenance | Report Bike Broken | listener → use case |
| Withdraw Bike | FleetMaintenance | take the bike away from riders (port `BikeWithdrawals`) | `rentalsctxacl` adapter → `POST /rentals/bikes/{bikeId}/withdrawal` |

## Contract mapping

| Spec element | Context | Code |
|---|---|---|
| Command: Report Bike Broken | FleetMaintenance | `ReportBikeBrokenUseCase` + record `ReportBikeBroken(bikeId, faultSource, incidentId?)`; no REST (triggered only by events): listeners `infra/adapters/primary/rentalsctxacl/BikeWithdrawnListener` (`@Async @EventListener` on `rentals.api.events.BikeWithdrawn`, ignores `REQUESTED`) and `infra/adapters/primary/customersupportctxacl/FleetIssueReportedListener` (on `customersupport.api.events.FleetIssueReported`) |
| Command: Withdraw Bike | Rentals | `WithdrawBikeUseCase` + record `WithdrawBike(bikeId)`; `POST /rentals/bikes/{bikeId}/withdrawal` `{}` (reason `REQUESTED`) |
| Command: Withdraw Bike (caller side) | FleetMaintenance | secondary port `BikeWithdrawals` (our language: "take bike away from riders"), adapter `infra/adapters/secondary/rentalsctxacl/RentalsBikeWithdrawalsAdapter` (RestClient + `VelocityServiceUrls`, 404 → `UnknownBike`) |
| Command: Reserve Bike (rider) | Rentals | `ReserveBikeUseCase` + `ReserveBike`, `POST /rentals/bikes/{bikeId}/reservations` (planned in `rider-reserves-bike`; 409 for a withdrawn bike) |
| Event: Bike Reported Broken | FleetMaintenance | domain event `BikeReportedBroken`; no integration event (no listener in integration-contracts.md) |
| Event: Bike Withdrawn | Rentals | domain event `BikeWithdrawn` (reason `REQUESTED`) mapped to the existing integration event `rentals.api.events.BikeWithdrawn` (listener: Fleet Maintenance) |
| Event: Bike Reserved | Rentals | domain event `BikeReserved` (planned in `rider-reserves-bike`); no integration event |
| Query (support for tests and technicians) | FleetMaintenance | `BrokenBikeQueryHandler` in `api/queries`, `GET /fleet-maintenance/bikes/{bikeId}` (see CONTRACT GAP) |

## Canvas changes

### FleetMaintenance (`docs/bounded-contexts/velocity/fleetmaintenance/canvas.md`)

- **Business decisions, tag:** "A bike withdrawn by Rentals is registered broken" → `(spec: fleet-operations-takes-broken-bike-out-of-circulation#E1.1)`; "A fleet-issue incident makes the bike broken and withdraws it" → `#E2.1`.
- **Business decisions, add** (to "A bike withdrawn by Rentals is registered broken"): *Not returned:* Rentals reports B-22 withdrawn with reason *not returned* → B-22 is broken with fault source *not returned* (spec: fleet-operations-takes-broken-bike-out-of-circulation#E1.2, DE-21).
- **Business decisions, add** (to "A duplicate fault report changes nothing"): *Incident on a broken bike:* B-17 broken since 10:40 (lock failed); fleet issue I-2 reported → I-2 linked, no second Bike Reported Broken, no second withdrawal request (spec: fleet-operations-takes-broken-bike-out-of-circulation#E3.1).

### Rentals (`docs/bounded-contexts/velocity/rentals/canvas.md`)

- **Business decisions, tag:** "A client can withdraw a bike" → `(spec: fleet-operations-takes-broken-bike-out-of-circulation#E2.2)`; "Only a technician can reserve a withdrawn bike" → `#E4.1`.
- **Business decisions, add** (to "A client can withdraw a bike"): *During a ride:* Anna's session on B-17 runs; Fleet Maintenance asks to withdraw B-17 → B-17 is withdrawn with reason *requested* and the session goes on until Anna returns B-17 (spec: fleet-operations-takes-broken-bike-out-of-circulation#E2.3, DE-86).

## Decisions

| # | Decision | Choice | Reason |
|---|---|---|---|
| D1 | TDD checkpoint loop | **off** | the trainer explicitly asked for a fully autonomous run (2026-11-08); the tactical skill's pushback is recorded in `docs/bounded-contexts/velocity/conversation.md`. Domain still built test-first, one example per cycle |
| D2 | Aggregate style | state-based | simplest; no requirement for an event history |
| D3 | ACLs | one per outbound query/command, in `@AntiCorruptionLayer` packages, calling REST via `RestClient` + `VelocityServiceUrls`; inbound events via `@Async @EventListener` ACL listeners | integration-contracts.md, CML `[D,ACL]` |
| D4 | Persistence | in-memory repositories with `Version` optimistic locking, after the domain is green | user memory "aggregate persistence after domain"; trainer's standing instruction |
| D5 | Old FleetMaintenance code | deleted (already staged for deletion in the working tree), together with the old context `plan.md`; rebuilt from the 2026-11-08 canvas | trainer: the old code is not a source of truth and will be replaced |
| D6 | Order of a fleet-issue hand-off | FM records the bike broken (and the incident link) first, then asks Rentals to withdraw it; a failed withdrawal call is logged and retried on the next report, the bike stays broken | safety first (DE-52): FM's register must never miss a broken bike because of a transient call failure |
| D7 | `BikeWithdrawn` with reason `REQUESTED` | ignored by FM's listener | spec "Notes for the plan"; integration-contracts.md "all reasons except `REQUESTED`"; avoids R2 looping into R1 |
| D8 | Unknown bike in FM | a bike FM never heard of is not broken; FM keeps no copy of the fleet | canvas: FM learns of a bike from its first fault report |

## Steps

### S1: Remove the old FleetMaintenance code and context plan
- **Status:** done
- **Context:** FleetMaintenance
- **Skill:** none (this skill)
- **Covers:** none
- **Done when:** `src/main/java/.../velocity/fleetmaintenance/**` (old `Bike`, `Circulation`, `FixNotifications`, policies, `TechnicianController`, config), its old tests and `docs/bounded-contexts/velocity/fleetmaintenance/plan.md` are gone, nothing references them, `./mvnw test` green.

### S2: Canvas updates for FleetMaintenance and Rentals
- **Status:** done
- **Context:** FleetMaintenance, Rentals
- **Skill:** none (apply the "Canvas changes" diff exactly)
- **Covers:** E1.2, E2.3, E3.1 (canvas only)
- **Done when:** both canvases carry the tags and additions above.

### S3: Rentals: withdraw a bike on a client's request
- **Status:** done
- **Context:** Rentals
- **Skill:** ddd-tactical-building-blocks (checkpoint loop off, D1), via bounded-context-scaffolder update mode
- **Prerequisite:** the Rentals bike lifecycle (bike, reservation, rental session) from the plans of `fleet-manager-adds-bike-to-fleet`, `rider-reserves-bike`, `rider-unlocks-reserved-bike` and `rider-returns-bike-at-station` (its withdrawal on a failed lock).
- **Covers:** R2 (Rentals half), E2.1, E2.2, E2.3
- **Done when:** one domain test per scenario passes: withdraw a free available bike (reason *requested*), withdraw a reserved bike (reservation cancelled), withdraw a bike in a ride (session goes on, bike not available after the return), withdraw an already withdrawn bike (nothing happens); `./mvnw test` green.

### S4: Rentals: Withdraw Bike use case and REST
- **Status:** done
- **Context:** Rentals
- **Skill:** bounded-context-scaffolder (6b)
- **Covers:** Withdraw Bike, Bike Withdrawn
- **Done when:** `WithdrawBikeUseCase` + `WithdrawBike`, `POST /rentals/bikes/{bikeId}/withdrawal` (404 unknown bike), domain `BikeWithdrawn` mapped to `rentals.api.events.BikeWithdrawn` with reason `REQUESTED`; a REST-level test passes; `./mvnw test` green.

### S5: FleetMaintenance: scaffold the context (6a)
- **Status:** done
- **Context:** FleetMaintenance
- **Skill:** bounded-context-scaffolder (6a), with this plan as the approved structural decisions
- **Covers:** none
- **Done when:** `package-info.java` (`@BoundedContext(name = "FleetMaintenance")`, `@Module`), api records (`ReportBikeBroken`), port `BikeWithdrawals` with `rentalsctxacl` adapter (`@AntiCorruptionLayer`), empty listener packages `infra/adapters/primary/rentalsctxacl` and `customersupportctxacl`, `FleetMaintenanceArchitectureTest` (only other contexts' `api` used); `./mvnw test` green.

### S6: FleetMaintenance: broken-bike rules
- **Status:** done
- **Context:** FleetMaintenance
- **Skill:** ddd-tactical-building-blocks (checkpoint loop off, D1); candidate: one aggregate per broken bike, left to the skill
- **Covers:** R1, R2, R3, E1.1, E1.2, E2.1, E3.1
- **Done when:** one domain test per example (a fault withdrawal makes the bike broken with its fault source; a fleet issue makes it broken, links the incident and asks for a withdrawal; an already broken bike only gets the incident link, no event); `./mvnw test` green.

### S7: FleetMaintenance: use case, listeners, query, persistence (6b)
- **Status:** done
- **Context:** FleetMaintenance
- **Skill:** bounded-context-scaffolder (6b)
- **Prerequisite:** Customer Support publishes `FleetIssueReported` (plan of `rider-reports-incident`).
- **Covers:** Report Bike Broken, Bike Reported Broken
- **Done when:** `ReportBikeBrokenUseCase`, `BikeWithdrawnListener` (ignores `REQUESTED`), `FleetIssueReportedListener`, `GET /fleet-maintenance/bikes/{bikeId}`, in-memory repository with `Version` (D4), wiring in `infra/config`; `./mvnw test` green.

### S8: Acceptance tests for the spec examples
- **Status:** done
- **Context:** FleetMaintenance, Rentals
- **Skill:** none (this skill)
- **Prerequisite:** `rider-returns-bike-at-station` (lock failure withdraws, E1.1), `fleet-operations-ends-overdue-rental` (not returned, E1.2), `rider-reports-incident` (fleet issue, E2.x), `rider-reserves-bike` (R5, E4.1) delivered.
- **Covers:** all examples
- **Done when:** `FleetOperationsTakesBrokenBikeOutOfCirculationSpecTest` (`@VelocitySpringTest`, real beans, `MutableClock`) has one test per example, set up only through the contexts' REST APIs; every cross-context hand-off (Rentals → FM, Customer Support → FM, FM → Rentals) is asserted with Awaitility; `./mvnw test` green.

## Acceptance tests

| Example | Test |
|---|---|
| E1.1 | `FleetOperationsTakesBrokenBikeOutOfCirculationSpecTest#e1_1_bike_withdrawn_for_failed_lock_becomes_broken` |
| E1.2 | `FleetOperationsTakesBrokenBikeOutOfCirculationSpecTest#e1_2_bike_not_returned_becomes_broken` |
| E2.1 | `FleetOperationsTakesBrokenBikeOutOfCirculationSpecTest#e2_1_fleet_issue_makes_available_bike_broken_and_withdrawn` |
| E2.2 | `FleetOperationsTakesBrokenBikeOutOfCirculationSpecTest#e2_2_fleet_issue_cancels_riders_reservation` |
| E2.3 | `FleetOperationsTakesBrokenBikeOutOfCirculationSpecTest#e2_3_fleet_issue_during_ride_lets_ride_go_on` |
| E3.1 | `FleetOperationsTakesBrokenBikeOutOfCirculationSpecTest#e3_1_fleet_issue_on_broken_bike_only_links_incident` |
| E4.1 | `FleetOperationsTakesBrokenBikeOutOfCirculationSpecTest#e4_1_rider_cannot_reserve_broken_bike` |

## Traceability

| Spec ID | Step | Files |
|---|---|---|
| R1 / E1.1, E1.2 | S2, S6, S7, S8 | FleetMaintenance: `src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/domain/bike/Bike.java` (`reportFault`), `src/test/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/domain/bike/FaultReportTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/api/usecases/ReportBikeBrokenUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/infra/adapters/primary/rentalsctxacl/BikeWithdrawnListener.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/infra/adapters/primary/customersupportctxacl/FleetIssueReportedListener.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/infra/adapters/secondary/rentalsctxacl/RentalsBikeWithdrawalsAdapter.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/FleetOperationsTakesBrokenBikeOutOfCirculationSpecTest.java` |
| R2 / E2.1 | S3, S4, S6, S7, S8 | Rentals: `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/Bike.java` (`withdraw`), `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/BikeWithdrawalTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/api/usecases/WithdrawBikeUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/primary/RentalsController.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/RentalsServiceForFleetMaintenanceRestTest.java` (`WithdrawingOnRequest`); FleetMaintenance: `src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/domain/bike/Bike.java` (`reportFault`), `src/test/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/domain/bike/FaultReportTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/api/usecases/ReportBikeBrokenUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/infra/adapters/primary/rentalsctxacl/BikeWithdrawnListener.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/infra/adapters/primary/customersupportctxacl/FleetIssueReportedListener.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/infra/adapters/secondary/rentalsctxacl/RentalsBikeWithdrawalsAdapter.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/FleetOperationsTakesBrokenBikeOutOfCirculationSpecTest.java` |
| R2 / E2.2 | S3, S4, S8 | Rentals: `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/Bike.java` (`withdraw`), `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/BikeWithdrawalTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/api/usecases/WithdrawBikeUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/primary/RentalsController.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/RentalsServiceForFleetMaintenanceRestTest.java` (`WithdrawingOnRequest`); FleetMaintenance: `src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/domain/bike/Bike.java` (`reportFault`), `src/test/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/domain/bike/FaultReportTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/api/usecases/ReportBikeBrokenUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/infra/adapters/primary/rentalsctxacl/BikeWithdrawnListener.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/infra/adapters/primary/customersupportctxacl/FleetIssueReportedListener.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/infra/adapters/secondary/rentalsctxacl/RentalsBikeWithdrawalsAdapter.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/FleetOperationsTakesBrokenBikeOutOfCirculationSpecTest.java` |
| R2 / E2.3 | S2, S3, S8 | Rentals: `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/Bike.java` (`withdraw`), `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/BikeWithdrawalTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/api/usecases/WithdrawBikeUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/primary/RentalsController.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/RentalsServiceForFleetMaintenanceRestTest.java` (`WithdrawingOnRequest`); FleetMaintenance: `src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/domain/bike/Bike.java` (`reportFault`), `src/test/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/domain/bike/FaultReportTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/api/usecases/ReportBikeBrokenUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/infra/adapters/primary/rentalsctxacl/BikeWithdrawnListener.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/infra/adapters/primary/customersupportctxacl/FleetIssueReportedListener.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/infra/adapters/secondary/rentalsctxacl/RentalsBikeWithdrawalsAdapter.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/FleetOperationsTakesBrokenBikeOutOfCirculationSpecTest.java` |
| R3 / E3.1 | S2, S6, S8 | FleetMaintenance: `src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/domain/bike/Bike.java` (`reportFault`), `src/test/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/domain/bike/FaultReportTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/api/usecases/ReportBikeBrokenUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/infra/adapters/primary/rentalsctxacl/BikeWithdrawnListener.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/infra/adapters/primary/customersupportctxacl/FleetIssueReportedListener.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/infra/adapters/secondary/rentalsctxacl/RentalsBikeWithdrawalsAdapter.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/FleetOperationsTakesBrokenBikeOutOfCirculationSpecTest.java` |
| R4 / E4.1 | S8 (rule delivered by `rider-reserves-bike`) | FleetMaintenance: `src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/domain/bike/Bike.java` (`reportFault`), `src/test/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/domain/bike/FaultReportTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/api/usecases/ReportBikeBrokenUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/infra/adapters/primary/rentalsctxacl/BikeWithdrawnListener.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/infra/adapters/primary/customersupportctxacl/FleetIssueReportedListener.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/infra/adapters/secondary/rentalsctxacl/RentalsBikeWithdrawalsAdapter.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/FleetOperationsTakesBrokenBikeOutOfCirculationSpecTest.java` |

## Risks and open questions

- CONTRACT GAP: `GET /fleet-maintenance/bikes/{bikeId}` has no `faultSource` and `reportedAt`; E1.x and E2.1 assert the fault source. Proposed: add `faultSource` (`LOCK_FAILED`\|`FAULT_REPORTED_AT_RETURN`\|`FAULT_SIGNALLED`\|`NOT_RETURNED`\|`FLEET_ISSUE`\|`TECHNICIAN`), `reportedAt`, `faultDescription?`. Planned as if it existed.
- E1.1 and E1.2 depend on Rentals publishing `rentals.api.events.BikeWithdrawn` for faults (plans of `rider-returns-bike-at-station`, `fleet-operations-ends-overdue-rental`). If those plans only raise the domain event, S4 here also maps the fault reasons to the integration event.
- R4 (E4.1) duplicates `rider-reserves-bike` E5.2; the acceptance test here only checks the end-to-end path broken → withdrawn → rejected.
- D6 makes FM's register and Rentals' availability briefly diverge if the withdrawal call fails; acceptable for the training, recorded for review.
- Acceptance tests share one Spring context: they use their own bike/rider ids (e.g. suffixed) instead of the spec's B-17/I-1.

## Review notes

- 2026-11-08: draft written (autonomous run); self-review against plan-critic.md checklist: AGREE (placement follows canvases and CML; hand-off directions match the map; every example covered; steps upstream-first; no building blocks decided). Note: R2 is deliberately split between FM and Rentals.
- 2026-11-08 (Rentals wave, autonomous run): Rentals wave: S1 marked done: the old FleetMaintenance code, tests and context plan were removed in commit 4702217 (prep). S3/S4 built in Rentals: `POST /rentals/bikes/{bikeId}/withdrawal` answers `200` (empty body), unknown bike `404`; a withdrawal during a ride withdraws the bike at once and publishes Bike Withdrawn *requested* while the ride goes on. The Rentals half of S2's canvas diff is applied to the Rentals canvas; the FleetMaintenance half and S2 itself stay todo for the Fleet Maintenance wave.
- 2026-11-08 (Fleet Maintenance wave, autonomous run): FleetMaintenance steps built; decisions:
  - Aggregate `Bike` (one per bike Fleet Maintenance heard of; a bike it never heard of is `Bike.unknown`, not broken, D8). The domain decides the withdrawal: `reportFault` raises `WithdrawalRequested` only for fault sources Rentals didn't withdraw itself (`FLEET_ISSUE`, `TECHNICIAN`); the use case then calls the `BikeWithdrawals` port.
  - D6 as planned (record first, then withdraw; a failed call is logged). Not built: the "retried on the next report" part, since a duplicate report must not ask for a second withdrawal (E3.1); recorded as a follow-up in the context plan.
  - Event-driven use cases retry a lost optimistic-locking race up to 3 times (`Retrying`), because listeners for one bike may run in parallel.
  - `GET /fleet-maintenance/bikes/{bikeId}` answers `404` for a bike Fleet Maintenance never heard of; it also carries the bike's whereabouts from Rentals (`stationId`, `latitude`, `longitude`, null when unknown).

## Changelog

### 2026-11-08: planned
- Built from spec dated 2026-11-08.

### 2026-11-08: Rentals wave (autonomous run)
- Rentals steps built test-first (TDD checkpoint loop off, D1); per-cycle log in `docs/bounded-contexts/velocity/rentals/plan.md`. Files and tests in the Traceability table; full suite green.

### 2026-11-08: Fleet Maintenance wave (autonomous run)
- FleetMaintenance steps built test-first (TDD checkpoint loop off, D1): domain tests `FaultReportTest (7, of which 4 for this spec)`, red seen before
  green; per-cycle log in `docs/bounded-contexts/velocity/fleetmaintenance/plan.md`. Canvas diff applied to the
  FleetMaintenance canvas. One acceptance test per example (Acceptance tests table), every cross-context hand-off
  asserted with Awaitility; full suite green.
