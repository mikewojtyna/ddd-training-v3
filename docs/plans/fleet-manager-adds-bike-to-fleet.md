---
name: fleet-manager-adds-bike-to-fleet
intent: fleet-manager-adds-bike-to-fleet
spec: fleet-manager-adds-bike-to-fleet
system: velocity
status: done
date: 2026-11-08
spec_date: 2026-11-08
---

# Implementation plan: A fleet manager wants to add a new bike to the fleet at a station

<!-- Reviewers: edit this file or answer in the conversation, then set status to "approved" (or say so).
     Invoking implementation-planner again resumes from this file. -->

## Summary

The intent lands in **Rentals**, which owns bike availability (CML responsibility "Bike availability"; canvas inbound
"Fleet manager | Command | Add Bike to Fleet"). It is the first of eight Rentals intents planned together, so this plan
also **removes the old BikeStation-based Rentals code and tests** (S1) and has the scaffolder create the fresh Rentals
module (package, `RentalsArchitectureTest`, Bike Availability query) that the other seven plans build on. No other
context is involved and no context-map change is needed. One example (E1.2) needs reserving, so its acceptance test
waits for `rider-reserves-bike`.

## Placement

| Spec element | Owner | Why |
|---|---|---|
| R1 A new bike added at a station is available there from that moment | Rentals | the rule sets bike availability, which Rentals owns (ownership table: rule → context holding the state; CML responsibility "Bike availability"; canvas decision "A fleet manager adds a new bike to the fleet at a station") |
| R2 A bike joins the fleet only once | Rentals | guards the identity of bikes Rentals lends (same state as R1; canvas: "putting B-40 into service again is rejected") |
| R3 A bike is added only at a station | Rentals | stations are reference data in Rentals (canvas UL "Station: … Rentals knows where each station is"; integration-contracts.md "Stations are reference data in Rentals") |
| Command: Add Bike to Fleet | Rentals | "who can say no?": R2 and R3 both live in Rentals (canvas inbound "Fleet manager \| Command \| Add Bike to Fleet") |
| Event: Bike Added to Fleet | Rentals | the fact happens in Rentals; no other context listens (canvas outbound: only Settlement and Fleet Maintenance listen, to other events) |
| Queries: none in the spec | – | the Bike Availability query (`GET /rentals/bikes/{bikeId}`) is added as the read side the acceptance tests and later plans use; it is Rentals' data (ownership table: query → data owner) |

**Outcome:** one existing context (Rentals, rebuilt from scratch).

## Context map changes

- None. Rentals has no relationship to the fleet manager's tooling; the fleet manager is an actor calling Rentals' REST resource.

## Translation

| Spec term | Context | Context term | Where translated |
|---|---|---|---|
| Fleet manager | Rentals | (actor; not modelled) | REST adapter (no identity needed by the rules) |
| Add to the fleet | Rentals | Add Bike to Fleet | REST adapter → `AddBikeToFleet` command |
| Fleet | Rentals | the bikes Rentals knows (repository) | domain; add **Fleet** to the canvas UL |
| Available | Rentals | Available (and free: no holder) | same word, same meaning |
| Station | Rentals | Station (reference data with a position) | same word, same meaning |
| "Harbour Café" is not a station | Rentals | unknown station | domain rejection, REST `409 {"error":"UnknownStation"}` |

## Contract mapping

| Spec element | Context | Code |
|---|---|---|
| Command: Add Bike to Fleet | Rentals | `AddBikeToFleetUseCase` + record `AddBikeToFleet(bikeId, stationId)` in `rentals/api/usecases`; `POST /rentals/bikes` `{bikeId, stationId}` → `201`; R2 → `409 {"error":"BikeAlreadyInFleet"}`, R3 → `409 {"error":"UnknownStation"}` |
| Event: Bike Added to Fleet | Rentals | domain event `BikeAddedToFleet` (bike, station, position, added at), returned in `DomainEvents` and published through `DomainEventPublisher`; **no** integration event (no listener in integration-contracts.md) |
| Query: Bike Availability (support) | Rentals | `BikeAvailabilityQuery` + `BikeAvailability` result in `rentals/api/queries`, `BikeAvailabilityQueryHandler`; `GET /rentals/bikes/{bikeId}` → `{bikeId, availability, hold, holderType?, holderId?, latitude, longitude, stationId?}`, unknown bike → `404` |
| Stations (reference data) | Rentals | Central 52.2300/21.0110, Old Town 52.2490/21.0122, Riverside 52.2400/21.0300 (integration-contracts.md); where the catalogue lives (domain value, secondary port with an in-memory adapter) is the tactical skill's call |

Data shape notes for the tactical skill: new bike = bike id, station id, added at (the clock's now).

## Canvas changes

### Rentals (`docs/bounded-contexts/velocity/rentals/canvas.md`)

- **Ubiquitous language, add:** **Fleet**: all bikes Rentals can lend or that wait for repair; a bike joins it once, when a fleet manager adds it at a station.
- **Outbound, change:** add *Bike Added to Fleet* to the row "Bike Reserved, Rental Session Started, Bike Made Available | Event | none outside Rentals yet".
- **Business decisions, add:**
  - `### A bike is added only at a station (spec: fleet-manager-adds-bike-to-fleet#E3.1)`: **Given** B-41 is unknown to Rentals and "Harbour Café" is not a station **When** a fleet manager puts B-41 into service at "Harbour Café" **Then** it is rejected and B-41 isn't part of the fleet.
- **Tag existing decisions:** "A fleet manager adds a new bike to the fleet at a station" → `(spec: fleet-manager-adds-bike-to-fleet#E1.1, #E2.1)`. E1.2 is the canvas decision "A rider reserves an available bike nearby" (tagged by `rider-reserves-bike`).

## Decisions

| # | Decision | Choice | Reason |
|---|---|---|---|
| D1 | TDD checkpoint loop | **off** | the trainer explicitly asked for a fully autonomous run (2026-11-08); the tactical skill's pushback is recorded in `docs/bounded-contexts/velocity/conversation.md`. Domain still built test-first, one example per cycle. |
| D2 | Aggregate style | state-based | simplest; no requirement for an event history |
| D3 | ACLs | one per outbound query/command, in `@AntiCorruptionLayer` packages, calling REST via `RestClient` + `VelocityServiceUrls`; inbound events via `@Async @EventListener` ACL listeners | trainer's integration style (integration-contracts.md). This plan has no outbound call. |
| D4 | Persistence | in-memory repositories with `Version` optimistic locking, after the domain is green | user memory "aggregate persistence after domain"; trainer's brief |
| D5 | Old Rentals code | delete `rentals/domain/bikestation/**`, `rentals/api/usecases/RentBikeUseCase.java` and `BikeStationTest` in S1; keep the published-language records in `rentals/api/events` (they follow integration-contracts.md) | the trainer said the old code is not a source of truth and may be removed; starting clean avoids the architecture test fighting the old classes |
| D6 | Rentals model | candidate for the tactical skill: one aggregate per bike holding its availability, hold (reservation/unlock) and current rental session; final call to `ddd-tactical-building-blocks` | the canvas' "one bike, one active holder" is a per-bike invariant |
| D7 | Delivery order of the eight Rentals plans | `fleet-manager-adds-bike-to-fleet` → `rider-reserves-bike` → `rider-unlocks-reserved-bike` → `fleet-operations-releases-expired-reservations` → `rider-returns-bike-at-station` → `rider-ends-ride-in-drop-zone` → `fleet-operations-ends-overdue-rental` → `fleet-operations-withdraws-bike-signalling-fault`. Examples that need a later intent get their own final step with that prerequisite. | each plan builds on the previous plan's states; no plan waits on a later one except for those deferred example steps |
| D8 | Rejections over REST | broken rule → `409 {"error":"<RuleName>","message":…}`; unknown bike in the path → `404`; malformed body → `400` | integration-contracts.md "Rejections over REST"; an unknown station in the body is a broken rule (R3), so `409`, not `404` |

## Steps

### S1: Remove the old Rentals code and tests
- **Status:** done
- **Context:** Rentals
- **Skill:** none (this skill)
- **Covers:** none
- **Done when:** `rentals/domain/bikestation/**`, `rentals/api/usecases/RentBikeUseCase.java` and `src/test/java/.../velocity/rentals/domain/bikestation/BikeStationTest.java` are deleted; nothing else references them (`grep -r bikestation src`); the "Rentals" section of `velocity/CLAUDE.md` and the `BikeStationTest` example commands in `README.md` are updated to the new module; `rentals/api/events/*` kept; `./mvnw test` green.

### S2: Update the Rentals canvas
- **Status:** done
- **Context:** Rentals
- **Skill:** none (this skill applies the canvas diff above)
- **Covers:** E3.1 (canvas decision), tags for E1.1, E2.1
- **Done when:** the canvas contains the diff from "Canvas changes" exactly.

### S3: Scaffold the fresh Rentals module (6a)
- **Status:** done
- **Context:** Rentals
- **Skill:** bounded-context-scaffolder (new context from the canvas, Step 6a), told that this plan settles the structural decisions (D1-D8)
- **Covers:** Command: Add Bike to Fleet (record), Bike Availability query records
- **Done when:** `rentals/package-info.java` (`@BoundedContext(name = "Rentals")`, `@Module`), `api/usecases/AddBikeToFleet`, `api/queries/BikeAvailabilityQuery` + `BikeAvailability`, `infra/config/RentalsContextConfig` (empty of domain beans), `RentalsArchitectureTest` and the context plan `docs/bounded-contexts/velocity/rentals/plan.md` (`Status: approved`, "approved via docs/plans/fleet-manager-adds-bike-to-fleet.md") exist; `./mvnw test` green.

### S4: Domain: adding a bike to the fleet
- **Status:** done
- **Context:** Rentals
- **Skill:** ddd-tactical-building-blocks (checkpoint loop off, D1), via bounded-context-scaffolder
- **Covers:** R1, R2, R3, E1.1, E2.1, E3.1
- **Done when:** one unit test per example (E1.1, E2.1, E3.1) in the domain package passes, each written first; the bike is available and free at its station's position after being added; `./mvnw test` green.

### S5: Use case, query, REST and wiring (6b)
- **Status:** done
- **Context:** Rentals
- **Skill:** bounded-context-scaffolder (Step 6b)
- **Covers:** Command: Add Bike to Fleet, Event: Bike Added to Fleet, R1, R2, R3
- **Done when:** `AddBikeToFleetUseCase` (load/check, execute, save, publish), `BikeAvailabilityQueryHandler`, REST `POST /rentals/bikes` and `GET /rentals/bikes/{bikeId}` with the D8 error mapping, in-memory bike repository with `Version` (D4), Spring wiring in `RentalsContextConfig` using the shared `velocityClock`; `./mvnw test` green.

### S6: Acceptance tests for E1.1, E2.1, E3.1
- **Status:** done
- **Context:** Rentals
- **Skill:** none (this skill)
- **Covers:** E1.1, E2.1, E3.1
- **Done when:** `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/FleetManagerAddsBikeToFleetSpecTest.java` is a `@VelocitySpringTest` with real beans that drives `POST /rentals/bikes` and asserts with `GET /rentals/bikes/{bikeId}` (MutableClock set per test, unique bike ids per test), one test method per example, scenario in `@DisplayName`; `./mvnw test` green.

### S7: Acceptance test for E1.2 (needs reserving)
- **Status:** done
- **Context:** Rentals
- **Skill:** none (this skill)
- **Covers:** E1.2
- **Done when:** prerequisite `docs/plans/rider-reserves-bike.md` S5 is done (Reserve Bike over REST, with the Accounts eligibility ACL); `FleetManagerAddsBikeToFleetSpecTest#e1_2_added_bike_can_be_reserved_by_rider_nearby` adds B-40 at Central at 07:30, onboards an eligible rider through Accounts' REST API and reserves B-40 at 07:45 from a point near Central, asserting the bike is reserved for that rider; `./mvnw test` green.

## Acceptance tests

| Example | Test |
|---|---|
| E1.1 | `FleetManagerAddsBikeToFleetSpecTest#e1_1_new_bike_added_at_station_is_available_there` |
| E1.2 | `FleetManagerAddsBikeToFleetSpecTest#e1_2_added_bike_can_be_reserved_by_rider_nearby` |
| E2.1 | `FleetManagerAddsBikeToFleetSpecTest#e2_1_adding_same_bike_again_is_rejected` |
| E3.1 | `FleetManagerAddsBikeToFleetSpecTest#e3_1_adding_bike_outside_a_station_is_rejected` |

## Traceability

| Spec ID | Step | Files |
|---|---|---|
| R1 / E1.1 | S4, S5, S6 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/Fleet.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/FleetTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/api/usecases/AddBikeToFleetUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/primary/RentalsController.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/FleetManagerAddsBikeToFleetSpecTest.java` |
| R1 / E1.2 | S7 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/Fleet.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/FleetTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/api/usecases/AddBikeToFleetUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/primary/RentalsController.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/FleetManagerAddsBikeToFleetSpecTest.java` |
| R2 / E2.1 | S4, S5, S6 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/Fleet.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/FleetTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/api/usecases/AddBikeToFleetUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/primary/RentalsController.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/FleetManagerAddsBikeToFleetSpecTest.java` |
| R3 / E3.1 | S2, S4, S5, S6 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/Fleet.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/FleetTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/api/usecases/AddBikeToFleetUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/primary/RentalsController.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/FleetManagerAddsBikeToFleetSpecTest.java` |

## Risks and open questions

- **Q1 (open, accepted):** can a bike be added in a drop zone? The plan follows E3.1's literal reading (stations only). If the answer is "yes", R3 changes in S4 and E3.1 is rewritten by `spec-writer` first.
- Deleting `BikeStation` removes the reference implementation that `ddd-tactical-building-blocks` (SKILL.md "The reference implementation is …/rentals/domain/bikestation") and `tdd` (SKILL.md, `BikeStationTest`) point to. The tactical skill can still read it with `git show master:src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bikestation/BikeStation.java`; updating the skills' text is out of this plan's scope and is flagged for the trainer.
- Fleet Maintenance's existing `rentalsctxacl` adapter does not import old Rentals classes (checked); it calls Rentals over REST only once its own plan rewrites it.
- Spring tests share one cached application context: bike ids must be unique per test (e.g. `B-40-<uuid>`), and assertions must not assume an empty fleet.

## Review notes

- 2026-11-08: draft written (autonomous run); self-review against plan-critic.md checklist: AGREE. Placement single-context and backed by CML + canvas; no new relationship; every example covered (E1.2 deferred to S7 with an explicit prerequisite); tactical step has 3 rules guarding the same bike state; step order canvas → 6a → domain → 6b → acceptance; no building block decided in the plan (D6 is a candidate only).
- 2026-11-08 (Rentals wave, autonomous run): S1: the old Rentals code and tests were removed in commit 4702217 (prep); this wave updated the README test commands (`BikeReservationTest`). The Rentals section of `velocity/CLAUDE.md` still describes the old BikeStation code: left for the trainer (agents don't edit CLAUDE.md). S3-S5 built the module per the context plan `docs/bounded-contexts/velocity/rentals/plan.md`. Decisions: stations are a domain value `Stations` (reference data, ids = names `Central`, `Old Town`, `Riverside`); `POST /rentals/bikes` answers `201` without a body; the factory `Fleet` checks R2/R3 and the repository rejects a concurrent duplicate (optimistic locking).

## Changelog

### 2026-11-08: planned
- Built from spec dated 2026-11-08.

### 2026-11-08: Rentals wave (autonomous run)
- Rentals steps built test-first (TDD checkpoint loop off, D1); per-cycle log in `docs/bounded-contexts/velocity/rentals/plan.md`. Files and tests in the Traceability table; full suite green.
