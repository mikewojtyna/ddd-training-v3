---
name: rider-reserves-bike
intent: rider-reserves-bike
spec: rider-reserves-bike
system: velocity
status: done
date: 2026-11-08
spec_date: 2026-11-08
---

# Implementation plan: A rider wants to reserve a bike they are standing near

<!-- Reviewers: edit this file or answer in the conversation, then set status to "approved" (or say so).
     Invoking implementation-planner again resumes from this file. -->

## Summary

The intent lands in **Rentals** (CML responsibility "Reserve and unlock"; canvas inbound "Rider app | Command | Reserve
Bike"). Eligibility to rent (R3) is Accounts' answer: Rentals asks it through the existing
`Accounts [U,OHS,PL] -> [D,ACL] Rentals : AccountStatus` relationship, with a new secondary port and an
`accountsctxacl` adapter calling `GET /accounts/riders/{riderId}/rental-eligibility`. It builds on the Rentals module from
`fleet-manager-adds-bike-to-fleet`. Two examples need later intents (a running ride, a withdrawn bike), so they get a
final deferred step.

## Placement

| Spec element | Owner | Why |
|---|---|---|
| R1 An eligible rider standing near an available bike can reserve it for 2 minutes | Rentals | reservations and their period are Rentals' state (CML "Reserve and unlock"; canvas UL "Reservation: 2 minutes for a rider (DE-10)") |
| R2 A rider can reserve only a bike within 50 metres | Rentals | needs the bike's position, which Rentals holds (canvas UL "Near: within 50 metres (DE-12)") |
| R3 Only a rider whose account is activated and not blocked can reserve | Rentals applies it, Accounts decides eligibility | the command belongs to Rentals; Accounts' rule becomes a precondition fetched through an ACL query (ownership table, "Command": rules in two contexts → query through an ACL; CML `AccountStatus` ACL; canvas outbound "Rental Eligibility \| Query \| Accounts") |
| R4 One rider, one bike | Rentals | reads the rider's reservations and sessions, both Rentals' state (canvas decision "One rider, one bike", DE-13) |
| R5 A bike that is reserved, ridden or broken can't be reserved by a rider | Rentals | bike hold and availability are Rentals' state (canvas Enforcer role "one bike, one active holder"; "Only a technician can reserve a withdrawn bike") |
| Command: Reserve Bike | Rentals | "who can say no?": R2, R4, R5 in Rentals, R3 as a precondition (canvas inbound) |
| Query: Rider's Active Rental | Rentals | it's a view of Rentals' reservations and sessions (ownership table: query → data owner; canvas inbound "Rider app \| Query \| Rider's Active Rental") |
| Event: Bike Reserved | Rentals | the fact happens in Rentals; nobody outside listens (canvas outbound "none outside Rentals yet") |

**Outcome:** one existing context (Rentals), with a precondition queried from Accounts over the existing ACL relationship.

## Context map changes

- None. `Accounts [U,OHS,PL]->[D,ACL] Rentals : AccountStatus` already covers the eligibility query.

## Translation

| Spec term | Context | Context term | Where translated |
|---|---|---|---|
| Rider | Rentals | Holder of type *rider* (`holderType: "RIDER"`, `holderId`) | REST adapter |
| Rider's position | Rentals | position (latitude, longitude) | REST adapter |
| Eligible to rent | Rentals | eligible to rent (answer of the eligibility port) | `accountsctxacl`: Accounts' `{eligible, reason}` → Rentals' eligibility answer; `ONBOARDING_INCOMPLETE`, `BLOCKED`, `UNKNOWN_RIDER` all mean "not eligible" |
| Activated / blocked | Accounts | onboarding step, blocks | stays in Accounts; Rentals never sees these words |
| Broken | Rentals | Withdrawn | canvas UL ("speaks of bikes as available or withdrawn, never broken") |
| Ride | Rentals | Rental session | canvas UL |
| Reserved until | Rentals | reservation end (reserved at + 2 min, inclusive, DE-81) | domain |

## Contract mapping

| Spec element | Context | Code |
|---|---|---|
| Command: Reserve Bike | Rentals | `ReserveBikeUseCase` + record `ReserveBike(bikeId, holderType, holderId, latitude, longitude)` in `rentals/api/usecases` (this plan accepts `holderType = RIDER`; the technician variant comes with `technician-reserves-broken-bike`); `POST /rentals/bikes/{bikeId}/reservations` `{holderType: "RIDER", holderId, latitude, longitude}`; rejections `409` with `TooFarFromBike` (R2), `RiderNotEligible` (R3), `OneRiderOneBike` (R4), `BikeNotAvailable` (R5); unknown bike `404` |
| Query: Rider's Active Rental | Rentals | `RidersActiveRentalQuery` + `RidersActiveRental` in `rentals/api/queries`, `RidersActiveRentalQueryHandler`; `GET /rentals/riders/{riderId}/active-rental` → `{riderId, bikeId, kind: "RESERVATION"\|"SESSION", sessionId?, since, expiresAt?}` or `404` |
| Event: Bike Reserved | Rentals | domain event `BikeReserved` (holder, bike, reserved at, reserved until); **no** integration event (no listener in integration-contracts.md) |
| Precondition R3 | Rentals → Accounts | secondary port `RentalEligibility` (our language: "is this rider eligible to rent?") in the domain; adapter `infra/adapters/secondary/accountsctxacl/AccountsRentalEligibilityAdapter` (`@AntiCorruptionLayer` package) calling `GET /accounts/riders/{riderId}/rental-eligibility` with `RestClient` + `VelocityServiceUrls`, own response record |

## Canvas changes

### Rentals (`docs/bounded-contexts/velocity/rentals/canvas.md`)

- **Business decisions, add:**
  - `### A rider standing exactly 50 m away is near (spec: rider-reserves-bike#E2.2)`: **Given** B-17 is available at Central and Anna is eligible to rent **When** Anna, standing exactly 50 m from B-17, reserves it at 10:00 **Then** B-17 is reserved for Anna until 10:02 (Bike Reserved).
  - `### An ineligible rider can't reserve (spec: rider-reserves-bike#E3.1, #E3.2)`: **Given** Ben hasn't finished onboarding (no payment method yet), and Carla's account is blocked for outstanding debt **When** either of them, standing 10 m from available B-17, reserves it **Then** the reservation is rejected (Accounts says *not eligible*).
  - `### A rider in a running session can't reserve (spec: rider-reserves-bike#E4.2)`: **Given** Anna's session on B-17 started at 09:30 and runs **When** Anna, standing 15 m from available B-22, reserves B-22 at 10:00 **Then** the reservation is rejected.
- **Tag existing decisions:** "A rider reserves an available bike nearby" → `#E1.1`; "A rider can't reserve a bike far away" → `#E2.1`; "One rider, one bike" → `#E4.1`; "A reserved bike can't be taken by someone else" → `#E5.1`; "Only a technician can reserve a withdrawn bike" → `#E5.2` (all `spec: rider-reserves-bike`).

## Decisions

| # | Decision | Choice | Reason |
|---|---|---|---|
| D1 | TDD checkpoint loop | **off** | the trainer explicitly asked for a fully autonomous run (2026-11-08); the tactical skill's pushback is recorded in `docs/bounded-contexts/velocity/conversation.md`. Domain still built test-first, one example per cycle. |
| D2 | Aggregate style | state-based | simplest; no requirement for an event history |
| D3 | ACLs | one per outbound query/command, in `@AntiCorruptionLayer` packages, calling REST via `RestClient` + `VelocityServiceUrls`; inbound events via `@Async @EventListener` ACL listeners | trainer's integration style. Here: `accountsctxacl` for Rental Eligibility. |
| D4 | Persistence | in-memory repositories with `Version` optimistic locking, after the domain is green | user memory; trainer's brief |
| D5 | Delivery order | second of the eight Rentals plans (see `fleet-manager-adds-bike-to-fleet` D7); E4.2 and E5.2 are deferred to S7 | E4.2 needs a running session (`rider-unlocks-reserved-bike`), E5.2 a withdrawn bike (`fleet-operations-withdraws-bike-signalling-fault`) |
| D6 | Where eligibility is asked | through the `RentalEligibility` port, called by the use case (or a domain service the tactical skill introduces) before the bike decides; whether local checks run first is the tactical skill's call | keeps the REST call out of the aggregate; the aggregate receives the answer |
| D7 | Accounts unavailable | the reservation is rejected (fail closed), `503` from Rentals | the vision: "only an activated account can reserve"; not knowing means not allowing |

## Steps

### S1: Update the Rentals canvas
- **Status:** done
- **Context:** Rentals
- **Skill:** none (this skill applies the canvas diff above)
- **Covers:** E2.2, E3.1, E3.2, E4.2 (canvas decisions), tags for E1.1, E2.1, E4.1, E5.1, E5.2
- **Done when:** the canvas contains the diff from "Canvas changes" exactly.

### S2: Scaffold the eligibility ACL and the API records (6a)
- **Status:** done
- **Context:** Rentals
- **Skill:** bounded-context-scaffolder (update mode, Step 6a), told that this plan settles the structural decisions
- **Covers:** Command: Reserve Bike (record), Query: Rider's Active Rental (records), R3 (port + ACL)
- **Done when:** `api/usecases/ReserveBike`, `api/queries/RidersActiveRentalQuery` + `RidersActiveRental`, port `RentalEligibility`, adapter `infra/adapters/secondary/accountsctxacl` (package-info with `@AntiCorruptionLayer`), its wiring in `RentalsContextConfig`, and an adapter test against a stubbed HTTP response exist; `RentalsArchitectureTest` green; changelog line in the Rentals context plan; `./mvnw test` green.

### S3: Domain: reserving a free, available bike nearby (run 1 of 2)
- **Status:** done
- **Context:** Rentals
- **Skill:** ddd-tactical-building-blocks (checkpoint loop off, D1)
- **Covers:** R1, R2, R5, E1.1, E2.1, E2.2, E5.1
- **Done when:** one unit test per example, written first, passes; the reservation ends at reserved at + 2 minutes; `./mvnw test` green.

### S4: Domain: eligibility and one rider, one bike (run 2 of 2)
- **Status:** done
- **Context:** Rentals
- **Skill:** ddd-tactical-building-blocks (checkpoint loop off, D1)
- **Covers:** R3, R4, E3.1, E3.2, E4.1
- **Done when:** one unit test per example passes (E3.1/E3.2 as "the eligibility port answers *not eligible*"); the tactical skill has decided and recorded how R4 is enforced across bikes; `./mvnw test` green.

### S5: Use case, query, REST and wiring (6b)
- **Status:** done
- **Context:** Rentals
- **Skill:** bounded-context-scaffolder (Step 6b)
- **Covers:** Command: Reserve Bike, Query: Rider's Active Rental, Event: Bike Reserved
- **Done when:** `ReserveBikeUseCase` (eligibility via port, load, execute, save, publish), `RidersActiveRentalQueryHandler`, REST `POST /rentals/bikes/{bikeId}/reservations` and `GET /rentals/riders/{riderId}/active-rental` with the error mapping above; `./mvnw test` green.

### S6: Acceptance tests for E1.1-E4.1 and E5.1
- **Status:** done
- **Context:** Rentals (Accounts over REST)
- **Skill:** none (this skill)
- **Covers:** E1.1, E2.1, E2.2, E3.1, E3.2, E4.1, E5.1
- **Done when:** prerequisite: Accounts serves onboarding, `GET /accounts/riders/{riderId}/rental-eligibility` and `POST /accounts/riders/{riderId}/blocks` (the Accounts plans: `person-registers-with-velocity` … `rider-signs-user-agreement`, and the plan that adds Block Account); `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/RiderReservesBikeSpecTest.java` is a `@VelocitySpringTest` with real beans: riders are onboarded through Accounts' REST API (a small test fixture in the `rentals` test package), bikes added with `POST /rentals/bikes`, time set with `MutableClock`; one test method per example asserting through `GET /rentals/bikes/{bikeId}` and `GET /rentals/riders/{riderId}/active-rental`; `./mvnw test` green.

### S7: Deferred examples: a running ride (E4.2) and a withdrawn bike (E5.2)
- **Status:** done
- **Context:** Rentals
- **Skill:** ddd-tactical-building-blocks (domain scenarios, if the earlier rules don't already cover them), then this skill (acceptance tests)
- **Covers:** R4, R5, E4.2, E5.2
- **Done when:** prerequisites `docs/plans/rider-unlocks-reserved-bike.md` S5 (E4.2) and `docs/plans/fleet-operations-withdraws-bike-signalling-fault.md` S5 (E5.2) are done; a domain test per example passes; `RiderReservesBikeSpecTest#e4_2…` starts a session through reserve + unlock, `#e5_2…` withdraws the bike through `POST /rentals/bikes/{bikeId}/fault-signals`; `./mvnw test` green.

## Acceptance tests

| Example | Test |
|---|---|
| E1.1 | `RiderReservesBikeSpecTest#e1_1_eligible_rider_nearby_reserves_available_bike_for_two_minutes` |
| E2.1 | `RiderReservesBikeSpecTest#e2_1_rider_400_m_away_is_rejected` |
| E2.2 | `RiderReservesBikeSpecTest#e2_2_rider_exactly_50_m_away_reserves` |
| E3.1 | `RiderReservesBikeSpecTest#e3_1_rider_without_payment_method_is_rejected` |
| E3.2 | `RiderReservesBikeSpecTest#e3_2_blocked_rider_is_rejected` |
| E4.1 | `RiderReservesBikeSpecTest#e4_1_rider_holding_a_reservation_cannot_reserve_another_bike` |
| E4.2 | `RiderReservesBikeSpecTest#e4_2_rider_in_running_ride_cannot_reserve` |
| E5.1 | `RiderReservesBikeSpecTest#e5_1_bike_reserved_for_someone_else_is_rejected` |
| E5.2 | `RiderReservesBikeSpecTest#e5_2_withdrawn_bike_is_rejected` |

## Traceability

| Spec ID | Step | Files |
|---|---|---|
| R1 / E1.1 | S3, S5, S6 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/Bike.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/BikeReservationTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/api/usecases/ReserveBikeUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/secondary/accountsctxacl/AccountsRentalEligibilityAdapter.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/primary/RentalsController.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/RiderReservesBikeSpecTest.java` |
| R2 / E2.1 | S3, S5, S6 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/Bike.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/BikeReservationTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/api/usecases/ReserveBikeUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/secondary/accountsctxacl/AccountsRentalEligibilityAdapter.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/primary/RentalsController.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/RiderReservesBikeSpecTest.java` |
| R2 / E2.2 | S1, S3, S5, S6 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/Bike.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/BikeReservationTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/api/usecases/ReserveBikeUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/secondary/accountsctxacl/AccountsRentalEligibilityAdapter.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/primary/RentalsController.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/RiderReservesBikeSpecTest.java` |
| R3 / E3.1 | S1, S2, S4, S6 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/Bike.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/BikeReservationTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/api/usecases/ReserveBikeUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/secondary/accountsctxacl/AccountsRentalEligibilityAdapter.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/primary/RentalsController.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/RiderReservesBikeSpecTest.java` |
| R3 / E3.2 | S1, S2, S4, S6 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/Bike.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/BikeReservationTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/api/usecases/ReserveBikeUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/secondary/accountsctxacl/AccountsRentalEligibilityAdapter.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/primary/RentalsController.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/RiderReservesBikeSpecTest.java` |
| R4 / E4.1 | S4, S6 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/Bike.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/BikeReservationTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/api/usecases/ReserveBikeUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/secondary/accountsctxacl/AccountsRentalEligibilityAdapter.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/primary/RentalsController.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/RiderReservesBikeSpecTest.java` |
| R4 / E4.2 | S1, S7 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/Bike.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/BikeReservationTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/api/usecases/ReserveBikeUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/secondary/accountsctxacl/AccountsRentalEligibilityAdapter.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/primary/RentalsController.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/RiderReservesBikeSpecTest.java` |
| R5 / E5.1 | S3, S6 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/Bike.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/BikeReservationTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/api/usecases/ReserveBikeUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/secondary/accountsctxacl/AccountsRentalEligibilityAdapter.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/primary/RentalsController.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/RiderReservesBikeSpecTest.java` |
| R5 / E5.2 | S7 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/Bike.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/BikeReservationTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/api/usecases/ReserveBikeUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/secondary/accountsctxacl/AccountsRentalEligibilityAdapter.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/primary/RentalsController.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/RiderReservesBikeSpecTest.java` |

## Risks and open questions

- **Q1 (open, accepted):** is exactly 50 m "near"? E2.2 takes the literal reading (yes). Risk: floating-point distance. The domain should compare a distance rounded to a fixed precision (e.g. centimetres), and the test should place the rider with the same distance formula; the tactical skill records its choice.
- **R4 spans bikes.** "One rider, one bike" isn't a single-bike invariant if one aggregate per bike is chosen (`fleet-manager-adds-bike-to-fleet` D6). The tactical skill decides between a check through the repository (with a known race between two concurrent reservations by the same rider) and a per-rider holding; it records the choice and its consistency trade-off in S4.
- **Dependency on other agents' plans:** no Accounts intent names Rental Eligibility as its own outcome; integration-contracts.md defines it. S6 assumes the Accounts plans serve it (likely with `finance-blocks-rider-with-invalid-card`, whose spec defines "Eligible" (DE-93)). If not, the orchestrator must assign it.
- The technician variant of Reserve Bike (same endpoint, `holderType: "TECHNICIAN"`, withdrawn bikes only, 30 min) is built by the `technician-reserves-broken-bike` plan, on top of S3-S5.

## Review notes

- 2026-11-08: draft written (autonomous run); self-review against plan-critic.md checklist: AGREE. Placement follows the CML (Accounts → Rentals ACL, direction correct, no new relationship); R3 kept as a precondition, not copied into Rentals; tactical runs ≤ 3 rules; deferred examples have explicit prerequisites; R4's cross-bike nature left to the tactical skill (not decided here).
- 2026-11-08 (Rentals wave, autonomous run): Decisions (tactical, S4): R4 "one rider, one bike" spans bikes, so the use case gathers a `RiderStanding` (Accounts' eligibility through the port + `BikeRepository.holdsABike(rider, now)`) and passes it into `Bike.reserveForRide(..)`; the bike decides. Accepted race: two concurrent reservations by the same rider on different bikes can both pass (recorded in the context plan). A reservation that ran out but wasn't released yet no longer counts as holding a bike. Accounts unavailable → `503 RentalEligibilityUnknown` (D7). Response `201 {bikeId, holderType, holderId, reservedAt, expiresAt}` per integration-contracts.md Additions.

## Changelog

### 2026-11-08: planned
- Built from spec dated 2026-11-08.

### 2026-11-08: Rentals wave (autonomous run)
- Rentals steps built test-first (TDD checkpoint loop off, D1); per-cycle log in `docs/bounded-contexts/velocity/rentals/plan.md`. Files and tests in the Traceability table; full suite green.
