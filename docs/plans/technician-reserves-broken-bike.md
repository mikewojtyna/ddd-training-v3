---
name: technician-reserves-broken-bike
intent: technician-reserves-broken-bike
spec: technician-reserves-broken-bike
system: velocity
status: approved
date: 2026-11-08
spec_date: 2026-11-08
---

# Implementation plan: A technician wants to reserve a broken bike

<!-- Reviewers: edit this file or answer in the conversation, then set status to "approved" (or say so).
     Invoking implementation-planner again resumes from this file. -->

## Summary

The technician talks to **FleetMaintenance** (*Reserve Broken Bike*, *Broken Bikes*), which checks that the bike is
broken (R2) and asks **Rentals** for a service reservation over Rentals' OHS. Rentals owns the hold itself: the
30-minute period (R1), one holder per bike (R3) and the expiry (R4). FleetMaintenance listens to Rentals'
`ReservationExpired` (technicians only) and lets the bike wait, still broken, for any technician. No Rentals intent
covers technician reservations, so the Rentals part is planned here; the expiry job itself comes from
`fleet-operations-releases-expired-reservations`. No context map change. Order: canvases → Rentals rules and REST →
FM scaffold additions, rules, use cases → acceptance tests.

## Placement

| Spec element | Owner | Why |
|---|---|---|
| R1 A technician can reserve a broken bike for 30 minutes | Rentals (the hold and its period); FleetMaintenance (records who holds its broken bike until when) | Rentals owns reservations and their periods (CML "Reserve and unlock"; Rentals canvas "Reservation … 30 minutes for a technician", DE-11, DE-26, DE-92) |
| R2 A technician reserves only broken bikes | FleetMaintenance (rejects first, in its language); Rentals keeps its own guard "a technician holds only withdrawn bikes" | "broken" is FM's state (canvas decision "Only a broken bike can be reserved by a technician"); Rentals canvas inbound "only withdrawn bikes (DE-16)" |
| R3 A reserved broken bike can't be reserved by another technician | Rentals | one bike, one holder is Rentals' Enforcer role (Rentals canvas "A reserved bike can't be taken by someone else"); FM translates Rentals' 409 |
| R4 An unused technician's reservation expires after 30 minutes and the bike stays broken | Rentals (expiry, bike stays withdrawn); FleetMaintenance (bike stays broken, waits for any technician) | Rentals canvas "An expired reservation releases the bike / technician variant"; FM canvas "An expired reservation leaves the bike broken" |
| Command: Reserve Broken Bike | FleetMaintenance (technician-facing), forwarded to Rentals' Reserve Bike for service | FM can say no by R2, Rentals by R3 (precondition through the ACL) |
| Command: Expire Reservations | Rentals | time-driven work on Rentals' holds; use case from `fleet-operations-releases-expired-reservations` |
| Query: Broken Bikes | FleetMaintenance | a view of FM's register (canvas inbound "Technician app / Query / Broken Bikes"); station/position comes from Rentals' OHS query |
| Event: Bike Reserved | Rentals | the hold is decided in Rentals |
| Event: Reservation Expired | Rentals | decided in Rentals; published as `rentals.api.events.ReservationExpired` (listener: Fleet Maintenance, technicians only) |

**Outcome:** several existing contexts (FleetMaintenance, Rentals).

Hand-offs (all in the CML direction `Rentals [U,OHS,PL]->[D,ACL] FleetMaintenance`):
1. FM calls Rentals' OHS *Reserve Bike for service* (`POST /rentals/bikes/{bikeId}/reservations`, `holderType: TECHNICIAN`).
2. FM calls Rentals' OHS *Bike Availability* (`GET /rentals/bikes/{bikeId}`) for the Broken Bikes view.
3. Rentals publishes `ReservationExpired`; FM's ACL listener reacts for `holderType = TECHNICIAN`.

## Context map changes

- None.

## Translation

| Spec term | Context | Context term | Where translated |
|---|---|---|---|
| Technician | Rentals | Holder of type `TECHNICIAN` | FM `rentalsctxacl` adapter |
| Technician's reservation | Rentals | Reservation (technician holder, 30 minutes) | – |
| Technician's reservation | FleetMaintenance | Technician reservation (who, until when) on the broken bike | from Rentals' reservation answer, in the `rentalsctxacl` adapter |
| Broken bike | Rentals | Withdrawn bike | FM `rentalsctxacl` adapter |
| Reservations are checked | Rentals | Release Expired Reservations (now) | Rentals' `@Scheduled` job |

## Contract mapping

| Spec element | Context | Code |
|---|---|---|
| Command: Reserve Broken Bike | FleetMaintenance | `ReserveBrokenBikeUseCase` + record `ReserveBrokenBike(bikeId, technicianId)`; `POST /fleet-maintenance/bikes/{bikeId}/reservation` `{technicianId}` (409 `NotBroken` for R2, 409 `AlreadyReserved` translated from Rentals for R3); secondary port `ServiceReservations`, adapter `infra/adapters/secondary/rentalsctxacl/RentalsServiceReservationsAdapter` |
| Command: Reserve Broken Bike (Rentals side) | Rentals | existing `ReserveBikeUseCase` + `ReserveBike` (from `rider-reserves-bike`) extended with a technician holder, no position; `POST /rentals/bikes/{bikeId}/reservations` `{holderType: "TECHNICIAN", holderId}` |
| Command: Expire Reservations | Rentals | `ReleaseExpiredReservationsUseCase` + `ReleaseExpiredReservations(now)` and its `@Scheduled` job (from `fleet-operations-releases-expired-reservations`) |
| Query: Broken Bikes | FleetMaintenance | `BrokenBikesQueryHandler` in `api/queries`, `GET /fleet-maintenance/broken-bikes`; station/position through port `BikeWhereabouts`, adapter `infra/adapters/secondary/rentalsctxacl/RentalsBikeWhereaboutsAdapter` (`GET /rentals/bikes/{bikeId}`) |
| Event: Bike Reserved | Rentals | domain event `BikeReserved` (technician holder); no integration event (no listener) |
| Event: Reservation Expired | Rentals | domain event `ReservationExpired` → existing `rentals.api.events.ReservationExpired`; FM listener `infra/adapters/primary/rentalsctxacl/ReservationExpiredListener` (`@Async @EventListener`, technicians only) → `ReleaseTechnicianReservationUseCase` + record `ReleaseTechnicianReservation(bikeId, technicianId)` |

## Canvas changes

### FleetMaintenance (`docs/bounded-contexts/velocity/fleetmaintenance/canvas.md`)

- **Business decisions, tag:** "Only a broken bike can be reserved by a technician" → `(spec: technician-reserves-broken-bike#E2.1, #E1.1)`; "An expired reservation leaves the bike broken" → `#E4.1`.
- **Business decisions, add** (to "Only a broken bike can be reserved by a technician"): *Already held:* B-17 reserved for Tom until 09:30; Tara reserves B-17 at 09:10 → rejected (Rentals' answer), B-17 stays reserved for Tom (spec: technician-reserves-broken-bike#E3.1).

### Rentals (`docs/bounded-contexts/velocity/rentals/canvas.md`)

- **Business decisions, add** (to "Only a technician can reserve a withdrawn bike"): *No position, several holds:* withdrawn B-22 with no known position; Tom reserves it at 09:00 without a position → reserved for Tom until 09:30; Tom, still holding B-17, reserves B-22 at 09:05 → reserved until 09:35 (spec: technician-reserves-broken-bike#E1.2, #E1.3, DE-26, DE-92). *Not withdrawn:* Tom reserves available B-40 → rejected (spec: technician-reserves-broken-bike#E2.1).
- **Business decisions, tag:** "An expired reservation releases the bike" technician variant → `(spec: technician-reserves-broken-bike#E4.1, #E4.2, #E4.3)`; "A reserved bike can't be taken by someone else" → `#E3.1`.

## Decisions

| # | Decision | Choice | Reason |
|---|---|---|---|
| D1 | TDD checkpoint loop | **off** | the trainer explicitly asked for a fully autonomous run (2026-11-08); the tactical skill's pushback is recorded in `docs/bounded-contexts/velocity/conversation.md`. Domain still built test-first, one example per cycle |
| D2 | Aggregate style | state-based | simplest; no requirement for an event history |
| D3 | ACLs | one per outbound query/command, in `@AntiCorruptionLayer` packages, calling REST via `RestClient` + `VelocityServiceUrls`; inbound events via `@Async @EventListener` ACL listeners | integration-contracts.md, CML `[D,ACL]` |
| D4 | Persistence | in-memory repositories with `Version` optimistic locking, after the domain is green | trainer's standing instruction |
| D5 | Reservation period | owned by Rentals only; FM stores the `expiresAt` Rentals answers, never computes 30 minutes itself | one source for DE-11 |
| D6 | Broken Bikes view | FM asks Rentals' Bike Availability per broken bike for station/position (no local copy) | few broken bikes at a time; avoids a second read model; revisit if it gets slow |
| D7 | Technician-only events | FM's listeners drop events with `holderType = RIDER` in the ACL, before any use case | integration-contracts.md "technicians only" |

## Steps

### S1: Canvas updates for FleetMaintenance and Rentals
- **Status:** todo
- **Context:** FleetMaintenance, Rentals
- **Skill:** none (apply the "Canvas changes" diff exactly)
- **Prerequisite:** `technician-reports-fault` delivered.
- **Covers:** E1.2, E1.3, E2.1, E3.1 (canvas only)
- **Done when:** both canvases carry the tags and additions above.

### S2: Rentals: technician reservations
- **Status:** todo
- **Context:** Rentals
- **Skill:** ddd-tactical-building-blocks (checkpoint loop off, D1), via bounded-context-scaffolder update mode
- **Prerequisite:** the reservation model of `rider-reserves-bike` and the withdrawal of `fleet-operations-takes-broken-bike-out-of-circulation`.
- **Covers:** R1, R2, R3, E1.1, E1.2, E1.3, E2.1, E3.1
- **Done when:** one domain test per example (30-minute hold without a position, several holds per technician, rejected on a bike that isn't withdrawn, rejected when another holder has it); `./mvnw test` green.

### S3: Rentals: technician reservations expire and the bike stays withdrawn
- **Status:** todo
- **Context:** Rentals
- **Skill:** ddd-tactical-building-blocks (checkpoint loop off, D1)
- **Prerequisite:** `ReleaseExpiredReservationsUseCase` from `fleet-operations-releases-expired-reservations`. If that plan deferred its technician examples (R2) until technician reservations exist, this step completes them; otherwise it only adds the scenarios below.
- **Covers:** R4, E4.1, E4.2, E4.3
- **Done when:** domain tests for expiry at 09:30:01 (bike stays withdrawn), no expiry at 09:29 and at exactly 09:30:00 pass; `./mvnw test` green.

### S4: Rentals: REST and published events for technicians (6b)
- **Status:** todo
- **Context:** Rentals
- **Skill:** bounded-context-scaffolder (6b)
- **Covers:** Bike Reserved, Reservation Expired
- **Done when:** `POST /rentals/bikes/{bikeId}/reservations` accepts `{holderType: "TECHNICIAN", holderId}` without a position and answers with the reservation (see CONTRACT GAP), `ReservationExpired` for technician holders published as `rentals.api.events.ReservationExpired`; REST-level test passes; `./mvnw test` green.

### S5: FleetMaintenance: ports, adapters and records (6a)
- **Status:** todo
- **Context:** FleetMaintenance
- **Skill:** bounded-context-scaffolder (6a, update mode)
- **Covers:** none
- **Done when:** records `ReserveBrokenBike`, `ReleaseTechnicianReservation`; ports `ServiceReservations`, `BikeWhereabouts` with `rentalsctxacl` adapters (409 → `AlreadyReserved`, 404 → `UnknownBike`); architecture test still green; `./mvnw test` green.

### S6: FleetMaintenance: technician reservation rules
- **Status:** todo
- **Context:** FleetMaintenance
- **Skill:** ddd-tactical-building-blocks (checkpoint loop off, D1)
- **Covers:** R1, R2, R4, E1.1, E2.1, E4.1
- **Done when:** one domain test per scenario (a broken bike records Tom's reservation until the time Rentals gave; a bike that isn't broken is rejected; an expired reservation leaves the bike broken and reservable by anyone); `./mvnw test` green.

### S7: FleetMaintenance: use cases, listener, query, REST (6b)
- **Status:** todo
- **Context:** FleetMaintenance
- **Skill:** bounded-context-scaffolder (6b)
- **Covers:** Reserve Broken Bike, Broken Bikes, Reservation Expired
- **Done when:** `ReserveBrokenBikeUseCase`, `ReleaseTechnicianReservationUseCase`, `ReservationExpiredListener` (technicians only, D7), `POST /fleet-maintenance/bikes/{bikeId}/reservation`, `GET /fleet-maintenance/broken-bikes`, wiring; `./mvnw test` green.

### S8: Acceptance tests for the spec examples
- **Status:** todo
- **Context:** FleetMaintenance, Rentals
- **Skill:** none (this skill)
- **Prerequisite:** `fleet-operations-ends-overdue-rental` delivered (E1.2 bike not returned).
- **Covers:** all examples
- **Done when:** `TechnicianReservesBrokenBikeSpecTest` (`@VelocitySpringTest`, real beans, `MutableClock`; the expiry is triggered by calling Rentals' `ReleaseExpiredReservationsUseCase` with the clock moved) has one test per example; the Rentals → FM `ReservationExpired` hand-off is asserted with Awaitility; `./mvnw test` green.

## Acceptance tests

| Example | Test |
|---|---|
| E1.1 | `TechnicianReservesBrokenBikeSpecTest#e1_1_technician_reserves_broken_bike_for_30_minutes` |
| E1.2 | `TechnicianReservesBrokenBikeSpecTest#e1_2_technician_reserves_bike_with_unknown_position` |
| E1.3 | `TechnicianReservesBrokenBikeSpecTest#e1_3_technician_holds_several_reservations` |
| E2.1 | `TechnicianReservesBrokenBikeSpecTest#e2_1_technician_cannot_reserve_bike_that_is_not_broken` |
| E3.1 | `TechnicianReservesBrokenBikeSpecTest#e3_1_second_technician_cannot_reserve_reserved_bike` |
| E4.1 | `TechnicianReservesBrokenBikeSpecTest#e4_1_unused_reservation_expires_and_bike_stays_broken` |
| E4.2 | `TechnicianReservesBrokenBikeSpecTest#e4_2_reservation_still_held_before_end` |
| E4.3 | `TechnicianReservesBrokenBikeSpecTest#e4_3_reservation_still_held_at_exact_end` |

## Traceability

| Spec ID | Step | Files |
|---|---|---|
| R1 / E1.1 | S2, S4, S6, S7, S8 | |
| R1 / E1.2, E1.3 | S1, S2, S4, S8 | |
| R2 / E2.1 | S1, S2, S6, S8 | |
| R3 / E3.1 | S1, S2, S5, S8 | |
| R4 / E4.1 | S3, S4, S6, S7, S8 | |
| R4 / E4.2, E4.3 | S3, S8 | |

## Risks and open questions

- CONTRACT GAP: the answer of `POST /rentals/bikes/{bikeId}/reservations` isn't specified; FM needs the expiry (D5). Proposed: `201 {bikeId, holderType, holderId, reservedAt, expiresAt}`.
- CONTRACT GAP: `GET /rentals/bikes/{bikeId}` lists `latitude, longitude` as always present; a bike withdrawn as *not returned* has no known position (E1.2, DE-26). Proposed: `latitude?`, `longitude?` (null when unknown).
- CONTRACT GAP: the item shape of `GET /fleet-maintenance/broken-bikes` is only "list", and `GET /fleet-maintenance/bikes/{bikeId}` lacks the reservation's end. Proposed item: `{bikeId, status, faultSource, reportedAt, stationId?, latitude?, longitude?, technicianId?, reservedUntil?, incidentIds}`; add `reservedUntil?` to the single-bike query.
- S3 depends on how `fleet-operations-releases-expired-reservations` handles its technician examples (completed there or deferred to here); the step text covers both.
- E4.1's "not offered to riders" is asserted through Rentals' Bike Availability (`availability: WITHDRAWN`).

## Review notes

- 2026-11-08: draft written (autonomous run); self-review against plan-critic.md checklist: AGREE (holds stay in Rentals, "broken" stays in FM; calls and events follow `Rentals [U,OHS,PL]->[D,ACL] FleetMaintenance`; every example covered; tactical steps ≤3 rules each; no building blocks decided).

## Changelog

### 2026-11-08: planned
- Built from spec dated 2026-11-08.
