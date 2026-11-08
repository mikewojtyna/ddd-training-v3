---
name: collections-attempts-debt-collection
intent: collections-attempts-debt-collection
spec: collections-attempts-debt-collection
system: velocity
status: approved
date: 2026-11-08
spec_date: 2026-11-08
---

# Implementation plan: Collections wants to remind riders and try to collect their debt on a schedule

<!-- Reviewers: edit this file or answer in the conversation, then set status to "approved" (or say so).
     Invoking implementation-planner again resumes from this file. -->

## Summary

The rules land in **DebtRecovery** (collection attempts, success, exhaustion). Each attempt reminds the rider through a
messaging port and asks **Settlement** to collect the whole outstanding debt over its REST resource
`POST /settlement/riders/{riderId}/debt-collections`, through a DebtRecovery ACL (Customer/Supplier, DebtRecovery
downstream and conformist to Settlement's PL). Settlement's *Outstanding Debt Cleared* is consumed by a DebtRecovery
listener. Order: Settlement's Collect Outstanding Debt resource first (upstream), then DebtRecovery's rules, use cases,
the scheduled job and the listener, then acceptance tests. Prerequisite: plan `collections-starts-debt-recovery`
(module and start rules). No context map change.

## Placement

| Spec element | Owner | Why |
|---|---|---|
| R1 When an attempt is due, remind the rider and ask for the whole debt | DebtRecovery | canvas UL "Collection attempt", "Due"; CML responsibility "Reminders"; the schedule is DebtRecovery's own clock (canvas Inbound "Collections (clock)") |
| R2 A collection that collects the debt ends recovery as succeeded | DebtRecovery | decides the recovery's state; canvas decision "A successful collection ends recovery" |
| R3 A failed attempt counts; next due 72 h later | DebtRecovery | guards failed attempts and next due time, DebtRecovery state (DE-40, DE-90) |
| R4 The 5th failed attempt exhausts the recovery | DebtRecovery | same state as R3 (DE-41); canvas decision "Recovery is exhausted after the fifth failed attempt" |
| R5 A cleared debt ends recovery as succeeded, even paused or exhausted; idempotent | DebtRecovery | DebtRecovery's reaction to Settlement's fact; canvas decision "A cleared debt ends recovery" (DE-45) |
| Command: Attempt Due Collections | DebtRecovery | issued by DebtRecovery's scheduled job; only DebtRecovery knows which recoveries are due |
| Command: Record Debt Cleared | DebtRecovery | DebtRecovery's command, issued by its ACL listener on Outstanding Debt Cleared |
| Collect Outstanding Debt (not a spec command; needed by R1) | Settlement | Settlement is "the only context that moves money" and holds the outstanding debt (CML responsibility "Outstanding debt"; Settlement canvas Inbound "Debt Recovery / Command / Collect Outstanding Debt", decisions "A collected debt is cleared…", "A failed collection leaves the debt") |
| Event: Outstanding Debt Cleared | Settlement | Settlement decides the debt reached PLN 0.00; DebtRecovery reacts. Published by plan `rider-pays-off-outstanding-debt` (and by the collection built in S2 here) |
| Event: Collection Attempted | DebtRecovery | the attempt is DebtRecovery's decision; no listener outside DebtRecovery |
| Event: Recovery Succeeded | DebtRecovery | DebtRecovery decides the recovery ended successfully |
| Event: Recovery Exhausted | DebtRecovery | DebtRecovery decides after the 5th failed attempt |
| Query: none in the spec | n/a | tests observe through the Recovery Process query built by `collections-starts-debt-recovery` |

**Outcome:** several existing contexts: DebtRecovery owns all rules; Settlement provides the Collect Outstanding Debt command (Customer/Supplier) and the Outstanding Debt Cleared event.

## Context map changes

- None. `Settlement [U,S,PL]->[D,C] DebtRecovery : DebtCollection` covers both the command (DebtRecovery calls the supplier's resource) and the event. The canvas notes "ACL? yes (Conformist to Settlement's PL)": the adapter still sits in an `@AntiCorruptionLayer` package per D3, but keeps Settlement's terms.

## Translation

| Spec term | Context | Context term | Where translated |
|---|---|---|---|
| Collection attempt | DebtRecovery | Collection attempt = payment reminder + collection request | canvas UL (same) |
| Try to collect the whole outstanding debt | DebtRecovery → Settlement | Collection (Settlement UL "charging the whole outstanding debt at once") | ACL adapter `settlementctxacl` (port `DebtCollection.collect(RiderId)` → `POST /settlement/riders/{riderId}/debt-collections`, response `{collected, collectedAmount, outstandingDebt}` → `CollectionResult`) |
| Payment reminder | DebtRecovery | Payment reminder | port `PaymentReminders`, adapter `infra/adapters/secondary/messagingacl` |
| Outstanding Debt Cleared (rider, cleared at) | DebtRecovery | debt cleared → Record Debt Cleared | ACL listener `settlementctxacl` (`OutstandingDebtCleared.riderId/occurredAt`) |
| Due | DebtRecovery | Due (next attempt due at ≤ now) | canvas UL |
| Succeeded / Exhausted | DebtRecovery | Succeeded / Exhausted | canvas UL (same) |

## Contract mapping

| Spec element | Context | Code |
|---|---|---|
| Collect Outstanding Debt (supplier side) | Settlement | `CollectOutstandingDebtUseCase` + record `CollectOutstandingDebt(riderId)` in `settlement/api/usecases`; `POST /settlement/riders/{riderId}/debt-collections` → `200 {collected, collectedAmount, outstandingDebt}`; reuses the collection behaviour built for Pay Outstanding Debt by plan `rider-pays-off-outstanding-debt` |
| Command: Attempt Due Collections | DebtRecovery | `AttemptDueCollectionsUseCase` + record `AttemptDueCollections(now)`; triggered by `infra/adapters/primary/scheduling/DueCollectionsJob` (`@Scheduled`, off when `velocity.scheduling.enabled=false`) with `Instant.now(velocityClock)`; no REST resource |
| Command: Record Debt Cleared | DebtRecovery | `RecordDebtClearedUseCase` + record `RecordDebtCleared(riderId, clearedAt)`; triggered only by the listener |
| Event: Outstanding Debt Cleared | Settlement → DebtRecovery | published language `settlement.api.events.OutstandingDebtCleared {riderId, occurredAt}` (exists); listener `debtrecovery/infra/adapters/primary/settlementctxacl/OutstandingDebtClearedListener` (`@AntiCorruptionLayer`, `@Async @EventListener`) → `RecordDebtClearedUseCase` |
| Event: Collection Attempted | DebtRecovery | domain event `CollectionAttempted(riderId, attemptNumber, amountAsked, collected, attemptedAt, nextAttemptDueAt?)`; no integration event |
| Event: Recovery Succeeded | DebtRecovery | domain event `RecoverySucceeded(riderId, succeededAt, failedAttempts)`; no integration event |
| Event: Recovery Exhausted | DebtRecovery | domain event `RecoveryExhausted(riderId, exhaustedAt, outstandingDebt)`; no integration event |
| Query: none in the spec | DebtRecovery | observation via `GET /debt-recovery/riders/{riderId}/recovery` (from `collections-starts-debt-recovery`) |

Secondary ports (DebtRecovery's language): `DebtCollection` → adapter `infra/adapters/secondary/settlementctxacl/SettlementDebtCollection` (`RestClient` + `VelocityServiceUrls`, own request/response records; a non-2xx answer becomes a failed `CollectionResult`); `PaymentReminders` → adapter `infra/adapters/secondary/messagingacl/SimulatedPaymentReminders` (records sent reminders in memory; tests read it).

## Canvas changes

### DebtRecovery (`docs/bounded-contexts/velocity/debtrecovery/canvas.md`)

Already in the canvas, tag only:
- "A successful collection ends recovery" ← E2.1 (and E1.1: the reminder is part of every attempt).
- "A failed collection schedules the next attempt in 3 days" ← E3.1.
- "Recovery is exhausted after the fifth failed attempt" ← E4.1, E4.2 ("no further attempts are made").
- "A cleared debt ends recovery" ← E5.1; its *After exhaustion* bullet ← E5.2, E5.3.

Missing, add:
- **Business decisions, add:**
  - `An attempt isn't made a minute early (spec: collections-attempts-debt-collection#E1.2)`: Given Anna's recovery is running with 1 failed attempt and its next attempt due at 10:50 on 2026-11-11, When due collections are attempted at 10:49 on 2026-11-11, Then no attempt is made for Anna (72 hours to the minute, DE-90).

### Settlement

- None. Inbound "Collect Outstanding Debt" and its two decisions are already in the canvas.

## Decisions

| # | Decision | Choice | Reason |
|---|---|---|---|
| D1 | TDD checkpoint loop | **off** | the trainer explicitly asked for a fully autonomous run (2026-11-08); the tactical skill's pushback is recorded in `docs/bounded-contexts/velocity/conversation.md`. Domain still built test-first, one example per cycle |
| D2 | Aggregate style | state-based | simplest; no requirement for an event history |
| D3 | ACLs | one per outbound query/command, in `@AntiCorruptionLayer` packages, calling REST via `RestClient` + `VelocityServiceUrls`; inbound events via `@Async @EventListener` ACL listeners | integration-contracts.md "Integration style" |
| D4 | Persistence | in-memory repositories with `Version` optimistic locking, after the domain is green | memory "aggregate persistence after domain"; repo convention |
| D5 | Who decides success after a collection | the attempt itself: Settlement's `collected=true` (or `outstandingDebt = 0`) ends the recovery as succeeded in the same use case; the `OutstandingDebtCleared` that Settlement publishes for the same collection then finds a succeeded recovery and changes nothing (R5, E5.3) | E2.1 requires Collection Attempted and Recovery Succeeded from the attempt; idempotent R5 makes the double signal harmless |
| D6 | Concurrency between attempt and clearance | the listener and the job reload and retry once on a `Version` conflict | an Outstanding Debt Cleared may arrive while the attempt for the same rider is saved |
| D7 | Domain stays pure | the domain decides "which recoveries are due" and "what an attempt's outcome does"; the use case calls `PaymentReminders` and `DebtCollection` between those two steps (load due → remind + collect per rider → record outcome → save → publish) | ports aren't called from aggregates (repo convention, BikeStation reference) |
| D8 | Acceptance-test arrangement | a shared test fixture `UnpaidRideFixture` in `src/test/java/.../velocity/debtrecovery/` creates real debt: onboards a rider through Accounts REST with a `tok_nofunds_*` card, publishes Rentals' published-language `RentalSessionEnded` on the event bus (a ride the launch tariff prices at the example's fee, e.g. 30 min ending in a drop zone = PLN 5.00), runs Settlement's retry use case with the `MutableClock` until Rental Fee Unpaid. A collection is made to succeed by the rider adding a new valid card through Accounts (DE-5) | real beans end to end; DebtRecovery's REST call to Settlement must meet a real debt |

## Steps

### S1: Tag the DebtRecovery canvas and add the 72-hour example
- **Status:** todo
- **Context:** DebtRecovery
- **Skill:** none (apply the approved canvas diff above)
- **Covers:** E1.2 (tags for E1.1, E2.1, E3.1, E4.1, E4.2, E5.1, E5.2, E5.3)
- **Done when:** canvas contains the new decision tagged with its spec ID.

### S2: Settlement offers Collect Outstanding Debt over REST
- **Status:** todo
- **Context:** Settlement
- **Skill:** bounded-context-scaffolder (update mode, 6b only: use case + REST); the collection rules themselves come from plan `rider-pays-off-outstanding-debt` (prerequisite: its Settlement domain step for paying the whole outstanding debt) and plan `finance-retries-failed-charge` (outstanding debt, Rental Fee Unpaid)
- **Covers:** Collect Outstanding Debt (no spec ID; prerequisite for R1-R4)
- **Done when:** `CollectOutstandingDebtUseCase` + `POST /settlement/riders/{riderId}/debt-collections` return `{collected, collectedAmount, outstandingDebt}`; a successful collection publishes `OutstandingDebtCleared` and unblocks the rider (Settlement canvas decision); a Settlement Spring test covers success and failure; `./mvnw test` green.

### S3: DebtRecovery ports and ACLs (6a)
- **Status:** todo
- **Context:** DebtRecovery
- **Skill:** bounded-context-scaffolder (update mode, 6a)
- **Covers:** Attempt Due Collections, Record Debt Cleared (api records), Outstanding Debt Cleared (listener package)
- **Done when:** records `AttemptDueCollections`, `RecordDebtCleared`; ports `DebtCollection`, `PaymentReminders`; `SettlementDebtCollection` (REST ACL) and `SimulatedPaymentReminders`; `DebtRecoveryArchitectureTest` still green; `./mvnw test` green.

### S4: Attempt rules, test-first (run 1 of 2)
- **Status:** todo
- **Context:** DebtRecovery
- **Skill:** ddd-tactical-building-blocks (D1: loop off; one spec example per cycle, verbatim)
- **Covers:** R1, R3, R4, E1.1, E1.2, E3.1, E4.1, E4.2, Collection Attempted, Recovery Exhausted
- **Done when:** one domain test per example; due-ness to the minute (72 h); exhausted recoveries are never due; `./mvnw test` green.

### S5: Success rules, test-first (run 2 of 2)
- **Status:** todo
- **Context:** DebtRecovery
- **Skill:** ddd-tactical-building-blocks (D1: loop off)
- **Covers:** R2, R5, E2.1, E5.1, E5.2, E5.3, Recovery Succeeded
- **Done when:** one domain test per example; a paused Given for E5.1 is built with minimal state (Pause Recovery comes with `collections-agent-pauses-debt-recovery`); a second clearance returns no events; `./mvnw test` green.

### S6: Use cases, scheduled job, listener and wiring (6b)
- **Status:** todo
- **Context:** DebtRecovery
- **Skill:** bounded-context-scaffolder (6b)
- **Covers:** Attempt Due Collections, Record Debt Cleared, Outstanding Debt Cleared
- **Done when:** `AttemptDueCollectionsUseCase` (D7), `RecordDebtClearedUseCase`, `DueCollectionsJob`, `OutstandingDebtClearedListener` (`@Async @EventListener`), retry on version conflict (D6), wiring in `infra/config/DebtRecoveryConfig`; `./mvnw test` green.

### S7: Acceptance tests
- **Status:** todo
- **Context:** DebtRecovery (with real Accounts, Payments, Pricing, Settlement beans)
- **Skill:** none (implementation-planner)
- **Covers:** all examples
- **Done when:** `src/test/java/pl/wojtyna/dddtrainingv3/velocity/debtrecovery/CollectionsAttemptsDebtCollectionSpecTest.java`, `@VelocitySpringTest`, one method per example, arranged with `UnpaidRideFixture` (D8), calling `AttemptDueCollectionsUseCase` with the `MutableClock` set to the example's time; reminders asserted on `SimulatedPaymentReminders`; the DebtRecovery → Settlement collection and the Settlement → DebtRecovery clearance hand-offs are asserted with Awaitility on `GET /debt-recovery/riders/{riderId}/recovery` and `GET /settlement/riders/{riderId}/balance`. E5.1 needs Pause Recovery (prerequisite: plan `collections-agent-pauses-debt-recovery`, its 6b step) and Pay Outstanding Debt (prerequisite: plan `rider-pays-off-outstanding-debt`); until then it is `@Disabled("needs <plan>")`. Re-enable the disabled E3.1/E3.2 methods of `CollectionsStartsDebtRecoverySpecTest`. `./mvnw test` green.

## Acceptance tests

| Example | Test |
|---|---|
| E1.1 | `CollectionsAttemptsDebtCollectionSpecTest#e1_1_due_attempt_reminds_rider_and_asks_for_whole_debt` |
| E1.2 | `CollectionsAttemptsDebtCollectionSpecTest#e1_2_no_attempt_a_minute_before_it_is_due` |
| E2.1 | `CollectionsAttemptsDebtCollectionSpecTest#e2_1_collected_debt_ends_recovery_as_succeeded` |
| E3.1 | `CollectionsAttemptsDebtCollectionSpecTest#e3_1_failed_attempt_counts_and_next_is_due_in_72_hours` |
| E4.1 | `CollectionsAttemptsDebtCollectionSpecTest#e4_1_fifth_failed_attempt_exhausts_recovery` |
| E4.2 | `CollectionsAttemptsDebtCollectionSpecTest#e4_2_exhausted_recovery_gets_no_attempt_or_reminder` |
| E5.1 | `CollectionsAttemptsDebtCollectionSpecTest#e5_1_debt_cleared_while_paused_ends_recovery_as_succeeded` |
| E5.2 | `CollectionsAttemptsDebtCollectionSpecTest#e5_2_debt_cleared_after_exhaustion_records_success` |
| E5.3 | `CollectionsAttemptsDebtCollectionSpecTest#e5_3_second_clearance_changes_nothing` |

## Traceability

| Spec ID | Step | Files |
|---|---|---|
| R1 / E1.1 | S1 (tag), S2, S4, S6, S7 | |
| R1 / E1.2 | S1, S4, S7 | |
| R2 / E2.1 | S1 (tag), S2, S5, S6, S7 | |
| R3 / E3.1 | S1 (tag), S4, S7 | |
| R4 / E4.1 | S1 (tag), S4, S7 | |
| R4 / E4.2 | S1 (tag), S4, S7 | |
| R5 / E5.1 | S1 (tag), S5, S6, S7 | |
| R5 / E5.2 | S1 (tag), S5, S6, S7 | |
| R5 / E5.3 | S1 (tag), S5, S6, S7 | |

## Risks and open questions

- **CONTRACT GAP:** integration-contracts.md has no entry for the messaging provider that delivers payment reminders (canvas Outbound "Payment Reminder → Rider notifications (messaging provider)"). Planned as port `PaymentReminders` with a simulated adapter in `infra/adapters/secondary/messagingacl` that records reminders for tests. The CML has no external party for it either; it's a technical integration, not a context relationship.
- **Settlement collection when there's nothing to collect:** if a clearance is still in flight, Settlement may answer `outstandingDebt = 0`; DebtRecovery treats that as succeeded (D5), not as a failed attempt.
- **Prerequisites in other plans:** Settlement's collection behaviour and Outstanding Debt Cleared (`rider-pays-off-outstanding-debt`), Rental Fee Unpaid (`finance-retries-failed-charge`), Accounts onboarding and Payments charging (their plans) for the fixture (D8). S2 can't start before them.
- **Shared application context:** `AttemptDueCollectionsUseCase` processes every due recovery in the shared repository, including other tests' riders; assertions are per rider, and failed attempts of other tests' riders are harmless.
- **Exhausted debt stays outstanding** and the rider stays blocked (DE-41); handing it to an agency is out of scope.
- No differences from the reference context map.

## Review notes

- 2026-11-08: draft written (autonomous run); self-review against plan-critic.md checklist: AGREE (all rules in DebtRecovery per canvas/CML; Settlement supplies the command it already lists as inbound, direction matches `Settlement [U,S,PL]->[D,C] DebtRecovery`; upstream S2 before downstream; tactical runs of 3 and 2 rules grouped by state; all 9 examples covered; ports called from the use case, not the domain).

## Changelog

### 2026-11-08: planned
- Built from spec dated 2026-11-08.
