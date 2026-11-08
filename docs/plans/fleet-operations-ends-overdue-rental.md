---
name: fleet-operations-ends-overdue-rental
intent: fleet-operations-ends-overdue-rental
spec: fleet-operations-ends-overdue-rental
system: velocity
status: approved
date: 2026-11-08
spec_date: 2026-11-08
---

# Implementation plan: Fleet operations wants a ride that runs past the maximum rental time ended

<!-- Reviewers: edit this file or answer in the conversation, then set status to "approved" (or say so).
     Invoking implementation-planner again resumes from this file. -->

## Summary

The intent lands in **Rentals** (CML responsibility "Max rental time end"; canvas inbound "Fleet operations (clock) |
Command | End Overdue Sessions"). Like reservation expiry, it is a use case taking "now", triggered by a `@Scheduled` job
(off in tests). Each overdue session ends at the 12-hour mark with no place and its bike is withdrawn as *not
returned*; `RentalSessionEnded` (Settlement prices the penalty) and `BikeWithdrawn` (Fleet Maintenance) are published.

## Placement

| Spec element | Owner | Why |
|---|---|---|
| R1 A ride running longer than 12 hours is ended automatically, at the 12-hour mark | Rentals | Rentals makes sure every session ends (canvas Purpose; CML "Max rental time end"; canvas decision "A session longer than the maximum rental time is ended automatically", DE-20, DE-82) |
| R2 The bike of an automatically ended ride is withdrawn as not returned | Rentals | Rentals withdraws bikes on faults it detects, *not returned* among them (canvas UL "Withdrawn"; DE-21) |
| R3 A ride that ended in time is left alone | Rentals | only running sessions are Rentals' to end (same state as R1) |
| Command: End Overdue Rental Sessions | Rentals | no rejection rules; Rentals owns the sessions (canvas inbound "End Overdue Sessions") |
| Event: Rental Session Ended | Rentals | the session ends in Rentals; Settlement listens (CML `Rentals -> Settlement`); the penalty is Pricing's/Settlement's business (DE-33) |
| Event: Bike Withdrawn | Rentals | Rentals decides the withdrawal; Fleet Maintenance listens and treats the bike as broken (DE-21) |

**Outcome:** one existing context (Rentals); two published events over existing relationships.

## Context map changes

- None.

## Translation

| Spec term | Context | Context term | Where translated |
|---|---|---|---|
| Fleet operations | Rentals | (actor) the scheduled job | `infra/adapters/primary` `@Scheduled` job |
| The rides are checked at T | Rentals | End Overdue Rental Sessions with now = T | job reads `velocityClock`; tests call the use case with `MutableClock` set |
| Ride | Rentals | rental session | canvas UL |
| Maximum rental time exceeded | Rentals | end reason *maximum rental time exceeded* | published as `MAX_RENTAL_TIME_EXCEEDED`, `stationId`/position empty |
| Not returned | Rentals | withdrawal reason *not returned* | published as `NOT_RETURNED` |

## Contract mapping

| Spec element | Context | Code |
|---|---|---|
| Command: End Overdue Rental Sessions | Rentals | `EndOverdueRentalSessionsUseCase` + record `EndOverdueRentalSessions(Instant now)` in `rentals/api/usecases`; triggered by `infra/adapters/primary/OverdueSessionsJob` (`@Scheduled`, inactive when `velocity.scheduling.enabled=false`); no REST endpoint |
| Event: Rental Session Ended | Rentals | domain event `RentalSessionEnded` (ended at = started at + 12 h, no place); integration event `rentals.api.events.RentalSessionEnded {…, endReason: "MAX_RENTAL_TIME_EXCEEDED", stationId: null, latitude: null, longitude: null}` |
| Event: Bike Withdrawn | Rentals | domain event `BikeWithdrawn` (reason *not returned*, withdrawn at = the check's now); integration event `rentals.api.events.BikeWithdrawn {bikeId, reason: "NOT_RETURNED", withdrawnAt}` |

## Canvas changes

### Rentals (`docs/bounded-contexts/velocity/rentals/canvas.md`)

- **Business decisions, add:**
  - `### An overdue session noticed late still ends at the 12-hour mark (spec: fleet-operations-ends-overdue-rental#E1.3)`: **Given** Anna's session on B-17 started at 08:00 and runs **When** sessions are checked only at 22:15 **Then** the session ended at 20:00 with reason *maximum rental time exceeded* (DE-82; until now only an Assumption).
  - `### A session that ended in time is left alone (spec: fleet-operations-ends-overdue-rental#E3.1)`: **Given** Anna's session on B-17 started at 08:00 and she returned B-17 at Old Town at 19:59 **When** sessions are checked at 20:00:01 **Then** nothing happens: the session stays ended at Old Town at 19:59 and B-17 stays available.
- **Tag existing decisions:** "A session longer than the maximum rental time is ended automatically" → `#E1.1`, `#E1.2` (boundary), `#E2.1` (all `spec: fleet-operations-ends-overdue-rental`).

## Decisions

| # | Decision | Choice | Reason |
|---|---|---|---|
| D1 | TDD checkpoint loop | **off** | the trainer explicitly asked for a fully autonomous run (2026-11-08); the tactical skill's pushback is recorded in `docs/bounded-contexts/velocity/conversation.md`. Domain still built test-first, one example per cycle. |
| D2 | Aggregate style | state-based | simplest; no requirement for an event history |
| D3 | ACLs | one per outbound query/command, in `@AntiCorruptionLayer` packages, calling REST via `RestClient` + `VelocityServiceUrls`; inbound events via `@Async @EventListener` ACL listeners | trainer's integration style; this plan has no outbound call |
| D4 | Persistence | in-memory repositories with `Version` optimistic locking, after the domain is green; the repository offers a lookup of sessions started before a given instant | user memory; trainer's brief |
| D5 | Delivery order | seventh of the eight Rentals plans (`fleet-manager-adds-bike-to-fleet` D7) | E3.1 needs station returns (`rider-returns-bike-at-station`) |
| D6 | One failure doesn't stop the run | each overdue session ends in its own load-execute-save; an optimistic-locking conflict (a return at the same moment) skips that bike for this run | a return that wins the race is a ride that ended in time (R3) |
| D7 | No lock command | the automatic end doesn't touch `BikeLocks` | nobody knows where the bike is (DE-21) |

## Steps

### S1: Update the Rentals canvas
- **Status:** todo
- **Context:** Rentals
- **Skill:** none (this skill applies the canvas diff above)
- **Covers:** E1.3, E3.1 (canvas decisions), tags for E1.1, E1.2, E2.1
- **Done when:** the canvas contains the diff from "Canvas changes" exactly.

### S2: Scaffold the command record (6a)
- **Status:** todo
- **Context:** Rentals
- **Skill:** bounded-context-scaffolder (update mode, Step 6a)
- **Covers:** Command: End Overdue Rental Sessions (record)
- **Done when:** `api/usecases/EndOverdueRentalSessions` exists; `RentalsArchitectureTest` green; `./mvnw test` green.

### S3: Domain: overdue sessions end at the 12-hour mark and withdraw the bike
- **Status:** todo
- **Context:** Rentals
- **Skill:** ddd-tactical-building-blocks (checkpoint loop off, D1)
- **Covers:** R1, R2, R3, E1.1, E1.2, E1.3, E2.1, E3.1
- **Done when:** one unit test per example, written first, passes (20:00:00 still running, 20:00:01 ended at 20:00; a late check at 22:15 still ends at 20:00; the bike is withdrawn as *not returned*; an ended session is untouched); `./mvnw test` green.

### S4: Use case, scheduled job, event publishing and wiring (6b)
- **Status:** todo
- **Context:** Rentals
- **Skill:** bounded-context-scaffolder (Step 6b)
- **Covers:** Command: End Overdue Rental Sessions, Event: Rental Session Ended, Event: Bike Withdrawn
- **Done when:** `EndOverdueRentalSessionsUseCase` (find sessions started more than 12 h before now, end each, save, publish, map integration events), `OverdueSessionsJob` (`@Scheduled`, using `velocityClock`), wiring; `./mvnw test` green.

### S5: Acceptance tests for every example
- **Status:** todo
- **Context:** Rentals (Accounts over REST for eligible riders)
- **Skill:** none (this skill)
- **Covers:** all examples
- **Done when:** `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/FleetOperationsEndsOverdueRentalSpecTest.java` is a `@VelocitySpringTest` with real beans; sessions started through REST at 08:00 (`MutableClock`), "the rides are checked at T" sets the clock to T and calls the `EndOverdueRentalSessionsUseCase` bean; asserts `GET /rentals/bikes/{bikeId}`, `GET /rentals/riders/{riderId}/active-rental` and the published `RentalSessionEnded` / `BikeWithdrawn` integration events with `@RecordApplicationEvents` and Awaitility; one test method per example; `./mvnw test` green.

## Acceptance tests

| Example | Test |
|---|---|
| E1.1 | `FleetOperationsEndsOverdueRentalSpecTest#e1_1_session_over_12_hours_ends_at_12_hour_mark` |
| E1.2 | `FleetOperationsEndsOverdueRentalSpecTest#e1_2_session_of_exactly_12_hours_still_runs` |
| E1.3 | `FleetOperationsEndsOverdueRentalSpecTest#e1_3_late_check_still_ends_session_at_12_hour_mark` |
| E2.1 | `FleetOperationsEndsOverdueRentalSpecTest#e2_1_bike_of_overdue_session_is_withdrawn_as_not_returned` |
| E3.1 | `FleetOperationsEndsOverdueRentalSpecTest#e3_1_session_returned_in_time_is_left_alone` |

## Traceability

| Spec ID | Step | Files |
|---|---|---|
| R1 / E1.1 | S3, S4, S5 | |
| R1 / E1.2 | S3, S5 | |
| R1 / E1.3 | S1, S3, S5 | |
| R2 / E2.1 | S3, S4, S5 | |
| R3 / E3.1 | S1, S3, S5 | |

## Risks and open questions

- **Q2 (open, accepted):** warn the rider before the limit? Not planned; doesn't change R1-R3.
- Ending a session at 20:00 while the check runs at 22:15 means `endedAt` < the event's publication time; Settlement must price on `endedAt` (DE-82), which its plan should assert.
- In the shared Spring context, a check at a late clock time ends other tests' running sessions too; tests assert only on their own bikes and set `MutableClock` explicitly (sessions must start on a fixed date so 12 hours can be crossed without disturbing other test classes more than necessary).

## Review notes

- 2026-11-08: draft written (autonomous run); self-review against plan-critic.md checklist: AGREE. Single owner; the penalty stays out (Pricing/Settlement); three rules in one tactical run guarding the running session; E3.1 relies on the station return delivered earlier (D5).

## Changelog

### 2026-11-08: planned
- Built from spec dated 2026-11-08.
