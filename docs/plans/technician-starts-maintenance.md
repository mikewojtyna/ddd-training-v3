---
name: technician-starts-maintenance
intent: technician-starts-maintenance
spec: technician-starts-maintenance
system: velocity
status: in progress
date: 2026-11-08
spec_date: 2026-11-08
---

# Implementation plan: A technician wants to unlock a reserved broken bike and start maintenance

<!-- Reviewers: edit this file or answer in the conversation, then set status to "approved" (or say so).
     Invoking implementation-planner again resumes from this file. -->

## Summary

The technician asks **FleetMaintenance** to unlock the broken bike they reserved; FleetMaintenance forwards it to
**Rentals'** OHS *Unlock Bike* with a technician holder. Rentals owns the unlock: only the holder of an unexpired
reservation may unlock (R3), a technician's unlock never starts a rental session (R2), and a failed lock releases
the reservation while the bike stays withdrawn (R4). FleetMaintenance starts maintenance when Rentals publishes
`BikeUnlocked` for a technician (R1) and forgets the reservation on `UnlockFailed`. Technician unlocks aren't
covered by any Rentals intent, so the Rentals part is planned here. No context map change. Order: canvases →
Rentals rules and REST/events → FM additions → acceptance tests.

## Placement

| Spec element | Owner | Why |
|---|---|---|
| R1 A technician's unlock of the broken bike they reserved starts maintenance | FleetMaintenance (maintenance started); Rentals (the unlock, Bike Unlocked) | FM canvas "A technician's unlock starts maintenance", CML "Maintenance"; the lock and the hold are Rentals' (CML "Reserve and unlock") |
| R2 A technician's unlock never starts a rental session and is never billed | Rentals | only Rentals can start a session; no session → no `RentalSessionEnded` → Settlement never prices or charges (Rentals canvas "A technician's unlock never starts a session") |
| R3 Only the technician holding an unexpired reservation of the bike can unlock it | Rentals | "who can say no?": Rentals holds the reservation (Rentals canvas "A reserved bike can't be taken by someone else", DE-81); FM translates Rentals' 409 |
| R4 A failed unlock releases the reservation and the bike stays broken | Rentals (reservation released, bike stays withdrawn); FleetMaintenance (bike stays broken, any technician may reserve) | Rentals canvas "A failed unlock releases the reservation / technician variant"; FM canvas "Reservation Expired / Unlock Failed … bike stays broken" |
| Command: Unlock Broken Bike | FleetMaintenance (technician-facing), forwarded to Rentals' Unlock Bike | Rentals can say no (R3) and is called as a precondition through the ACL |
| Event: Bike Unlocked | Rentals | the lock opened in Rentals; published `rentals.api.events.BikeUnlocked` (listener: Fleet Maintenance, technicians only) |
| Event: Maintenance Started | FleetMaintenance | the fact happens in FM; internal |
| Event: Unlock Failed | Rentals | decided in Rentals; published `rentals.api.events.UnlockFailed` (listener: Fleet Maintenance, technicians only) |

**Outcome:** several existing contexts (FleetMaintenance, Rentals; Settlement only as a non-listener for R2).

Hand-offs (CML `Rentals [U,OHS,PL]->[D,ACL] FleetMaintenance`):
1. FM calls Rentals' OHS *Unlock Bike* (`POST /rentals/bikes/{bikeId}/unlock`, `holderType: TECHNICIAN`).
2. Rentals publishes `BikeUnlocked` / `UnlockFailed`; FM's ACL listeners react for technician holders.

## Context map changes

- None.

## Translation

| Spec term | Context | Context term | Where translated |
|---|---|---|---|
| Technician | Rentals | Holder of type `TECHNICIAN` | FM `rentalsctxacl` adapter / listeners |
| Unlock | Rentals | Unlock (by a technician holder: no rental session) | – |
| Under maintenance | FleetMaintenance | Under maintenance (maintenance started) | listener `BikeUnlocked` → Start Maintenance |
| Under maintenance | Rentals | Withdrawn bike unlocked for a technician | – |
| Reservation released | FleetMaintenance | technician reservation forgotten, bike broken | listener `UnlockFailed` → Release Technician Reservation |

## Contract mapping

| Spec element | Context | Code |
|---|---|---|
| Command: Unlock Broken Bike | FleetMaintenance | `UnlockBrokenBikeUseCase` + record `UnlockBrokenBike(bikeId, technicianId)`; `POST /fleet-maintenance/bikes/{bikeId}/unlock` `{technicianId}` (409 translated from Rentals for R3); secondary port `ServiceUnlocks`, adapter `infra/adapters/secondary/rentalsctxacl/RentalsServiceUnlocksAdapter` |
| Command: Unlock Broken Bike (Rentals side) | Rentals | existing `UnlockBikeUseCase` + `UnlockBike` (from `rider-unlocks-reserved-bike`) extended with a technician holder: no eligibility check, no session; `POST /rentals/bikes/{bikeId}/unlock` `{holderType: "TECHNICIAN", holderId}`; lock through port `BikeLocks` (`bikelocksacl`) |
| Event: Bike Unlocked | Rentals | domain event `BikeUnlocked` → existing `rentals.api.events.BikeUnlocked`; FM listener `infra/adapters/primary/rentalsctxacl/BikeUnlockedListener` (`@Async @EventListener`, technicians only) → `StartMaintenanceUseCase` + record `StartMaintenance(bikeId, technicianId, startedAt)` |
| Event: Maintenance Started | FleetMaintenance | domain event `MaintenanceStarted`; no integration event |
| Event: Unlock Failed | Rentals | domain event `UnlockFailed` → existing `rentals.api.events.UnlockFailed`; FM listener `infra/adapters/primary/rentalsctxacl/UnlockFailedListener` (technicians only) → `ReleaseTechnicianReservationUseCase` (from `technician-reserves-broken-bike`) |

## Canvas changes

### FleetMaintenance (`docs/bounded-contexts/velocity/fleetmaintenance/canvas.md`)

- **Business decisions, tag:** "A technician's unlock starts maintenance" → `(spec: technician-starts-maintenance#E1.1)`.
- **Business decisions, add:** *A failed unlock leaves the bike broken*: **Given** B-17 is broken and reserved for Tom until 09:30, **When** Rentals reports Tom's unlock of B-17 failed at 09:20, **Then** no maintenance starts, B-17 is still broken and any technician can reserve it (spec: technician-starts-maintenance#E4.1).

### Rentals (`docs/bounded-contexts/velocity/rentals/canvas.md`)

- **Business decisions, tag:** "A technician's unlock never starts a session" → `(spec: technician-starts-maintenance#E1.1, #E2.1)`; "A failed unlock releases the reservation" technician variant → `#E4.1`.
- **Business decisions, add** (to "A technician's unlock never starts a session"): *Wrong or expired holder:* B-17 reserved for Tom until 09:30; Tara unlocks at 09:20 → rejected, still reserved for Tom; after Tom's reservation expired at 09:30, Tom unlocks at 09:35 → rejected (spec: technician-starts-maintenance#E3.1, #E3.2).

## Decisions

| # | Decision | Choice | Reason |
|---|---|---|---|
| D1 | TDD checkpoint loop | **off** | the trainer explicitly asked for a fully autonomous run (2026-11-08); the tactical skill's pushback is recorded in `docs/bounded-contexts/velocity/conversation.md`. Domain still built test-first, one example per cycle |
| D2 | Aggregate style | state-based | simplest; no requirement for an event history |
| D3 | ACLs | one per outbound query/command, in `@AntiCorruptionLayer` packages, calling REST via `RestClient` + `VelocityServiceUrls`; inbound events via `@Async @EventListener` ACL listeners | integration-contracts.md, CML `[D,ACL]` |
| D4 | Persistence | in-memory repositories with `Version` optimistic locking, after the domain is green | trainer's standing instruction |
| D5 | Where maintenance starts | on Rentals' `BikeUnlocked` event, not on the REST answer of the unlock call | the event is the published fact (FM canvas "Maintenance started: the moment a technician's unlock … is reported"); keeps FM correct even if a technician unlocks through another client |
| D6 | Technician unlock and Accounts | a technician's unlock skips the rider eligibility check (DE-80 is about riders) | technicians aren't riders in Accounts |

## Steps

### S1: Canvas updates for FleetMaintenance and Rentals
- **Status:** todo
- **Context:** FleetMaintenance, Rentals
- **Skill:** none (apply the "Canvas changes" diff exactly)
- **Prerequisite:** `technician-reserves-broken-bike` delivered.
- **Covers:** E3.1, E3.2, E4.1 (canvas only)
- **Done when:** both canvases carry the tags and additions above.

### S2: Rentals: technician unlocks
- **Status:** done
- **Context:** Rentals
- **Skill:** ddd-tactical-building-blocks (checkpoint loop off, D1), via bounded-context-scaffolder update mode
- **Prerequisite:** the unlock and `BikeLocks` port of `rider-unlocks-reserved-bike`.
- **Covers:** R2, R3, R4, E2.1, E3.1, E3.2, E4.1
- **Done when:** one domain test per scenario (holder's unlock opens the lock, no session; another technician rejected; expired holder rejected; failed lock releases the reservation, bike stays withdrawn); `./mvnw test` green.

### S3: Rentals: REST and published events for technician unlocks (6b)
- **Status:** done
- **Context:** Rentals
- **Skill:** bounded-context-scaffolder (6b)
- **Covers:** Bike Unlocked, Unlock Failed
- **Done when:** `POST /rentals/bikes/{bikeId}/unlock` accepts `{holderType: "TECHNICIAN", holderId}` (D6), `BikeUnlocked` / `UnlockFailed` for technician holders published as `rentals.api.events.*`; REST-level test passes; `./mvnw test` green.

### S4: FleetMaintenance: port, adapter, records (6a)
- **Status:** todo
- **Context:** FleetMaintenance
- **Skill:** bounded-context-scaffolder (6a, update mode)
- **Covers:** none
- **Done when:** records `UnlockBrokenBike`, `StartMaintenance`; port `ServiceUnlocks` with `rentalsctxacl` adapter (409 → `NotYourReservation`); architecture test green; `./mvnw test` green.

### S5: FleetMaintenance: maintenance rules
- **Status:** todo
- **Context:** FleetMaintenance
- **Skill:** ddd-tactical-building-blocks (checkpoint loop off, D1)
- **Covers:** R1, R4, E1.1, E4.1
- **Done when:** domain tests: a technician's unlock of a broken bike starts maintenance (Maintenance Started at the unlock time); a failed unlock leaves the bike broken and unreserved; `./mvnw test` green.

### S6: FleetMaintenance: use cases, listeners, REST (6b)
- **Status:** todo
- **Context:** FleetMaintenance
- **Skill:** bounded-context-scaffolder (6b)
- **Covers:** Unlock Broken Bike, Maintenance Started
- **Done when:** `UnlockBrokenBikeUseCase`, `StartMaintenanceUseCase`, `BikeUnlockedListener`, `UnlockFailedListener` (technicians only), `POST /fleet-maintenance/bikes/{bikeId}/unlock`, `GET /fleet-maintenance/bikes/{bikeId}` reporting `UNDER_MAINTENANCE`; `./mvnw test` green.

### S7: Acceptance tests for the spec examples
- **Status:** todo
- **Context:** FleetMaintenance, Rentals
- **Skill:** none (this skill)
- **Covers:** all examples
- **Done when:** `TechnicianStartsMaintenanceSpecTest` (`@VelocitySpringTest`, real beans, `MutableClock`; lock failure through `SimulatedBikeLocks`; E2.1 asserts with `@RecordApplicationEvents` that no `RentalSessionEnded` is published and Rentals reports the bike `UNLOCKED` by a `TECHNICIAN`) has one test per example; the Rentals → FM `BikeUnlocked`/`UnlockFailed` hand-offs are asserted with Awaitility; `./mvnw test` green.

## Acceptance tests

| Example | Test |
|---|---|
| E1.1 | `TechnicianStartsMaintenanceSpecTest#e1_1_technician_unlock_starts_maintenance` |
| E2.1 | `TechnicianStartsMaintenanceSpecTest#e2_1_maintenance_starts_no_rental_session_and_no_charge` |
| E3.1 | `TechnicianStartsMaintenanceSpecTest#e3_1_other_technician_cannot_unlock` |
| E3.2 | `TechnicianStartsMaintenanceSpecTest#e3_2_expired_reservation_cannot_unlock` |
| E4.1 | `TechnicianStartsMaintenanceSpecTest#e4_1_failed_unlock_releases_reservation_bike_stays_broken` |

## Traceability

| Spec ID | Step | Files |
|---|---|---|
| R1 / E1.1 | S3, S5, S6, S7 | Rentals: `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/Bike.java` (`unlockForService`), `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/TechnicianServiceTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/api/usecases/UnlockBikeUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/secondary/IntegrationEventsPublisher.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/RentalsServiceForFleetMaintenanceRestTest.java` (`UnlockingForService`) |
| R2 / E2.1 | S2, S3, S7 | Rentals: `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/Bike.java` (`unlockForService`), `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/TechnicianServiceTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/api/usecases/UnlockBikeUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/secondary/IntegrationEventsPublisher.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/RentalsServiceForFleetMaintenanceRestTest.java` (`UnlockingForService`) |
| R3 / E3.1, E3.2 | S1, S2, S4, S7 | Rentals: `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/Bike.java` (`unlockForService`), `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/TechnicianServiceTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/api/usecases/UnlockBikeUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/secondary/IntegrationEventsPublisher.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/RentalsServiceForFleetMaintenanceRestTest.java` (`UnlockingForService`) |
| R4 / E4.1 | S1, S2, S3, S5, S6, S7 | Rentals: `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/Bike.java` (`unlockForService`), `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/TechnicianServiceTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/api/usecases/UnlockBikeUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/infra/adapters/secondary/IntegrationEventsPublisher.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/RentalsServiceForFleetMaintenanceRestTest.java` (`UnlockingForService`) |

## Risks and open questions

- CONTRACT GAP: the answer of `POST /rentals/bikes/{bikeId}/unlock` (and of FM's `POST /fleet-maintenance/bikes/{bikeId}/unlock`) doesn't say how a failed lock is reported. Proposed: `200 {bikeId, outcome: "UNLOCKED"|"UNLOCK_FAILED", sessionId?}`; FM relays `outcome` to the technician. If `rider-unlocks-reserved-bike`'s plan defines another shape, FM follows it.
- Spec Q1 (flag repeated failed technician unlocks) is open and not blocking; nothing is planned for it.
- R2's "never charged" can't be observed directly in Settlement for a technician (no rider); the test proves the cause (no session, no `RentalSessionEnded`).

## Review notes

- 2026-11-08: draft written (autonomous run); self-review against plan-critic.md checklist: AGREE (the unlock and its rules stay in Rentals, maintenance in FM; events follow the CML direction; every example covered; tactical steps ≤3 rules; no building blocks decided).
- 2026-11-08 (Rentals wave, autonomous run): Rentals wave: S2/S3 built. Technician unlock answers `200 {bikeId, outcome: "UNLOCKED"|"UNLOCK_FAILED"}` (no `sessionId`); someone else's or an expired reservation → `409 NotReservationHolder`. `BikeUnlocked` is published for technicians only; `UnlockFailed` for every holder. The Rentals half of S1's canvas diff is applied; S1 stays todo for the FleetMaintenance half.

## Changelog

### 2026-11-08: planned
- Built from spec dated 2026-11-08.

### 2026-11-08: Rentals wave (autonomous run)
- Rentals steps built test-first (TDD checkpoint loop off, D1); per-cycle log in `docs/bounded-contexts/velocity/rentals/plan.md`. Files and tests in the Traceability table; full suite green.
