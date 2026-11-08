---
name: rider-ends-ride-in-drop-zone
intent: rider-ends-ride-in-drop-zone
spec: rider-ends-ride-in-drop-zone
system: velocity
status: done
date: 2026-11-08
spec_date: 2026-11-08
---

# Implementation plan: A rider wants to end the ride where they are, outside a station

<!-- Reviewers: edit this file or answer in the conversation, then set status to "approved" (or say so).
     Invoking implementation-planner again resumes from this file. -->

## Summary

The intent lands in **Rentals** (CML "Rental sessions"; canvas inbound "Rider app | Command | End Ride Here
(free-floating return)"). It mirrors `rider-returns-bike-at-station` with a position instead of a station: Rentals
closes the lock, ends the session at the rider's position and withdraws the bike on a failed lock. Whether the position
is in a drop zone is **not** Rentals' question: Pricing asks the City zone map when it prices the ride. Rentals publishes
`RentalSessionEnded` (Settlement) and `BikeWithdrawn` (Fleet Maintenance).

## Placement

| Spec element | Owner | Why |
|---|---|---|
| R1 The ride ends where the rider is, in a drop zone or anywhere else | Rentals | ending sessions is Rentals' (CML "Rental sessions"; canvas decision "A free-floating return ends the ride where the rider is"); the zone only matters for the price, which Pricing owns (CML Pricing "drop zone / unauthorized zone fees"; `City -> Pricing : ZoneMap`) |
| R2 When the lock fails, the ride ends at once and the bike is withdrawn, wherever it is | Rentals | Rentals detects the failed lock and withdraws the bike (CML "Withdrawing faulty bikes"; canvas decision "A failed lock at a free-floating return is a lock failure", DE-23) |
| R3 Only the rider whose ride is running can end it | Rentals | guards the session (canvas Enforcer role) |
| Command: End Ride Here | Rentals | "who can say no?": R3 in Rentals (canvas inbound) |
| Event: Rental Session Ended | Rentals | the session ends in Rentals; Settlement listens (CML `Rentals -> Settlement`) |
| Event: Bike Withdrawn | Rentals | Rentals decides the withdrawal; Fleet Maintenance listens (CML `Rentals -> FleetMaintenance`) |

**Outcome:** one existing context (Rentals); two published events over existing relationships. The drop-zone decision deliberately stays out of Rentals.

## Context map changes

- None. Rentals needs no zone map; adding a `City -> Rentals` relationship would give Rentals a pricing question (Brain Context smell).

## Translation

| Spec term | Context | Context term | Where translated |
|---|---|---|---|
| Rider's position | Rentals | position (latitude, longitude), trusted as reported (DE-22) | REST adapter |
| Free-floating return / lock failure | Rentals | end reason *free-floating return* / *lock failure* | published as `FREE_FLOATING_RETURN` / `LOCK_FAILURE` |
| Drop zone / unauthorized zone | Pricing | drop zone / unauthorized zone | not translated in Rentals; Pricing derives it from the published position |
| Withdrawn (lock failed) | Rentals | withdrawal reason *lock failed* | published as `LOCK_FAILED` |

## Contract mapping

| Spec element | Context | Code |
|---|---|---|
| Command: End Ride Here | Rentals | `EndRideHereUseCase` + record `EndRideHere(bikeId, riderId, latitude, longitude)` in `rentals/api/usecases`; `POST /rentals/bikes/{bikeId}/free-floating-return` `{riderId, latitude, longitude}` → `200 {sessionId, endReason, endedAt}`; rejection `409 NoRunningSession` (R3); unknown bike `404` |
| Event: Rental Session Ended | Rentals | domain event `RentalSessionEnded`; integration event `rentals.api.events.RentalSessionEnded` with `latitude`/`longitude` and no `stationId` |
| Event: Bike Withdrawn | Rentals | domain event `BikeWithdrawn` (reason *lock failed*, position); integration event `rentals.api.events.BikeWithdrawn {bikeId, reason: "LOCK_FAILED", withdrawnAt}` |
| Locking | Rentals | port `BikeLocks` + `bikelocksacl`, from `rider-unlocks-reserved-bike` |

## Canvas changes

### Rentals (`docs/bounded-contexts/velocity/rentals/canvas.md`)

- **Business decisions, add:**
  - `### A free-floating return in an unauthorized zone still ends the ride (spec: rider-ends-ride-in-drop-zone#E1.2)`: **Given** Anna's session on B-17 started at 10:01 **When** Anna ends the ride at (52.2500, 21.1000), outside every drop zone, at 10:30 and the lock closes **Then** the session ended there at 10:30 with reason *free-floating return*; the zone only decides the price.
  - `### Only the rider of a running session can end it here (spec: rider-ends-ride-in-drop-zone#E3.1, #E3.2)`: **Given** Anna's session on B-17 runs **When** Ben ends the ride on B-17, or Anna (without a session on B-22) ends a ride on B-22 **Then** it is rejected and Anna's session keeps running.
- **Tag existing decisions:** "A free-floating return ends the ride where the rider is" → `#E1.1` (add "and B-17 is available there"); "A failed lock at a free-floating return is a lock failure" → `#E2.1` (the spec uses position (52.2297, 21.0122); the canvas example keeps its own position, the rule is the same) (all `spec: rider-ends-ride-in-drop-zone`).

## Decisions

| # | Decision | Choice | Reason |
|---|---|---|---|
| D1 | TDD checkpoint loop | **off** | the trainer explicitly asked for a fully autonomous run (2026-11-08); the tactical skill's pushback is recorded in `docs/bounded-contexts/velocity/conversation.md`. Domain still built test-first, one example per cycle. |
| D2 | Aggregate style | state-based | simplest; no requirement for an event history |
| D3 | ACLs | one per outbound query/command, in `@AntiCorruptionLayer` packages, calling REST via `RestClient` + `VelocityServiceUrls`; inbound events via `@Async @EventListener` ACL listeners | trainer's integration style; here only `bikelocksacl` is used |
| D4 | Persistence | in-memory repositories with `Version` optimistic locking, after the domain is green | user memory; trainer's brief |
| D5 | Delivery order | sixth of the eight Rentals plans (`fleet-manager-adds-bike-to-fleet` D7) | reuses the session end and withdrawal built by `rider-returns-bike-at-station` |
| D6 | No zone check in Rentals | Rentals publishes the position; Pricing decides drop zone vs unauthorized zone | the spec's Out of scope; keeps Rentals' key question (availability and sessions) single |

## Steps

### S1: Update the Rentals canvas
- **Status:** done
- **Context:** Rentals
- **Skill:** none (this skill applies the canvas diff above)
- **Covers:** E1.2, E3.1, E3.2 (canvas decisions), tags for E1.1, E2.1
- **Done when:** the canvas contains the diff from "Canvas changes" exactly.

### S2: Scaffold the command record (6a)
- **Status:** done
- **Context:** Rentals
- **Skill:** bounded-context-scaffolder (update mode, Step 6a)
- **Covers:** Command: End Ride Here (record)
- **Done when:** `api/usecases/EndRideHere` exists; `RentalsArchitectureTest` green; `./mvnw test` green.

### S3: Domain: ending the ride at the rider's position
- **Status:** done
- **Context:** Rentals
- **Skill:** ddd-tactical-building-blocks (checkpoint loop off, D1)
- **Covers:** R1, R2, R3, E1.1, E1.2, E2.1, E3.1, E3.2
- **Done when:** one unit test per example, written first, passes; after a free-floating return the bike is free and available at that position (so a rider near it can reserve it); a failed lock withdraws it with *lock failed*; `./mvnw test` green.

### S4: Use case, REST, event publishing and wiring (6b)
- **Status:** done
- **Context:** Rentals
- **Skill:** bounded-context-scaffolder (Step 6b)
- **Covers:** Command: End Ride Here, Event: Rental Session Ended, Event: Bike Withdrawn
- **Done when:** `EndRideHereUseCase` (load, close the lock, execute, save, publish, map integration events), REST `POST /rentals/bikes/{bikeId}/free-floating-return` with the answer and error mapping above; `./mvnw test` green.

### S5: Acceptance tests for every example
- **Status:** done
- **Context:** Rentals (Accounts over REST for eligible riders)
- **Skill:** none (this skill)
- **Covers:** all examples
- **Done when:** `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/RiderEndsRideInDropZoneSpecTest.java` is a `@VelocitySpringTest` with real beans; sessions reached through REST; asserts the answer, `GET /rentals/bikes/{bikeId}` and the published `RentalSessionEnded` / `BikeWithdrawn` integration events with `@RecordApplicationEvents` and Awaitility; one test method per example; `./mvnw test` green.

## Acceptance tests

| Example | Test |
|---|---|
| E1.1 | `RiderEndsRideInDropZoneSpecTest#e1_1_ride_ends_at_position_in_drop_zone` |
| E1.2 | `RiderEndsRideInDropZoneSpecTest#e1_2_ride_ends_at_position_in_unauthorized_zone` |
| E2.1 | `RiderEndsRideInDropZoneSpecTest#e2_1_failed_lock_ends_ride_and_withdraws_bike` |
| E3.1 | `RiderEndsRideInDropZoneSpecTest#e3_1_other_rider_cannot_end_the_ride` |
| E3.2 | `RiderEndsRideInDropZoneSpecTest#e3_2_ending_without_running_session_is_rejected` |

## Traceability

| Spec ID | Step | Files |
|---|---|---|
| R1 / E1.1 | S3, S4, S5 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/Bike.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/FreeFloatingReturnTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/api/usecases/EndRideHereUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/secondary/IntegrationEventsPublisher.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/primary/RentalsController.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/RiderEndsRideInDropZoneSpecTest.java` |
| R1 / E1.2 | S1, S3, S5 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/Bike.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/FreeFloatingReturnTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/api/usecases/EndRideHereUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/secondary/IntegrationEventsPublisher.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/primary/RentalsController.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/RiderEndsRideInDropZoneSpecTest.java` |
| R2 / E2.1 | S3, S4, S5 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/Bike.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/FreeFloatingReturnTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/api/usecases/EndRideHereUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/secondary/IntegrationEventsPublisher.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/primary/RentalsController.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/RiderEndsRideInDropZoneSpecTest.java` |
| R3 / E3.1 | S1, S3, S5 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/Bike.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/FreeFloatingReturnTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/api/usecases/EndRideHereUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/secondary/IntegrationEventsPublisher.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/primary/RentalsController.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/RiderEndsRideInDropZoneSpecTest.java` |
| R3 / E3.2 | S1, S3, S5 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/Bike.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/FreeFloatingReturnTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/api/usecases/EndRideHereUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/secondary/IntegrationEventsPublisher.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/primary/RentalsController.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/RiderEndsRideInDropZoneSpecTest.java` |

## Risks and open questions

- **Q1 (open, accepted):** can a rider report a fault at a free-floating return? Not planned; if yes, `EndRideHere` gains an optional fault description like `ReturnBikeAtStation` (a contract change for integration-contracts.md).
- **CONTRACT GAP:** as in `rider-returns-bike-at-station`, the free-floating-return endpoint has no response body in integration-contracts.md; the plan assumes `200 {sessionId, endReason, endedAt}` so the rider is told the ride ended because the lock failed (E2.1).
- The spec's Bike Withdrawn carries a position; the published `BikeWithdrawn` doesn't. Fleet Maintenance doesn't need it for its current rules (finding bikes is not modelled, DE-26), so no gap is raised.

## Review notes

- 2026-11-08: draft written (autonomous run); self-review against plan-critic.md checklist: AGREE. Single owner; zone decision kept in Pricing (no new City → Rentals relationship); three rules in one tactical run, all guarding the session end; integration events only for existing listeners.
- 2026-11-08 (Rentals wave, autonomous run): No zone logic in Rentals (D6): the position goes into `RentalSessionEnded` (no `stationId`).

## Changelog

### 2026-11-08: planned
- Built from spec dated 2026-11-08.

### 2026-11-08: Rentals wave (autonomous run)
- Rentals steps built test-first (TDD checkpoint loop off, D1); per-cycle log in `docs/bounded-contexts/velocity/rentals/plan.md`. Files and tests in the Traceability table; full suite green.
