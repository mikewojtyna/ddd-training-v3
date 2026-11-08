---
name: rider-returns-bike-at-station
intent: rider-returns-bike-at-station
spec: rider-returns-bike-at-station
system: velocity
status: done
date: 2026-11-08
spec_date: 2026-11-08
---

# Implementation plan: A rider wants to return the bike at a station and end the ride

<!-- Reviewers: edit this file or answer in the conversation, then set status to "approved" (or say so).
     Invoking implementation-planner again resumes from this file. -->

## Summary

The intent lands in **Rentals** (CML "Station returns and locking", "Withdrawing faulty bikes"; canvas inbound "Rider app |
Command | Return Bike at Station"). Rentals closes the lock through the `BikeLocks` port from
`rider-unlocks-reserved-bike`, ends the session and, on a failed lock or a reported fault, withdraws the bike, which
introduces the *withdrawn* state. It publishes `RentalSessionEnded` (Settlement listens) and `BikeWithdrawn` (Fleet
Maintenance listens) as integration events; the reactions of those contexts belong to their own plans.

## Placement

| Spec element | Owner | Why |
|---|---|---|
| R1 A station return with a lock that closes ends the ride at that station | Rentals | rental sessions and station returns are Rentals' (CML "Rental sessions", "Station returns and locking"; canvas decision "A station return with a locked bike ends the session") |
| R2 When the lock fails, the ride ends at once and the bike is withdrawn | Rentals | Rentals detects the failed lock and withdraws the bike itself (CML "Withdrawing faulty bikes"; canvas UL "Withdrawn … failed lock") |
| R3 A fault reported at a station return withdraws the bike, but the ride ends normally | Rentals | same: a fault Rentals learns of at return (canvas decision "A fault reported at a station return withdraws the bike", DE-24); the normal fee is Pricing's business, the end reason Rentals' |
| R4 The ride ends at the station the rider names | Rentals | stations are Rentals' reference data; the client's station is trusted (canvas Assumption DE-22) |
| R5 Only the rider whose ride is running can return the bike | Rentals | guards the session, Rentals' state (canvas Enforcer role) |
| Command: Return Bike at Station | Rentals | "who can say no?": R4, R5 in Rentals (canvas inbound) |
| Event: Rental Session Ended | Rentals | the session ends in Rentals; Settlement listens (CML `Rentals -> Settlement : RentalSessionEnded`) |
| Event: Bike Withdrawn | Rentals | Rentals decides the withdrawal; Fleet Maintenance listens (CML `Rentals -> FleetMaintenance : BikeAvailability`) |

**Outcome:** one existing context (Rentals); two published events over existing relationships.

## Context map changes

- None. Both events already appear in the CML relationships from Rentals.

## Translation

| Spec term | Context | Context term | Where translated |
|---|---|---|---|
| Rider | Rentals | the session's rider (`riderId`) | REST adapter |
| Ride | Rentals | rental session | canvas UL |
| Station return / lock failure | Rentals | end reason *station return* / *lock failure* | published as `STATION_RETURN` / `LOCK_FAILURE` in `RentalSessionEnded` |
| Lock failed / fault reported at return | Rentals | withdrawal reason | published as `LOCK_FAILED` / `FAULT_REPORTED_AT_RETURN` in `BikeWithdrawn` |
| Fault description | Rentals | fault description (optional) | REST body `faultDescription` |
| "Anna is told the ride ended because the lock failed" | Rentals | the end reason in the command's answer | REST response (CONTRACT GAP below) |

## Contract mapping

| Spec element | Context | Code |
|---|---|---|
| Command: Return Bike at Station | Rentals | `ReturnBikeAtStationUseCase` + record `ReturnBikeAtStation(bikeId, riderId, stationId, faultDescription)` in `rentals/api/usecases`; `POST /rentals/bikes/{bikeId}/station-return` `{riderId, stationId, faultDescription?}` → `200 {sessionId, endReason, endedAt}`; rejections `409` `UnknownStation` (R4), `NoRunningSession` (R5); unknown bike `404` |
| Event: Rental Session Ended | Rentals | domain event `RentalSessionEnded`; integration event `rentals.api.events.RentalSessionEnded {sessionId, bikeId, riderId, startedAt, endedAt, endReason, stationId, latitude, longitude}` (record exists), with `stationId` and the station's position |
| Event: Bike Withdrawn | Rentals | domain event `BikeWithdrawn` (bike, reason, fault description, withdrawn at); integration event `rentals.api.events.BikeWithdrawn {bikeId, reason, withdrawnAt}` (record exists) |
| Locking | Rentals | port `BikeLocks` (close), adapter `bikelocksacl/SimulatedBikeLocks`, both from `rider-unlocks-reserved-bike` |

## Canvas changes

### Rentals (`docs/bounded-contexts/velocity/rentals/canvas.md`)

- **Business decisions, add:**
  - `### The ride ends at the station the rider names (spec: rider-returns-bike-at-station#E4.1, #E4.2)`: **Given** Anna's session on B-17 runs **When** Anna returns B-17 naming station Central and the lock closes **Then** the session ended at Central, without checking her position; naming "Harbour Café", which is no station, is rejected and the session keeps running.
  - `### Only the rider of a running session can return the bike (spec: rider-returns-bike-at-station#E5.1, #E5.2)`: **Given** Anna's session on B-17 runs **When** Ben returns B-17 at Old Town, or Anna (without a session on B-22) returns B-22 **Then** the return is rejected and Anna's session keeps running.
- **Change:** "A station return with a locked bike ends the session" → add "and B-17 is available at Old Town".
- **Tag existing decisions:** "A station return with a locked bike ends the session" → `#E1.1`; "A failed lock ends the session and withdraws the bike" → `#E2.1`; "A fault reported at a station return withdraws the bike" → `#E3.1` (all `spec: rider-returns-bike-at-station`).

## Decisions

| # | Decision | Choice | Reason |
|---|---|---|---|
| D1 | TDD checkpoint loop | **off** | the trainer explicitly asked for a fully autonomous run (2026-11-08); the tactical skill's pushback is recorded in `docs/bounded-contexts/velocity/conversation.md`. Domain still built test-first, one example per cycle. |
| D2 | Aggregate style | state-based | simplest; no requirement for an event history |
| D3 | ACLs | one per outbound query/command, in `@AntiCorruptionLayer` packages, calling REST via `RestClient` + `VelocityServiceUrls`; inbound events via `@Async @EventListener` ACL listeners | trainer's integration style; here only the `bikelocksacl` port is used |
| D4 | Persistence | in-memory repositories with `Version` optimistic locking, after the domain is green | user memory; trainer's brief |
| D5 | Delivery order | fifth of the eight Rentals plans (`fleet-manager-adds-bike-to-fleet` D7) | needs running sessions from `rider-unlocks-reserved-bike` |
| D6 | Integration events | `RentalSessionEnded` on every session end; `BikeWithdrawn` on every withdrawal Rentals decides | listeners: Settlement and Fleet Maintenance (integration-contracts.md) |
| D7 | Rejections | as in `fleet-manager-adds-bike-to-fleet` D8: an unknown station in the body is `409 UnknownStation` | a broken rule (R4), not an unknown resource |

## Steps

### S1: Update the Rentals canvas
- **Status:** done
- **Context:** Rentals
- **Skill:** none (this skill applies the canvas diff above)
- **Covers:** E4.1, E4.2, E5.1, E5.2 (canvas decisions), tags for E1.1, E2.1, E3.1
- **Done when:** the canvas contains the diff from "Canvas changes" exactly.

### S2: Scaffold the command record and check the published events (6a)
- **Status:** done
- **Context:** Rentals
- **Skill:** bounded-context-scaffolder (update mode, Step 6a)
- **Covers:** Command: Return Bike at Station (record), Event: Rental Session Ended, Event: Bike Withdrawn (integration records, exist)
- **Done when:** `api/usecases/ReturnBikeAtStation` exists; `BikeLocks` offers closing the lock; `api/events/RentalSessionEnded` and `BikeWithdrawn` match integration-contracts.md; `./mvnw test` green.

### S3: Domain: ending the session at a named station (run 1 of 2)
- **Status:** done
- **Context:** Rentals
- **Skill:** ddd-tactical-building-blocks (checkpoint loop off, D1)
- **Covers:** R1, R4, R5, E1.1, E4.1, E4.2, E5.1, E5.2
- **Done when:** one unit test per example, written first, passes; after a station return the bike is free and available at the station's position; `./mvnw test` green.

### S4: Domain: lock failure and reported fault withdraw the bike (run 2 of 2)
- **Status:** done
- **Context:** Rentals
- **Skill:** ddd-tactical-building-blocks (checkpoint loop off, D1)
- **Covers:** R2, R3, E2.1, E3.1
- **Done when:** one unit test per example passes: a failed lock ends the session with *lock failure* and withdraws with *lock failed*; a reported fault ends with *station return* and withdraws with *fault reported at return*; a withdrawn bike is no longer reservable by riders (the rule `rider-reserves-bike` R5 already expects); `./mvnw test` green.

### S5: Use case, REST, event publishing and wiring (6b)
- **Status:** done
- **Context:** Rentals
- **Skill:** bounded-context-scaffolder (Step 6b)
- **Covers:** Command: Return Bike at Station, Event: Rental Session Ended, Event: Bike Withdrawn
- **Done when:** `ReturnBikeAtStationUseCase` (load, close the lock via `BikeLocks`, execute, save, publish, map both integration events), REST `POST /rentals/bikes/{bikeId}/station-return` with the answer and error mapping above; `GET /rentals/bikes/{bikeId}` shows `availability: "WITHDRAWN"` after a withdrawal; `./mvnw test` green.

### S6: Acceptance tests for every example
- **Status:** done
- **Context:** Rentals (Accounts over REST for eligible riders)
- **Skill:** none (this skill)
- **Covers:** all examples
- **Done when:** `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/RiderReturnsBikeAtStationSpecTest.java` is a `@VelocitySpringTest` with real beans; each test reaches a running session through REST (add bike, reserve, unlock), returns over REST, and asserts the answer, `GET /rentals/bikes/{bikeId}` and the published `RentalSessionEnded` / `BikeWithdrawn` integration events with `@RecordApplicationEvents` and Awaitility (the hand-off to Settlement and Fleet Maintenance is asserted in their plans); E2.1 registers the bike as faulty on `SimulatedBikeLocks` after the unlock; one test method per example; `./mvnw test` green.

## Acceptance tests

| Example | Test |
|---|---|
| E1.1 | `RiderReturnsBikeAtStationSpecTest#e1_1_station_return_with_closed_lock_ends_ride_at_station` |
| E2.1 | `RiderReturnsBikeAtStationSpecTest#e2_1_failed_lock_ends_ride_and_withdraws_bike` |
| E3.1 | `RiderReturnsBikeAtStationSpecTest#e3_1_reported_fault_ends_ride_normally_and_withdraws_bike` |
| E4.1 | `RiderReturnsBikeAtStationSpecTest#e4_1_ride_ends_at_named_station_without_position_check` |
| E4.2 | `RiderReturnsBikeAtStationSpecTest#e4_2_return_at_unknown_station_is_rejected` |
| E5.1 | `RiderReturnsBikeAtStationSpecTest#e5_1_other_rider_cannot_return_the_bike` |
| E5.2 | `RiderReturnsBikeAtStationSpecTest#e5_2_return_without_running_session_is_rejected` |

## Traceability

| Spec ID | Step | Files |
|---|---|---|
| R1 / E1.1 | S3, S5, S6 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/Bike.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/StationReturnTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/api/usecases/ReturnBikeAtStationUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/secondary/IntegrationEventsPublisher.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/primary/RentalsController.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/RiderReturnsBikeAtStationSpecTest.java` |
| R2 / E2.1 | S4, S5, S6 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/Bike.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/StationReturnTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/api/usecases/ReturnBikeAtStationUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/secondary/IntegrationEventsPublisher.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/primary/RentalsController.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/RiderReturnsBikeAtStationSpecTest.java` |
| R3 / E3.1 | S4, S5, S6 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/Bike.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/StationReturnTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/api/usecases/ReturnBikeAtStationUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/secondary/IntegrationEventsPublisher.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/primary/RentalsController.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/RiderReturnsBikeAtStationSpecTest.java` |
| R4 / E4.1 | S1, S3, S6 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/Bike.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/StationReturnTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/api/usecases/ReturnBikeAtStationUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/secondary/IntegrationEventsPublisher.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/primary/RentalsController.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/RiderReturnsBikeAtStationSpecTest.java` |
| R4 / E4.2 | S1, S3, S6 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/Bike.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/StationReturnTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/api/usecases/ReturnBikeAtStationUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/secondary/IntegrationEventsPublisher.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/primary/RentalsController.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/RiderReturnsBikeAtStationSpecTest.java` |
| R5 / E5.1 | S1, S3, S6 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/Bike.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/StationReturnTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/api/usecases/ReturnBikeAtStationUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/secondary/IntegrationEventsPublisher.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/primary/RentalsController.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/RiderReturnsBikeAtStationSpecTest.java` |
| R5 / E5.2 | S1, S3, S6 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/Bike.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/StationReturnTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/api/usecases/ReturnBikeAtStationUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/secondary/IntegrationEventsPublisher.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/primary/RentalsController.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/RiderReturnsBikeAtStationSpecTest.java` |

## Risks and open questions

- **CONTRACT GAP:** integration-contracts.md lists no response body for `POST /rentals/bikes/{bikeId}/station-return`. E2.1 needs the rider to be told why the ride ended; the plan assumes `200 {sessionId, endReason, endedAt}` (canvas Assumption: "told why … through the answer to their return command").
- **CONTRACT GAP (minor):** the spec's Bike Withdrawn carries the fault description; the published `BikeWithdrawn {bikeId, reason, withdrawnAt}` doesn't. The plan keeps the description in the domain event only. If Fleet Maintenance should show it to technicians, add an optional `faultDescription` to `BikeWithdrawn` (it would also carry the fault code of `fleet-operations-withdraws-bike-signalling-fault`).
- A fault the bike signalled during the ride (DE-25) is applied at this return by the `fleet-operations-withdraws-bike-signalling-fault` plan (its S4), not here.

## Review notes

- 2026-11-08: draft written (autonomous run); self-review against plan-critic.md checklist: AGREE. Single owner; both events published only because their listeners exist in the CML; R1/R4/R5 grouped (they guard the running session) and R2/R3 grouped (withdrawal at return); pricing consequences (no extra charge after a lock failure) left out of scope as the spec says.
- 2026-11-08 (Rentals wave, autonomous run): Decisions: answer `200 {sessionId, bikeId, endReason, endedAt}`; a lock failure at a station return publishes `RentalSessionEnded` with the named station and its position; `BikeWithdrawn` carries the rider's description in the new optional last field `faultDescription` (integration-contracts.md Additions). A bike already withdrawn during the ride (a client's request, DE-86) gets no second Bike Withdrawn at the return.

## Changelog

### 2026-11-08: planned
- Built from spec dated 2026-11-08.

### 2026-11-08: Rentals wave (autonomous run)
- Rentals steps built test-first (TDD checkpoint loop off, D1); per-cycle log in `docs/bounded-contexts/velocity/rentals/plan.md`. Files and tests in the Traceability table; full suite green.
