---
name: fleet-operations-withdraws-bike-signalling-fault
intent: fleet-operations-withdraws-bike-signalling-fault
spec: fleet-operations-withdraws-bike-signalling-fault
system: velocity
status: done
date: 2026-11-08
spec_date: 2026-11-08
---

# Implementation plan: Fleet operations wants a bike that signals a fault taken away from riders

<!-- Reviewers: edit this file or answer in the conversation, then set status to "approved" (or say so).
     Invoking implementation-planner again resumes from this file. -->

## Summary

The intent lands in **Rentals** (CML responsibility "Withdrawing faulty bikes"; canvas inbound "Bike (telemetry) |
Command | Signal Fault"). The bike's telemetry calls Rentals' REST resource; Rentals withdraws a free or reserved bike at
once (cancelling a reservation) and, during a ride, remembers the fault and withdraws the bike when the session ends.
`BikeWithdrawn` (reason `FAULT_SIGNALLED`) is published for Fleet Maintenance, whose reaction (a broken bike waiting
for a technician) belongs to its own plans.

## Placement

| Spec element | Owner | Why |
|---|---|---|
| R1 A free bike that signals a fault is withdrawn at once | Rentals | availability is Rentals' state, and Rentals withdraws bikes on faults it detects (CML "Withdrawing faulty bikes"; canvas UL "Withdrawn … fault signalled by the bike"; DE-25) |
| R2 A rider's reservation on a bike that signals a fault is cancelled, and the bike is withdrawn | Rentals | the hold and availability are Rentals' state (canvas decision "A fault signalled by the bike withdraws it, but never interrupts a ride") |
| R3 A fault signalled during a ride never interrupts it; the bike is withdrawn when the ride ends | Rentals | the session is Rentals' state (canvas decision, *during a ride* variant; DE-25, DE-86) |
| R4 A bike already withdrawn stays withdrawn when it signals a fault | Rentals | same availability state (canvas decision "A client can withdraw a bike": withdrawing a withdrawn bike changes nothing) |
| Command: Signal Fault | Rentals | no rejection rules; Rentals owns the state it changes (canvas inbound) |
| Event: Bike Withdrawn | Rentals | Rentals decides the withdrawal; Fleet Maintenance listens (CML `Rentals -> FleetMaintenance : BikeAvailability`) |

**Outcome:** one existing context (Rentals); one published event over an existing relationship.

## Context map changes

- None. The bike's telemetry is a device calling Rentals, not a bounded context.

## Translation

| Spec term | Context | Context term | Where translated |
|---|---|---|---|
| Fault signal / fault code | Rentals | Signal Fault with a fault code | REST adapter (`{faultCode}`) |
| Broken | Rentals | Withdrawn | canvas UL |
| Fault signalled | Rentals | withdrawal reason *fault signalled* | published as `FAULT_SIGNALLED` |
| Reservation is cancelled | Rentals | the hold is released without an expiry or unlock | domain |
| Ride | Rentals | rental session | canvas UL |

## Contract mapping

| Spec element | Context | Code |
|---|---|---|
| Command: Signal Fault | Rentals | `SignalFaultUseCase` + record `SignalFault(bikeId, faultCode)` in `rentals/api/usecases`; `POST /rentals/bikes/{bikeId}/fault-signals` `{faultCode}` → `202` (accepted in every state: withdrawn now, at the session end, or nothing changes); unknown bike `404`; missing fault code `400` |
| Event: Bike Withdrawn | Rentals | domain event `BikeWithdrawn` (reason *fault signalled*, fault code, withdrawn at); integration event `rentals.api.events.BikeWithdrawn {bikeId, reason: "FAULT_SIGNALLED", withdrawnAt}` — at once outside a ride, at the session end during one (published by the return use cases of `rider-returns-bike-at-station`, `rider-ends-ride-in-drop-zone`, `fleet-operations-ends-overdue-rental`) |

## Canvas changes

### Rentals (`docs/bounded-contexts/velocity/rentals/canvas.md`)

- **Business decisions, add:**
  - `### A free bike that signals a fault is withdrawn at once (spec: fleet-operations-withdraws-bike-signalling-fault#E1.1, #E1.2)`: **Given** B-17 is available at Central and nobody holds it **When** B-17 signals fault "BRAKE_SENSOR" at 09:00 **Then** B-17 is withdrawn with reason *fault signalled* (Bike Withdrawn), and Anna's reservation of B-17 at 09:05 is rejected.
  - `### A withdrawn bike that signals a fault stays withdrawn (spec: fleet-operations-withdraws-bike-signalling-fault#E4.1)`: **Given** B-17 was withdrawn at 08:00 with reason *lock failed* **When** B-17 signals fault "BRAKE_SENSOR" at 09:00 **Then** nothing changes and no second Bike Withdrawn happens.
- **Tag existing decisions:** "A fault signalled by the bike withdraws it, but never interrupts a ride" → `#E2.1` (add "Anna may reserve another bike"), *during a ride* variant → `#E3.1`, `#E3.2` (all `spec: fleet-operations-withdraws-bike-signalling-fault`).

## Decisions

| # | Decision | Choice | Reason |
|---|---|---|---|
| D1 | TDD checkpoint loop | **off** | the trainer explicitly asked for a fully autonomous run (2026-11-08); the tactical skill's pushback is recorded in `docs/bounded-contexts/velocity/conversation.md`. Domain still built test-first, one example per cycle. |
| D2 | Aggregate style | state-based | simplest; no requirement for an event history |
| D3 | ACLs | one per outbound query/command, in `@AntiCorruptionLayer` packages, calling REST via `RestClient` + `VelocityServiceUrls`; inbound events via `@Async @EventListener` ACL listeners | trainer's integration style; this plan has no outbound call |
| D4 | Persistence | in-memory repositories with `Version` optimistic locking, after the domain is green | user memory; trainer's brief |
| D5 | Delivery order | last of the eight Rentals plans (`fleet-manager-adds-bike-to-fleet` D7) | R3 changes how every session end withdraws the bike, so all session ends must exist first |
| D6 | One withdrawal per session end | when a session with a pending fault signal ends with a reason that withdraws the bike anyway (lock failure, fault reported at return, not returned), the bike is withdrawn once, with the reason found at the session end; the pending fault is used only when the end itself wouldn't withdraw the bike | E4.1 says a withdrawn bike gets no second Bike Withdrawn; the spec doesn't cover the combination (see risks) |
| D7 | Signal Fault never rejects | the bike's telemetry gets `202` in every state | the spec lists no "Rejected when"; a device can't act on a business rejection |

## Steps

### S1: Update the Rentals canvas
- **Status:** done
- **Context:** Rentals
- **Skill:** none (this skill applies the canvas diff above)
- **Covers:** E1.1, E1.2, E4.1 (canvas decisions), tags for E2.1, E3.1, E3.2
- **Done when:** the canvas contains the diff from "Canvas changes" exactly.

### S2: Scaffold the command record (6a)
- **Status:** done
- **Context:** Rentals
- **Skill:** bounded-context-scaffolder (update mode, Step 6a)
- **Covers:** Command: Signal Fault (record)
- **Done when:** `api/usecases/SignalFault` exists; `RentalsArchitectureTest` green; `./mvnw test` green.

### S3: Domain: a fault outside a ride withdraws the bike (run 1 of 2)
- **Status:** done
- **Context:** Rentals
- **Skill:** ddd-tactical-building-blocks (checkpoint loop off, D1)
- **Covers:** R1, R2, R4, E1.1, E1.2, E2.1, E4.1
- **Done when:** one unit test per example, written first, passes: a free bike is withdrawn with *fault signalled*; a reservation on it is cancelled and its rider holds nothing; an already withdrawn bike produces no event; `./mvnw test` green.

### S4: Domain: a fault during a ride withdraws the bike at the session end (run 2 of 2)
- **Status:** done
- **Context:** Rentals
- **Skill:** ddd-tactical-building-blocks (checkpoint loop off, D1)
- **Covers:** R3, E3.1, E3.2
- **Done when:** one unit test per example passes; the station return, free-floating return and overdue end (from the earlier plans) apply the pending fault by D6, with their existing tests still green; `./mvnw test` green.

### S5: Use case, REST and wiring (6b)
- **Status:** done
- **Context:** Rentals
- **Skill:** bounded-context-scaffolder (Step 6b)
- **Covers:** Command: Signal Fault, Event: Bike Withdrawn
- **Done when:** `SignalFaultUseCase` (load, execute, save, publish, map `BikeWithdrawn`), REST `POST /rentals/bikes/{bikeId}/fault-signals`, wiring; `./mvnw test` green.

### S6: Acceptance tests for every example
- **Status:** done
- **Context:** Rentals (Accounts over REST for eligible riders)
- **Skill:** none (this skill)
- **Covers:** all examples
- **Done when:** `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/FleetOperationsWithdrawsBikeSignallingFaultSpecTest.java` is a `@VelocitySpringTest` with real beans; bikes, reservations and sessions set up through REST; asserts `GET /rentals/bikes/{bikeId}`, `GET /rentals/riders/{riderId}/active-rental` and the published `BikeWithdrawn` integration events with `@RecordApplicationEvents` and Awaitility (E4.1: exactly one, from the earlier lock failure); one test method per example; `./mvnw test` green.

## Acceptance tests

| Example | Test |
|---|---|
| E1.1 | `FleetOperationsWithdrawsBikeSignallingFaultSpecTest#e1_1_free_bike_signalling_fault_is_withdrawn` |
| E1.2 | `FleetOperationsWithdrawsBikeSignallingFaultSpecTest#e1_2_withdrawn_bike_cannot_be_reserved_by_rider` |
| E2.1 | `FleetOperationsWithdrawsBikeSignallingFaultSpecTest#e2_1_reservation_on_faulty_bike_is_cancelled_and_bike_withdrawn` |
| E3.1 | `FleetOperationsWithdrawsBikeSignallingFaultSpecTest#e3_1_fault_during_ride_does_not_interrupt_it` |
| E3.2 | `FleetOperationsWithdrawsBikeSignallingFaultSpecTest#e3_2_bike_with_fault_during_ride_is_withdrawn_at_return` |
| E4.1 | `FleetOperationsWithdrawsBikeSignallingFaultSpecTest#e4_1_withdrawn_bike_signalling_fault_stays_withdrawn_once` |

## Traceability

| Spec ID | Step | Files |
|---|---|---|
| R1 / E1.1 | S1, S3, S5, S6 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/Bike.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/FaultSignalTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/api/usecases/SignalFaultUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/secondary/IntegrationEventsPublisher.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/primary/RentalsController.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/FleetOperationsWithdrawsBikeSignallingFaultSpecTest.java` |
| R1 / E1.2 | S1, S3, S6 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/Bike.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/FaultSignalTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/api/usecases/SignalFaultUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/secondary/IntegrationEventsPublisher.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/primary/RentalsController.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/FleetOperationsWithdrawsBikeSignallingFaultSpecTest.java` |
| R2 / E2.1 | S3, S5, S6 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/Bike.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/FaultSignalTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/api/usecases/SignalFaultUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/secondary/IntegrationEventsPublisher.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/primary/RentalsController.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/FleetOperationsWithdrawsBikeSignallingFaultSpecTest.java` |
| R3 / E3.1 | S4, S6 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/Bike.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/FaultSignalTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/api/usecases/SignalFaultUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/secondary/IntegrationEventsPublisher.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/primary/RentalsController.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/FleetOperationsWithdrawsBikeSignallingFaultSpecTest.java` |
| R3 / E3.2 | S4, S6 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/Bike.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/FaultSignalTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/api/usecases/SignalFaultUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/secondary/IntegrationEventsPublisher.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/primary/RentalsController.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/FleetOperationsWithdrawsBikeSignallingFaultSpecTest.java` |
| R4 / E4.1 | S1, S3, S6 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/Bike.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/FaultSignalTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/api/usecases/SignalFaultUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/secondary/IntegrationEventsPublisher.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/primary/RentalsController.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/FleetOperationsWithdrawsBikeSignallingFaultSpecTest.java` |

## Risks and open questions

- **Q1 (open, accepted):** is the rider told their reservation was cancelled? Not planned; `GET /rentals/riders/{riderId}/active-rental` answers `404` afterwards.
- **Q2 (open, accepted):** should a second fault code on a withdrawn bike be recorded for the technician? Not planned (E4.1: no state change, no event).
- **Spec gap (not a contradiction):** a session with a pending fault signal that ends by a lock failure or by the maximum rental time isn't covered by any spec. D6 picks "one withdrawal, the reason found at the session end"; the trainer should confirm, or `spec-writer` adds an example.
- **CONTRACT GAP (minor, shared with `rider-returns-bike-at-station`):** the spec's Bike Withdrawn carries the fault code; the published `BikeWithdrawn` has no field for it. An optional `faultDescription` on `BikeWithdrawn` would carry both the rider's description and the bike's fault code.
- S4 changes behaviour delivered by three earlier plans (the session end paths). Their acceptance tests must stay green; that is the S4 "Done when".

## Review notes

- 2026-11-08: draft written (autonomous run); self-review against plan-critic.md checklist: AGREE. Single owner; the event is published only because Fleet Maintenance listens; R1/R2/R4 grouped (bike outside a ride), R3 alone (session end); last in delivery order so S4 doesn't depend on later work.
- 2026-11-08 (Rentals wave, autonomous run): D6 implemented and tested (`FaultSignalTest#pendingFaultAndLockFailureWithdrawOnceWithLockFailed`): a pending fault is used only when the session end wouldn't withdraw the bike anyway. The fault code travels as `BikeWithdrawn.faultDescription`. A cancelled reservation raises the domain event `ReservationCancelled` (not published: no listener).

## Changelog

### 2026-11-08: planned
- Built from spec dated 2026-11-08.

### 2026-11-08: Rentals wave (autonomous run)
- Rentals steps built test-first (TDD checkpoint loop off, D1); per-cycle log in `docs/bounded-contexts/velocity/rentals/plan.md`. Files and tests in the Traceability table; full suite green.
