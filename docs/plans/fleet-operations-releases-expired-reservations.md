---
name: fleet-operations-releases-expired-reservations
intent: fleet-operations-releases-expired-reservations
spec: fleet-operations-releases-expired-reservations
system: velocity
status: approved
date: 2026-11-08
spec_date: 2026-11-08
---

# Implementation plan: Fleet operations wants reservations that weren't used released

<!-- Reviewers: edit this file or answer in the conversation, then set status to "approved" (or say so).
     Invoking implementation-planner again resumes from this file. -->

## Summary

The intent lands in **Rentals**, which owns reservations (CML "Reserve and unlock"; canvas inbound "Fleet operations
(clock) | Command | Expire Reservations"). Following integration-contracts.md, the time-driven work is a use case taking
"now", triggered by a `@Scheduled` job that tests switch off and replace with a direct call under `MutableClock`. Each
expiry publishes `ReservationExpired` as an integration event because Fleet Maintenance listens (for technicians). The
technician examples need technician reservations, which the `technician-reserves-broken-bike` plan adds to Rentals,
so they get their own step.

## Placement

| Spec element | Owner | Why |
|---|---|---|
| R1 A rider's reservation not used within 2 minutes expires and the bike is available to anyone | Rentals | reservations and their period are Rentals' state (CML "Reserve and unlock"; canvas decision "An expired reservation releases the bike", DE-10, DE-81) |
| R2 A technician's reservation not used within 30 minutes expires, and the bike stays broken | Rentals | same state; Rentals speaks of *withdrawn*, never *broken* (canvas technician variant, DE-11) |
| R3 A reservation that ended by an unlock doesn't expire | Rentals | only a held reservation can expire; the hold is Rentals' state (canvas UL "Reservation", "Unlock") |
| Command: Release Expired Reservations | Rentals | no rejection rules; Rentals owns every reservation it touches (canvas inbound "Expire Reservations") |
| Event: Reservation Expired | Rentals | the fact happens in Rentals; Fleet Maintenance listens for technicians (integration-contracts.md) |

**Outcome:** one existing context (Rentals); one published event over the existing `Rentals -> FleetMaintenance` relationship.

## Context map changes

- None. `Rentals [U,OHS,PL]->[D,ACL] FleetMaintenance : BikeAvailability` covers `ReservationExpired`.

## Translation

| Spec term | Context | Context term | Where translated |
|---|---|---|---|
| Fleet operations | Rentals | (actor) the scheduled job | `infra/adapters/primary` `@Scheduled` job |
| The reservations are checked at T | Rentals | Release Expired Reservations with now = T | the job reads `velocityClock`; tests call the use case with `MutableClock` set |
| Rider / technician | Rentals | holder of type *rider* / *technician* | domain; published as `holderType` |
| Broken, withdrawn from riders | Rentals | Withdrawn | canvas UL |
| Available to anyone | Rentals | available and free | domain |

## Contract mapping

| Spec element | Context | Code |
|---|---|---|
| Command: Release Expired Reservations | Rentals | `ReleaseExpiredReservationsUseCase` + record `ReleaseExpiredReservations(Instant now)` in `rentals/api/usecases`; triggered by `infra/adapters/primary/ReservationExpiryJob` (`@Scheduled`, inactive when `velocity.scheduling.enabled=false`); no REST endpoint (integration-contracts.md has none; Fleet Operations is the clock) |
| Event: Reservation Expired | Rentals | domain event `ReservationExpired` (bike, holder, reserved at, expired at), one per expired reservation; integration event `rentals.api.events.ReservationExpired {bikeId, holderType, holderId, expiredAt}` (record exists) for every holder; Fleet Maintenance's listener keeps technicians only |

Data note: *expired at* is the moment of the check (now), which is later than the reservation end (DE-81: from the next second on).

## Canvas changes

### Rentals (`docs/bounded-contexts/velocity/rentals/canvas.md`)

- **Business decisions, add:**
  - `### The next rider can reserve a bike whose reservation expired (spec: fleet-operations-releases-expired-reservations#E1.3)`: **Given** Anna's reservation of B-17 expired at 10:02:01 **When** Ben, standing 10 m from B-17, reserves it at 10:04 **Then** B-17 is reserved for Ben until 10:06.
  - `### A reservation that ended by an unlock doesn't expire (spec: fleet-operations-releases-expired-reservations#E3.1, #E3.2)`: **Given** Anna unlocked B-17 at 10:01 (ride running), or Anna's unlock of B-17 failed at 10:01 **When** the reservations are checked at 10:03 **Then** nothing expires.
  - Change the technician variant of "An expired reservation releases the bike" to the spec's boundary: checked at 09:30:01 → expired, stays withdrawn; checked at exactly 09:30:00 → still reserved.
- **Tag existing decisions:** "An expired reservation releases the bike" → `#E1.1`, `#E1.2`, `#E2.1`, `#E2.2` (`spec: fleet-operations-releases-expired-reservations`).

## Decisions

| # | Decision | Choice | Reason |
|---|---|---|---|
| D1 | TDD checkpoint loop | **off** | the trainer explicitly asked for a fully autonomous run (2026-11-08); the tactical skill's pushback is recorded in `docs/bounded-contexts/velocity/conversation.md`. Domain still built test-first, one example per cycle. |
| D2 | Aggregate style | state-based | simplest; no requirement for an event history |
| D3 | ACLs | one per outbound query/command, in `@AntiCorruptionLayer` packages, calling REST via `RestClient` + `VelocityServiceUrls`; inbound events via `@Async @EventListener` ACL listeners | trainer's integration style; this plan has no outbound call |
| D4 | Persistence | in-memory repositories with `Version` optimistic locking, after the domain is green; the repository offers a lookup of reservations ending before a given instant | user memory; trainer's brief |
| D5 | Delivery order | fourth of the eight Rentals plans (`fleet-manager-adds-bike-to-fleet` D7); E2.1/E2.2 deferred to S6 | technician reservations come from `technician-reserves-broken-bike` |
| D6 | One failure doesn't stop the run | each expired reservation is released in its own load-execute-save; an optimistic-locking conflict (e.g. a concurrent unlock) skips that bike for this run | a reservation that was just unlocked must not be expired (R3) |
| D7 | Integration events for every holder | as in `rider-unlocks-reserved-bike` D6 | the event states a fact; Fleet Maintenance filters |

## Steps

### S1: Update the Rentals canvas
- **Status:** todo
- **Context:** Rentals
- **Skill:** none (this skill applies the canvas diff above)
- **Covers:** E1.3, E3.1, E3.2 (canvas decisions), E2.2 (boundary), tags for E1.1, E1.2, E2.1
- **Done when:** the canvas contains the diff from "Canvas changes" exactly.

### S2: Scaffold the command record and check the published event (6a)
- **Status:** todo
- **Context:** Rentals
- **Skill:** bounded-context-scaffolder (update mode, Step 6a)
- **Covers:** Command: Release Expired Reservations (record), Event: Reservation Expired (integration record, exists)
- **Done when:** `api/usecases/ReleaseExpiredReservations` exists; `api/events/ReservationExpired` matches integration-contracts.md; `RentalsArchitectureTest` green; `./mvnw test` green.

### S3: Domain: rider reservations expire, used ones don't
- **Status:** todo
- **Context:** Rentals
- **Skill:** ddd-tactical-building-blocks (checkpoint loop off, D1)
- **Covers:** R1, R3, E1.1, E1.2, E1.3, E3.1, E3.2
- **Done when:** one unit test per example, written first, passes (10:02:00 still reserved, 10:02:01 expired; an unlocked or failed-unlock bike has nothing to expire); `./mvnw test` green.

### S4: Use case, scheduled job, event publishing and wiring (6b)
- **Status:** todo
- **Context:** Rentals
- **Skill:** bounded-context-scaffolder (Step 6b)
- **Covers:** Command: Release Expired Reservations, Event: Reservation Expired
- **Done when:** `ReleaseExpiredReservationsUseCase` (find reservations ending before now, release each, save, publish, map to the integration event), `ReservationExpiryJob` (`@Scheduled`, e.g. every second, using `velocityClock`), wiring in `RentalsContextConfig`; `./mvnw test` green.

### S5: Acceptance tests for the rider examples
- **Status:** todo
- **Context:** Rentals (Accounts over REST for eligible riders)
- **Skill:** none (this skill)
- **Covers:** E1.1, E1.2, E1.3, E3.1, E3.2
- **Done when:** `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/FleetOperationsReleasesExpiredReservationsSpecTest.java` is a `@VelocitySpringTest` with real beans; reservations and unlocks go through REST; "the reservations are checked at T" sets `MutableClock` to T and calls the `ReleaseExpiredReservationsUseCase` bean; E1.1 asserts the published `ReservationExpired` integration event with `@RecordApplicationEvents` and Awaitility; one test method per example; `./mvnw test` green.

### S6: Technician reservations expire, the bike stays withdrawn
- **Status:** todo
- **Context:** Rentals
- **Skill:** ddd-tactical-building-blocks (domain scenarios), then this skill (acceptance tests)
- **Covers:** R2, E2.1, E2.2
- **Done when:** prerequisites: the `technician-reserves-broken-bike` plan's Rentals step (Reserve Bike with `holderType: "TECHNICIAN"`, 30 min) and `fleet-operations-withdraws-bike-signalling-fault` S5 (to withdraw a bike in the test) are done; one domain test per example passes (09:30:00 still reserved, 09:30:01 expired and still withdrawn); `FleetOperationsReleasesExpiredReservationsSpecTest#e2_1…`, `#e2_2…` pass; `./mvnw test` green.

## Acceptance tests

| Example | Test |
|---|---|
| E1.1 | `FleetOperationsReleasesExpiredReservationsSpecTest#e1_1_unused_rider_reservation_expires_and_bike_is_available` |
| E1.2 | `FleetOperationsReleasesExpiredReservationsSpecTest#e1_2_rider_reservation_still_holds_at_exact_end` |
| E1.3 | `FleetOperationsReleasesExpiredReservationsSpecTest#e1_3_next_rider_reserves_bike_after_expiry` |
| E2.1 | `FleetOperationsReleasesExpiredReservationsSpecTest#e2_1_unused_technician_reservation_expires_and_bike_stays_withdrawn` |
| E2.2 | `FleetOperationsReleasesExpiredReservationsSpecTest#e2_2_technician_reservation_still_holds_at_exact_end` |
| E3.1 | `FleetOperationsReleasesExpiredReservationsSpecTest#e3_1_unlocked_reservation_does_not_expire` |
| E3.2 | `FleetOperationsReleasesExpiredReservationsSpecTest#e3_2_reservation_released_by_failed_unlock_does_not_expire` |

## Traceability

| Spec ID | Step | Files |
|---|---|---|
| R1 / E1.1 | S3, S4, S5 | (filled in when built) |
| R1 / E1.2 | S3, S5 | (filled in when built) |
| R1 / E1.3 | S1, S3, S5 | (filled in when built) |
| R2 / E2.1 | S6 | (filled in when built) |
| R2 / E2.2 | S1, S6 | (filled in when built) |
| R3 / E3.1 | S1, S3, S5 | (filled in when built) |
| R3 / E3.2 | S1, S3, S5 | (filled in when built) |

## Risks and open questions

- **Q2 (open, accepted):** is the rider told their reservation expired? Doesn't change the rules; `GET /rentals/riders/{riderId}/active-rental` answers `404` afterwards, which is all the rider app can see for now.
- **Overlap with `technician-reserves-broken-bike`:** its spec repeats R2 as its own R4 (E4.1-E4.3). Whoever is built second only adds acceptance tests; the expiry logic is built once, here (S3 for riders, S6 for technicians). The orchestrator should tell that plan's author.
- The scheduled job is off in tests; checks in the shared Spring context expire other tests' reservations too. Tests assert only on their own bikes and set `MutableClock` explicitly at the start.

## Review notes

- 2026-11-08: draft written (autonomous run); self-review against plan-critic.md checklist: AGREE. Single owner; integration event only because Fleet Maintenance listens; R2 kept in Rentals (the rule is about the hold, not about repair); technician examples wait for the plan that creates technician reservations instead of duplicating it.

## Changelog

### 2026-11-08: planned
- Built from spec dated 2026-11-08.

