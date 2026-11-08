---
name: collections-starts-debt-recovery
intent: collections-starts-debt-recovery
spec: collections-starts-debt-recovery
system: velocity
status: done
date: 2026-11-08
spec_date: 2026-11-08
---

# Implementation plan: Collections wants debt recovery to start as soon as a rental fee stays unpaid

<!-- Reviewers: edit this file or answer in the conversation, then set status to "approved" (or say so).
     Invoking implementation-planner again resumes from this file. -->

## Summary

The intent lands in **DebtRecovery**, a new code module for an existing context (canvas
`docs/bounded-contexts/velocity/debtrecovery/canvas.md`, CML `BoundedContext DebtRecovery`). The trigger, *Rental Fee
Unpaid*, is Settlement's published language (`settlement.api.events.RentalFeeUnpaid`), consumed downstream over the
existing `Settlement [U,S,PL]->[D,C] DebtRecovery` relationship by an `@Async @EventListener` in DebtRecovery. This is
the first DebtRecovery intent, so it scaffolds the module, then builds the start/join rules test-first, then the
listener and the Recovery Process query that the acceptance tests observe. No context map change.

## Placement

| Spec element | Owner | Why |
|---|---|---|
| R1 An unpaid rental fee starts a debt recovery, first attempt due at once | DebtRecovery | the rule decides when the active pursuit of debt begins: DebtRecovery's purpose and CML responsibility "Reminders", canvas decision "An unpaid fee starts recovery with an immediate attempt" (rule heuristic: who would the business ask? Collections) |
| R2 A further unpaid fee joins the open recovery; count not reset | DebtRecovery | guards the "one open recovery per rider" state that only DebtRecovery holds (canvas UL "Recovery process", DE-44, DE-88) |
| R3 After a recovery ended, a new unpaid fee starts a new recovery | DebtRecovery | same state (open vs ended recovery) as R2; canvas Assumptions DE-88 |
| Command: Start Debt Recovery | DebtRecovery | the context whose rules (R2) turn it into a join instead of a start ("who can say no?"); issued by DebtRecovery's own listener on Rental Fee Unpaid |
| Event: Rental Fee Unpaid | Settlement | the fact happens in Settlement, which decides a fee is unpaid after the 3rd failed charge (canvas Settlement, DE-35); DebtRecovery only reacts. Built by plan `finance-retries-failed-charge` |
| Event: Recovery Started | DebtRecovery | DebtRecovery decides a recovery starts ("who would be lying?"); no listener outside DebtRecovery (canvas Outbound) |
| Query: none in the spec | n/a | the Recovery Process query (integration-contracts) is built here only as an observation point, see D5 |

**Outcome:** one existing context (DebtRecovery, new module), consuming one upstream event over an existing relationship.

## Context map changes

- None. `Settlement [U,S,PL]->[D,C] DebtRecovery : DebtCollection` already carries Rental Fee Unpaid.

## Translation

| Spec term | Context | Context term | Where translated |
|---|---|---|---|
| Rental fee unpaid (rider, ride, unpaid amount, outstanding debt, unpaid at) | DebtRecovery | unpaid fee joining / starting a recovery process | ACL listener `settlementctxacl` (`RentalFeeUnpaid.riderId/sessionId/amount/outstandingDebt/occurredAt` → `StartDebtRecovery`) |
| Debt recovery | DebtRecovery | Recovery process | canvas UL (same meaning) |
| Open recovery (running or paused) | DebtRecovery | open recovery process (running / paused) | canvas UL; "open" added to the canvas (see Canvas changes) |
| Collection attempt due | DebtRecovery | Due / next attempt due at | canvas UL |
| Rider | DebtRecovery | Rider (`RiderId`) | REST / listener edge (`riderId` string → `RiderId`) |
| Ride | DebtRecovery | ride reference (`sessionId`), kept only for traceability | ACL listener |
| Outstanding debt (Money, PLN) | DebtRecovery | outstanding debt being pursued | ACL listener (decimal string/BigDecimal → DebtRecovery money value) |

## Contract mapping

| Spec element | Context | Code |
|---|---|---|
| Command: Start Debt Recovery | DebtRecovery | `StartDebtRecoveryUseCase` + record `StartDebtRecovery(riderId, sessionId, unpaidAmount, outstandingDebt, unpaidAt)` in `debtrecovery/api/usecases`; no REST resource (only triggered by the event) |
| Event: Rental Fee Unpaid | Settlement → DebtRecovery | published language `settlement.api.events.RentalFeeUnpaid {riderId, sessionId, amount, outstandingDebt, occurredAt}` (exists); listener `debtrecovery/infra/adapters/primary/settlementctxacl/RentalFeeUnpaidListener` (`@AntiCorruptionLayer`, `@Async @EventListener`) → `StartDebtRecoveryUseCase` |
| Event: Recovery Started | DebtRecovery | domain event `RecoveryStarted(riderId, outstandingDebt, startedAt, firstAttemptDueAt)` returned in `DomainEvents`, published by the use case through `DomainEventPublisher`; no integration event in `api/events` (no listener outside DebtRecovery) |
| Query: none in the spec (observation point) | DebtRecovery | `RecoveryProcessQuery(riderId)` + `RecoveryProcessQueryHandler` in `api/queries`; `GET /debt-recovery/riders/{riderId}/recovery` → `{riderId, status: "RUNNING"\|"PAUSED"\|"SUCCEEDED"\|"EXHAUSTED", failedAttempts, nextAttemptDueAt?, pauseReason?}`, `404` without a recovery process |

Secondary port: `RecoveryProcesses` (repository, `@SecondaryPort`), in-memory adapter in `infra/adapters/secondary/persistence`.

## Canvas changes

### DebtRecovery (`docs/bounded-contexts/velocity/debtrecovery/canvas.md`)

Already in the canvas, tag only:
- "An unpaid fee starts recovery with an immediate attempt" ← spec E1.1.
- "A second unpaid fee joins the running recovery" ← spec E2.1 (add to its Then: "its next attempt stays due when it was").

Missing, add:
- **Ubiquitous language, add:** **Open**: a recovery process that is running or paused; it has neither succeeded nor been exhausted. A rider has at most one open recovery process.
- **Business decisions, add:**
  - `An unpaid fee joins a paused recovery, which stays paused (spec: collections-starts-debt-recovery#E2.2)`: Given Anna's recovery was paused for a *dispute* with 1 failed attempt, When another rental fee of PLN 3.00 is unpaid, Then no new recovery starts; the fee joins Anna's recovery, which stays paused for *dispute* with 1 failed attempt (DE-88).
  - `After success or exhaustion a new unpaid fee starts a new recovery (spec: collections-starts-debt-recovery#E3.1, #E3.2)`: Given Anna's recovery was exhausted on 2026-11-20 with PLN 5.00 still outstanding, When her rental fee of PLN 3.00 is unpaid at 12:00 on 2026-11-25 (outstanding debt PLN 8.00), Then a new recovery starts for her whole outstanding debt of PLN 8.00, with 0 failed attempts and the first attempt due at 12:00 on 2026-11-25 (Recovery Started). The same after a succeeded recovery (E3.1).

## Decisions

| # | Decision | Choice | Reason |
|---|---|---|---|
| D1 | TDD checkpoint loop | **off** | the trainer explicitly asked for a fully autonomous run (2026-11-08); the tactical skill's pushback is recorded in `docs/bounded-contexts/velocity/conversation.md`. Domain still built test-first, one example per cycle |
| D2 | Aggregate style | state-based | simplest; no requirement for an event history |
| D3 | ACLs | one per outbound query/command, in `@AntiCorruptionLayer` packages, calling REST via `RestClient` + `VelocityServiceUrls`; inbound events via `@Async @EventListener` ACL listeners | integration-contracts.md "Integration style" |
| D4 | Persistence | in-memory repositories with `Version` optimistic locking, after the domain is green | memory "aggregate persistence after domain"; repo convention |
| D5 | Observation point for acceptance tests | build `GET /debt-recovery/riders/{riderId}/recovery` (Recovery Process query from integration-contracts.md) in this plan, although the spec has no query | the Then of every example is DebtRecovery state; the pause plan later adds `pauseReason`/`outstandingDebt` to the same query |
| D6 | Acceptance-test trigger | the tests publish Settlement's published-language record `RentalFeeUnpaid` on the real Spring event bus (`ApplicationEventPublisher`), exactly as Settlement would, and wait with Awaitility | DebtRecovery's inbound boundary is that event; the Settlement chain that produces it is tested by `finance-retries-failed-charge`. Givens with an ended recovery (E3.1, E3.2) need real collection attempts against Settlement, so they reuse the `UnpaidRideFixture` (real Accounts → Payments → Settlement chain) introduced by plan `collections-attempts-debt-collection` (see S6) |
| D7 | Candidates for the tactical skill (not decisions) | a recovery-process aggregate per rider holding state, failed attempts, next due time and outstanding debt; "one open recovery per rider" enforced at the use case/repository level (find open by rider) | listed only as input; the tactical skill decides |

## Steps

### S1: Update the DebtRecovery canvas
- **Status:** done
- **Context:** DebtRecovery
- **Skill:** none (apply the approved canvas diff above)
- **Covers:** E2.2, E3.1, E3.2 (tags for E1.1, E2.1)
- **Done when:** canvas contains the "Open" term and the two new business decisions, tagged with spec IDs.

### S2: Scaffold the DebtRecovery module (6a)
- **Status:** done
- **Context:** DebtRecovery
- **Skill:** bounded-context-scaffolder (6a), told that this plan settles the structural decisions and D1
- **Covers:** Start Debt Recovery (api record), Rental Fee Unpaid (listener stub)
- **Done when:** `debtrecovery/package-info.java` with `@BoundedContext(name = "DebtRecovery")` + `@Module`; `api/usecases/StartDebtRecovery` record; `api/queries/RecoveryProcessQuery`; empty `settlementctxacl` listener package marked `@AntiCorruptionLayer`; `DebtRecoveryArchitectureTest` (only other contexts' `api` used) green; `./mvnw test` green.

### S3: Start and join rules, test-first
- **Status:** done
- **Context:** DebtRecovery
- **Skill:** ddd-tactical-building-blocks (D1: loop off; one spec example per red-green cycle, examples used verbatim as scenarios)
- **Covers:** R1, R2, R3, E1.1, E2.1, E2.2, E3.1, E3.2, Recovery Started
- **Done when:** one domain test per example (names after the IDs), `RecoveryStarted` returned in `DomainEvents`; Givens that need a paused/exhausted/succeeded recovery are built by the tactical skill with the minimal state it needs (the commands that reach them belong to later plans); `./mvnw test` green.

### S4: Use case, listener, query, REST and wiring (6b)
- **Status:** done
- **Context:** DebtRecovery
- **Skill:** bounded-context-scaffolder (6b)
- **Covers:** Start Debt Recovery, Rental Fee Unpaid, Recovery Started
- **Done when:** `StartDebtRecoveryUseCase` (load open recovery by rider or create, execute, save, publish via `DomainEventPublisher`); `RentalFeeUnpaidListener` (`@Async @EventListener`) translating the event; `RecoveryProcessQueryHandler` + `GET /debt-recovery/riders/{riderId}/recovery`; `infra/config/DebtRecoveryConfig` wiring with the `velocityClock`; `./mvnw test` green.

### S5: Persistence with optimistic locking
- **Status:** done
- **Context:** DebtRecovery
- **Skill:** ddd-tactical-building-blocks (repository + `Version`)
- **Covers:** none (D4)
- **Done when:** in-memory `RecoveryProcesses` adapter rejects stale versions; the listener retries once on a version conflict (two unpaid fees for one rider arriving together); `./mvnw test` green.

### S6: Acceptance tests
- **Status:** done
- **Context:** DebtRecovery
- **Skill:** none (implementation-planner)
- **Covers:** all examples
- **Done when:** `src/test/java/pl/wojtyna/dddtrainingv3/velocity/debtrecovery/CollectionsStartsDebtRecoverySpecTest.java`, `@VelocitySpringTest`, one method per example; the Settlement → DebtRecovery hand-off is asserted with Awaitility on `GET /debt-recovery/riders/{riderId}/recovery`. E1.1, E2.1 run as soon as S4 is done. E2.2, E3.1, E3.2 need a paused / succeeded / exhausted recovery: prerequisites are plan `collections-attempts-debt-collection` (its acceptance step: attempts, success, exhaustion) and plan `collections-agent-pauses-debt-recovery` (its 6b step: Pause Recovery REST); until those are delivered these three methods are written and `@Disabled("needs <plan>")`, and the last of those plans re-enables them. `./mvnw test` green.

## Acceptance tests

| Example | Test |
|---|---|
| E1.1 | `CollectionsStartsDebtRecoverySpecTest#e1_1_unpaid_fee_starts_recovery_with_first_attempt_due_at_once` |
| E2.1 | `CollectionsStartsDebtRecoverySpecTest#e2_1_further_unpaid_fee_joins_running_recovery_keeping_count_and_due_time` |
| E2.2 | `CollectionsStartsDebtRecoverySpecTest#e2_2_unpaid_fee_joins_paused_recovery_which_stays_paused` |
| E3.1 | `CollectionsStartsDebtRecoverySpecTest#e3_1_unpaid_fee_after_succeeded_recovery_starts_new_recovery` |
| E3.2 | `CollectionsStartsDebtRecoverySpecTest#e3_2_unpaid_fee_after_exhausted_recovery_starts_new_recovery_for_whole_debt` |

## Traceability

| Spec ID | Step | Files |
|---|---|---|
| R1 / E1.1 | S1 (tag), S3, S4, S6 | `domain/recovery/RecoveryProcess.java`, `RecoveryProcessTest`; `api/usecases/StartDebtRecoveryUseCase`; `infra/adapters/primary/settlementctxacl/RentalFeeUnpaidListener`; `CollectionsStartsDebtRecoverySpecTest` |
| R2 / E2.1 | S1 (tag), S3, S4, S6 | `domain/recovery/RecoveryProcess.java`, `RecoveryProcessTest`; `api/usecases/StartDebtRecoveryUseCase`; `infra/adapters/primary/settlementctxacl/RentalFeeUnpaidListener`; `CollectionsStartsDebtRecoverySpecTest` |
| R2 / E2.2 | S1, S3, S6 | `domain/recovery/RecoveryProcess.java`, `RecoveryProcessTest`; `api/usecases/StartDebtRecoveryUseCase`; `infra/adapters/primary/settlementctxacl/RentalFeeUnpaidListener`; `CollectionsStartsDebtRecoverySpecTest` |
| R3 / E3.1 | S1, S3, S6 | `domain/recovery/RecoveryProcess.java`, `RecoveryProcessTest`; `api/usecases/StartDebtRecoveryUseCase`; `infra/adapters/primary/settlementctxacl/RentalFeeUnpaidListener`; `CollectionsStartsDebtRecoverySpecTest` |
| R3 / E3.2 | S1, S3, S6 | `domain/recovery/RecoveryProcess.java`, `RecoveryProcessTest`; `api/usecases/StartDebtRecoveryUseCase`; `infra/adapters/primary/settlementctxacl/RentalFeeUnpaidListener`; `CollectionsStartsDebtRecoverySpecTest` |

## Risks and open questions

- **Prerequisite upstream:** Settlement must publish `RentalFeeUnpaid` (plan `finance-retries-failed-charge`). The record already exists in `settlement/api/events`, so DebtRecovery can be built and tested before Settlement is.
- **Shared application context in tests:** every `@VelocitySpringTest` class shares one context and one `MutableClock`; tests use their own rider IDs and never assume an empty repository.
- **Duplicate Rental Fee Unpaid for the same ride:** Settlement settles a ride at most once (DE-95), so a duplicate isn't expected; if one arrives, the outstanding debt in the event is absolute, so joining twice doesn't inflate the pursued debt. Recorded, not specified.
- **Spec test data:** E3.1's fee of PLN 7.00 can't be produced by the launch tariff (DE-30: fees are combinations of 0/3/8/15/… and 0/2/40). Harmless here because the test publishes `RentalFeeUnpaid` directly (D6), but worth fixing in the spec if the example is ever driven through Settlement.
- No differences from the reference context map.

## Review notes

- 2026-11-08: draft written (autonomous run); self-review against plan-critic.md checklist: AGREE (placement follows the canvas and CML; integration follows `Settlement [U,S,PL]->[D,C] DebtRecovery`; all 5 examples covered; at most 3 rules in S3, all guarding the same open-recovery state; S6 dependencies on later plans made explicit).

- 2026-11-08 (DebtRecovery wave, autonomous run): built. Decisions taken: one `RecoveryProcess` aggregate per rider (identity `RiderId`), restarted by a new unpaid fee after success/exhaustion; version conflicts reload and retry up to 3 times (`RecoveryProcessChange`). D6 refined: every example uses REAL Settlement debt (`UnpaidRideFixture`: rider with a `tok_nofunds_*` card, Rentals' `RentalSessionEnded` published by the test, Settlement's retries driven with the `MutableClock`), except E3.1's PLN 7.00 Rental Fee Unpaid, which the launch tariff can't produce and is published as Settlement would. No `@Disabled` tests were needed (all four DebtRecovery plans were built together). The fixture registers riders in Accounts and verifies cards at Payments directly (as `SettlementDriver` does) instead of full onboarding, which Settlement doesn't need.

## Changelog

### 2026-11-08: planned
- Built from spec dated 2026-11-08.

### 2026-11-08: delivered (DebtRecovery wave)
- S1-S6 done; acceptance tests `CollectionsStartsDebtRecoverySpecTest` (5), domain tests in `RecoveryProcessTest`; per-context plan `docs/bounded-contexts/velocity/debtrecovery/plan.md`.
