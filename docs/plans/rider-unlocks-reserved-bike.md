---
name: rider-unlocks-reserved-bike
intent: rider-unlocks-reserved-bike
spec: rider-unlocks-reserved-bike
system: velocity
status: done
date: 2026-11-08
spec_date: 2026-11-08
---

# Implementation plan: A rider wants to unlock the bike they reserved and start riding

<!-- Reviewers: edit this file or answer in the conversation, then set status to "approved" (or say so).
     Invoking implementation-planner again resumes from this file. -->

## Summary

The intent lands in **Rentals** (CML responsibilities "Reserve and unlock", "Rental sessions"; canvas inbound "Rider app |
Command | Unlock Bike"). The rider's eligibility is re-checked in Accounts (DE-80) through the `accountsctxacl` adapter
built by `rider-reserves-bike`. The lock itself is hardware behind a new `BikeLocks` port (`bikelocksacl`, simulated).
Rentals publishes `UnlockFailed` and `BikeUnlocked` as integration events because Fleet Maintenance listens (for
technicians); a rider's successful unlock starts a rental session, which stays internal.

## Placement

| Spec element | Owner | Why |
|---|---|---|
| R1 The rider holding the reservation unlocks the bike, and the ride starts | Rentals | reservation and rental session are Rentals' state (CML "Reserve and unlock", "Rental sessions"; canvas decision "A rider's unlock starts a rental session") |
| R2 A failed unlock releases the reservation and the bike is available again | Rentals | the hold is Rentals' state; the lock's answer comes from the bike locks port (canvas decision "A failed unlock releases the reservation", DE-15) |
| R3 Only the holder of a reservation that hasn't run out can unlock the bike | Rentals | guards the same hold as R1 (canvas Enforcer role; DE-81 boundary) |
| R4 An ineligible rider's unlock is rejected, and nothing changes | Rentals applies it, Accounts decides eligibility | precondition through the ACL query, as in `rider-reserves-bike` R3 (ownership table "Command"; canvas decision "An ineligible rider can't unlock", DE-80) |
| Command: Unlock Bike | Rentals | "who can say no?": R3 in Rentals, R4 as a precondition (canvas inbound) |
| Event: Rental Session Started | Rentals | the session begins in Rentals; no listener outside (canvas outbound "none outside Rentals yet") |
| Event: Unlock Failed | Rentals | Rentals decided the unlock failed; Fleet Maintenance listens for technicians (canvas outbound; integration-contracts.md) |

**Outcome:** one existing context (Rentals), with Accounts' eligibility as a precondition over the existing ACL relationship.

## Context map changes

- None. `Accounts -> Rentals : AccountStatus` (ACL) covers eligibility; `Rentals -> FleetMaintenance : BikeAvailability` covers `UnlockFailed`/`BikeUnlocked`. Bike locks are hardware, not a bounded context.

## Translation

| Spec term | Context | Context term | Where translated |
|---|---|---|---|
| Rider | Rentals | holder of type *rider* | REST adapter (`{holderType: "RIDER", holderId}`) |
| Ride | Rentals | rental session | canvas UL |
| Reservation is used up | Rentals | the hold turns from *reserved* to *unlocked*, and a rental session runs | domain |
| The lock opens / doesn't open | Rentals | the `BikeLocks` port's answer | `bikelocksacl` adapter (simulated hardware) |
| Ineligible | Rentals | not eligible to rent | `accountsctxacl` (from `rider-reserves-bike`) |

## Contract mapping

| Spec element | Context | Code |
|---|---|---|
| Command: Unlock Bike | Rentals | `UnlockBikeUseCase` + record `UnlockBike(bikeId, holderType, holderId)` in `rentals/api/usecases`; `POST /rentals/bikes/{bikeId}/unlock` `{holderType, holderId}` → `200 {outcome: "SESSION_STARTED"\|"UNLOCK_FAILED", sessionId?}`; rejections `409` `NotReservationHolder` (R3: other holder, no reservation, reservation period over), `RiderNotEligible` (R4); unknown bike `404` |
| Event: Rental Session Started | Rentals | domain event `RentalSessionStarted` (session, rider, bike, station or position, started at); **no** integration event (no listener) |
| Event: Unlock Failed | Rentals | domain event `UnlockFailed`; integration event `rentals.api.events.UnlockFailed {bikeId, holderType, holderId, failedAt}` (record exists), published through `ApplicationEventPublisher` for every holder; Fleet Maintenance's listener keeps technicians only |
| (technician unlock) | Rentals | integration event `rentals.api.events.BikeUnlocked` is published only when a *technician's* unlock succeeds, which the `technician-starts-maintenance` plan adds; this plan maps nothing to it |
| Lock hardware | Rentals | secondary port `BikeLocks` (open / close a bike's lock, answer opened or failed); adapter `infra/adapters/secondary/bikelocksacl/SimulatedBikeLocks` (`@AntiCorruptionLayer`): fails for bikes registered as faulty on it (integration-contracts.md) |
| Precondition R4 | Rentals → Accounts | port `RentalEligibility` + `accountsctxacl` adapter, reused from `rider-reserves-bike` |

## Canvas changes

### Rentals (`docs/bounded-contexts/velocity/rentals/canvas.md`)

- **Business decisions, add:**
  - `### After a failed unlock the rider may reserve another bike (spec: rider-unlocks-reserved-bike#E2.2)`: **Given** Anna's unlock of B-17 failed at 10:01 **When** Anna, standing 10 m from available B-22, reserves B-22 at 10:01:30 **Then** B-22 is reserved for Anna.
  - `### No unlock without a reservation that still holds (spec: rider-unlocks-reserved-bike#E3.2, #E3.3)`: **Given** Anna holds no reservation for available B-22, and Anna's reservation of B-17 ran until 10:02 but wasn't released yet **When** Anna unlocks B-22, or unlocks B-17 at 10:02:30 **Then** the unlock is rejected and the lock stays closed.
- **Tag existing decisions:** "A rider's unlock starts a rental session" → `#E1.1`; "An expired reservation releases the bike", boundary "Anna can still unlock at 10:02:00" → `#E1.2`; "A failed unlock releases the reservation" → `#E2.1`; "A reserved bike can't be taken by someone else" → `#E3.1`; "An ineligible rider can't unlock" → `#E4.1` (all `spec: rider-unlocks-reserved-bike`).

## Decisions

| # | Decision | Choice | Reason |
|---|---|---|---|
| D1 | TDD checkpoint loop | **off** | the trainer explicitly asked for a fully autonomous run (2026-11-08); the tactical skill's pushback is recorded in `docs/bounded-contexts/velocity/conversation.md`. Domain still built test-first, one example per cycle. |
| D2 | Aggregate style | state-based | simplest; no requirement for an event history |
| D3 | ACLs | one per outbound query/command, in `@AntiCorruptionLayer` packages, calling REST via `RestClient` + `VelocityServiceUrls`; inbound events via `@Async @EventListener` ACL listeners | trainer's integration style. Here: `accountsctxacl` (reused), `bikelocksacl` (hardware, simulated). |
| D4 | Persistence | in-memory repositories with `Version` optimistic locking, after the domain is green | user memory; trainer's brief |
| D5 | Delivery order | third of the eight Rentals plans (`fleet-manager-adds-bike-to-fleet` D7) | needs reservations from `rider-reserves-bike` |
| D6 | Integration events for every holder | `UnlockFailed` (and later `ReservationExpired`) are published for riders and technicians alike; the Fleet Maintenance listener filters on `holderType` | an integration event states a fact; filtering is the consumer's ACL concern. `BikeUnlocked` is the exception: a rider's unlock is a *Rental Session Started*, not a published fact |
| D7 | The rider is told the outcome | the REST answer of Unlock Bike states `SESSION_STARTED` or `UNLOCK_FAILED` | the vision: "the rider is free to try another bike"; see CONTRACT GAP below |

## Steps

### S1: Update the Rentals canvas
- **Status:** done
- **Context:** Rentals
- **Skill:** none (this skill applies the canvas diff above)
- **Covers:** E2.2, E3.2, E3.3 (canvas decisions), tags for E1.1, E1.2, E2.1, E3.1, E4.1
- **Done when:** the canvas contains the diff from "Canvas changes" exactly.

### S2: Scaffold the bike locks port, the command record and the published events (6a)
- **Status:** done
- **Context:** Rentals
- **Skill:** bounded-context-scaffolder (update mode, Step 6a)
- **Covers:** Command: Unlock Bike (record), Event: Unlock Failed (integration record, exists)
- **Done when:** `api/usecases/UnlockBike`, port `BikeLocks`, adapter `infra/adapters/secondary/bikelocksacl/SimulatedBikeLocks` (with a way for tests to register a bike as faulty) and its wiring exist; `api/events/UnlockFailed` checked against integration-contracts.md; `RentalsArchitectureTest` green; `./mvnw test` green.

### S3: Domain: the holder unlocks within the reservation period (run 1 of 2)
- **Status:** done
- **Context:** Rentals
- **Skill:** ddd-tactical-building-blocks (checkpoint loop off, D1)
- **Covers:** R1, R3, E1.1, E1.2, E3.1, E3.2, E3.3
- **Done when:** one unit test per example, written first, passes (E1.2 at exactly 10:02:00 succeeds, E3.3 at 10:02:30 is rejected); a session gets its own identity; `./mvnw test` green.

### S4: Domain: failed unlock and eligibility re-check (run 2 of 2)
- **Status:** done
- **Context:** Rentals
- **Skill:** ddd-tactical-building-blocks (checkpoint loop off, D1)
- **Covers:** R2, R4, E2.1, E2.2, E4.1
- **Done when:** one unit test per example passes; after a failed unlock the bike is free and available and the rider holds nothing (so "one rider, one bike" from `rider-reserves-bike` lets them reserve again); an ineligible rider's unlock changes nothing; `./mvnw test` green.

### S5: Use case, REST, event publishing and wiring (6b)
- **Status:** done
- **Context:** Rentals
- **Skill:** bounded-context-scaffolder (Step 6b)
- **Covers:** Command: Unlock Bike, Event: Rental Session Started, Event: Unlock Failed
- **Done when:** `UnlockBikeUseCase` (eligibility via port for riders, load, open the lock via `BikeLocks`, execute, save, publish domain events, map `UnlockFailed` to the integration event), REST `POST /rentals/bikes/{bikeId}/unlock` with the error mapping above; `GET /rentals/riders/{riderId}/active-rental` reports `kind: "SESSION"` with `sessionId`; `./mvnw test` green.

### S6: Acceptance tests for every example
- **Status:** done
- **Context:** Rentals (Accounts and Payments over REST)
- **Skill:** none (this skill)
- **Covers:** all examples
- **Done when:** prerequisite for E4.1: the `finance-blocks-rider-with-invalid-card` plan has delivered blocking on `POST /payments/processor-notifications/card-invalidations`; `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/RiderUnlocksReservedBikeSpecTest.java` is a `@VelocitySpringTest` with real beans, one test method per example; E2.1 registers the bike as faulty on `SimulatedBikeLocks` and asserts the published `UnlockFailed` integration event with `@RecordApplicationEvents` and Awaitility; E4.1 invalidates Anna's card through Payments and waits with Awaitility until Accounts reports her blocked before unlocking; `./mvnw test` green.

## Acceptance tests

| Example | Test |
|---|---|
| E1.1 | `RiderUnlocksReservedBikeSpecTest#e1_1_holder_unlocks_and_session_starts` |
| E1.2 | `RiderUnlocksReservedBikeSpecTest#e1_2_unlock_at_exact_end_of_reservation_starts_session` |
| E2.1 | `RiderUnlocksReservedBikeSpecTest#e2_1_failed_unlock_releases_reservation_and_bike_is_available` |
| E2.2 | `RiderUnlocksReservedBikeSpecTest#e2_2_after_failed_unlock_rider_reserves_another_bike` |
| E3.1 | `RiderUnlocksReservedBikeSpecTest#e3_1_other_rider_cannot_unlock_reserved_bike` |
| E3.2 | `RiderUnlocksReservedBikeSpecTest#e3_2_unlock_without_reservation_is_rejected` |
| E3.3 | `RiderUnlocksReservedBikeSpecTest#e3_3_unlock_after_reservation_period_is_rejected` |
| E4.1 | `RiderUnlocksReservedBikeSpecTest#e4_1_rider_blocked_during_reservation_cannot_unlock` |

## Traceability

| Spec ID | Step | Files |
|---|---|---|
| R1 / E1.1 | S3, S5, S6 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/Bike.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/BikeUnlockTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/api/usecases/UnlockBikeUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/secondary/bikelocksacl/SimulatedBikeLocks.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/primary/RentalsController.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/RiderUnlocksReservedBikeSpecTest.java` |
| R1 / E1.2 | S3, S5, S6 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/Bike.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/BikeUnlockTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/api/usecases/UnlockBikeUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/secondary/bikelocksacl/SimulatedBikeLocks.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/primary/RentalsController.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/RiderUnlocksReservedBikeSpecTest.java` |
| R2 / E2.1 | S4, S5, S6 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/Bike.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/BikeUnlockTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/api/usecases/UnlockBikeUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/secondary/bikelocksacl/SimulatedBikeLocks.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/primary/RentalsController.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/RiderUnlocksReservedBikeSpecTest.java` |
| R2 / E2.2 | S1, S4, S6 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/Bike.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/BikeUnlockTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/api/usecases/UnlockBikeUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/secondary/bikelocksacl/SimulatedBikeLocks.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/primary/RentalsController.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/RiderUnlocksReservedBikeSpecTest.java` |
| R3 / E3.1 | S3, S6 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/Bike.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/BikeUnlockTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/api/usecases/UnlockBikeUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/secondary/bikelocksacl/SimulatedBikeLocks.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/primary/RentalsController.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/RiderUnlocksReservedBikeSpecTest.java` |
| R3 / E3.2 | S1, S3, S6 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/Bike.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/BikeUnlockTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/api/usecases/UnlockBikeUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/secondary/bikelocksacl/SimulatedBikeLocks.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/primary/RentalsController.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/RiderUnlocksReservedBikeSpecTest.java` |
| R3 / E3.3 | S1, S3, S6 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/Bike.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/BikeUnlockTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/api/usecases/UnlockBikeUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/secondary/bikelocksacl/SimulatedBikeLocks.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/primary/RentalsController.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/RiderUnlocksReservedBikeSpecTest.java` |
| R4 / E4.1 | S4, S5, S6 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/Bike.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/BikeUnlockTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/api/usecases/UnlockBikeUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/secondary/bikelocksacl/SimulatedBikeLocks.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/primary/RentalsController.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/RiderUnlocksReservedBikeSpecTest.java` |

## Risks and open questions

- **CONTRACT GAP:** integration-contracts.md lists no response body for `POST /rentals/bikes/{bikeId}/unlock`. The plan assumes `200 {outcome: "SESSION_STARTED"|"UNLOCK_FAILED", sessionId?}` so the rider learns whether the ride started (a failed unlock is a business outcome, not an error).
- **Intent text vs spec:** the intent's "Business rules mentioned" still says "the account isn't checked again at unlock" (DE-14); the approved spec revises it (R4, DE-80). The plan follows the spec; the intent wording should be refreshed by `intent-creator`.
- `SimulatedBikeLocks` is one bean for all tests in the shared context: tests register only their own (unique) bike ids as faulty.
- The vision's "a bike can never be in two active rental sessions" holds by construction if one aggregate per bike is chosen; if the tactical skill chooses otherwise it must show how.

## Review notes

- 2026-11-08: draft written (autonomous run); self-review against plan-critic.md checklist: AGREE. Single owner, precondition via existing ACL; integration event only where Fleet Maintenance listens; ≤ 3 rules per tactical run (R1+R3 guard the hold, R2+R4 the unlock outcome); E2.2 relies on `rider-reserves-bike`, delivered earlier (D5).
- 2026-11-08 (Rentals wave, autonomous run): Decisions: the unlock answer follows integration-contracts.md Additions, `200 {bikeId, outcome: "UNLOCKED"|"UNLOCK_FAILED", sessionId?}` (the plan's `SESSION_STARTED` was overridden by the contract). The bike calls the `BikeLocks` port itself (`unlockForRide(rider).given(eligibility).at(now).using(locks)`) only after the holder and eligibility checks, so a rejected unlock never touches the lock. `SimulatedBikeLocks` lets tests `registerFaulty(bikeId)` or `scriptNext(bikeId, results...)`. Published-event assertions use a recording listener on the multicaster (events are published on server threads, `@RecordApplicationEvents` wouldn't see them) with Awaitility.

## Changelog

### 2026-11-08: planned
- Built from spec dated 2026-11-08.

### 2026-11-08: Rentals wave (autonomous run)
- Rentals steps built test-first (TDD checkpoint loop off, D1); per-cycle log in `docs/bounded-contexts/velocity/rentals/plan.md`. Files and tests in the Traceability table; full suite green.
