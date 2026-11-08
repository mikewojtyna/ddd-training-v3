---
name: collections-agent-pauses-debt-recovery
intent: collections-agent-pauses-debt-recovery
spec: collections-agent-pauses-debt-recovery
system: velocity
status: approved
date: 2026-11-08
spec_date: 2026-11-08
---

# Implementation plan: A collections agent wants to pause a rider's debt recovery

<!-- Reviewers: edit this file or answer in the conversation, then set status to "approved" (or say so).
     Invoking implementation-planner again resumes from this file. -->

## Summary

The intent lands entirely in **DebtRecovery** (CML responsibility "Pausing and resuming recovery"). It adds the
Pause Recovery command with its REST resource `POST /debt-recovery/riders/{riderId}/recovery/pause`, the rule that a
paused recovery is never due, and extends the Recovery Process query with the pause reason and the outstanding debt.
Prerequisites: plans `collections-starts-debt-recovery` (module, Recovery Process query) and
`collections-attempts-debt-collection` (Attempt Due Collections, needed by R3). No integration, no context map change.

## Placement

| Spec element | Owner | Why |
|---|---|---|
| R1 A collections agent can pause a running recovery, keeping its failed attempts | DebtRecovery | CML responsibility "Pausing and resuming recovery"; canvas Inbound "Collections agent / Pause Recovery (reason)"; DE-42 |
| R2 Pause only for dispute, payment plan or regulatory hold | DebtRecovery | canvas UL "Paused: reason dispute, payment plan or regulatory hold"; DE-89 closed list |
| R3 A paused recovery makes no collection attempts | DebtRecovery | guards the same recovery state as Attempt Due Collections; canvas decision "A paused recovery makes no attempts" |
| R4 Only a running recovery can be paused | DebtRecovery | guards the recovery's state (DE-89) |
| Command: Pause Recovery | DebtRecovery | DebtRecovery owns R2 and R4, the rules that reject it |
| Query: Rider's Debt Recovery | DebtRecovery | a view of DebtRecovery's own recovery process (canvas Inbound "Collections agent / Query / Recovery Process"); the outstanding debt in it is DebtRecovery's local copy fed by Rental Fee Unpaid and collection results, no call to Settlement |
| Event: Recovery Paused | DebtRecovery | DebtRecovery decides the pause; no listener outside DebtRecovery |

**Outcome:** one existing context.

## Context map changes

- None.

## Translation

| Spec term | Context | Context term | Where translated |
|---|---|---|---|
| Debt recovery | DebtRecovery | Recovery process | canvas UL |
| Pause reason *dispute* / *payment plan* / *regulatory hold* | DebtRecovery | pause reason `DISPUTE` / `PAYMENT_PLAN` / `REGULATORY_HOLD` | REST adapter (body `reason` string → pause reason) |
| Collections agent (paused by) | DebtRecovery | Collections agent (`AgentId`) | REST adapter (`agentId` → `AgentId`) |
| Rider's Debt Recovery (query) | DebtRecovery | Recovery Process (query) | integration-contracts.md name; same data |
| State running / paused / succeeded / exhausted | DebtRecovery | `RUNNING` / `PAUSED` / `SUCCEEDED` / `EXHAUSTED` | query response |

## Contract mapping

| Spec element | Context | Code |
|---|---|---|
| Command: Pause Recovery | DebtRecovery | `PauseRecoveryUseCase` + record `PauseRecovery(riderId, reason, agentId, now)` in `api/usecases`; `POST /debt-recovery/riders/{riderId}/recovery/pause` with `{agentId, reason: "DISPUTE"\|"PAYMENT_PLAN"\|"REGULATORY_HOLD"}`; `409 {"error": "RecoveryNotRunning"}` for R4 (including no recovery process, E4.2), `400` for a reason outside the list (R2, D5) |
| Query: Rider's Debt Recovery | DebtRecovery | `RecoveryProcessQuery` + `RecoveryProcessQueryHandler` (built by `collections-starts-debt-recovery`), response extended to `{riderId, status, failedAttempts, nextAttemptDueAt?, pauseReason?, outstandingDebt}`; `GET /debt-recovery/riders/{riderId}/recovery`, `404` without a recovery process |
| Event: Recovery Paused | DebtRecovery | domain event `RecoveryPaused(riderId, reason, pausedBy, pausedAt, failedAttempts)`; no integration event (no listener outside DebtRecovery) |

## Canvas changes

### DebtRecovery (`docs/bounded-contexts/velocity/debtrecovery/canvas.md`)

Already in the canvas, tag only:
- "A paused recovery makes no attempts" ← E3.1.

Missing, add:
- **Business decisions, add:**
  - `Pausing keeps the failed attempts (spec: collections-agent-pauses-debt-recovery#E1.1, #E2.1)`: Given Anna's recovery is running with 1 failed attempt and its next attempt due at 10:50 on 2026-11-11, When collections agent Kate pauses it for a *dispute* at 14:00 on 2026-11-09, Then Anna's recovery is paused for *dispute* with 1 failed attempt (Recovery Paused).
  - `Only the three pause reasons are accepted (spec: collections-agent-pauses-debt-recovery#E2.2)`: Given Ben's recovery is running, When Kate pauses it giving the reason "rider is on holiday", Then the pause is rejected and Ben's recovery keeps running.
  - `Only a running recovery can be paused (spec: collections-agent-pauses-debt-recovery#E4.1, #E4.2, #E4.3)`: Given Anna's recovery is paused for a *dispute*, When Kate pauses it for a *regulatory hold*, Then the pause is rejected and the reason stays *dispute* (DE-89). The same for an exhausted recovery (E4.1) and a rider without one (E4.2).

## Decisions

| # | Decision | Choice | Reason |
|---|---|---|---|
| D1 | TDD checkpoint loop | **off** | the trainer explicitly asked for a fully autonomous run (2026-11-08); the tactical skill's pushback is recorded in `docs/bounded-contexts/velocity/conversation.md`. Domain still built test-first, one example per cycle |
| D2 | Aggregate style | state-based | simplest; no requirement for an event history |
| D3 | ACLs | one per outbound query/command, in `@AntiCorruptionLayer` packages, calling REST via `RestClient` + `VelocityServiceUrls`; inbound events via `@Async @EventListener` ACL listeners | integration-contracts.md "Integration style" (no ACL needed by this intent) |
| D4 | Persistence | in-memory repositories with `Version` optimistic locking, after the domain is green | memory "aggregate persistence after domain"; repo convention |
| D5 | Where R2 is enforced | the pause reason is a closed type in the domain, so an unknown reason can't reach it; the REST adapter rejects it with `400 Bad Request` `{"error": "UnknownPauseReason"}` | integration-contracts.md: 400 for malformed input; a closed list makes the wrong value unrepresentable. E2.2 is therefore an acceptance test only; E2.1 is the domain test for R2 |
| D6 | Pausing a rider without a recovery (E4.2) | rejected by the use case when no open recovery process exists for the rider, answered as `409 RecoveryNotRunning` | same rule (R4) as the domain's state check; one error name for the agent |

## Steps

### S1: Update the DebtRecovery canvas
- **Status:** todo
- **Context:** DebtRecovery
- **Skill:** none (apply the approved canvas diff above)
- **Covers:** E1.1, E2.1, E2.2, E4.1, E4.2, E4.3 (tag for E3.1)
- **Done when:** canvas contains the three new decisions, tagged.

### S2: Pause Recovery api record (6a)
- **Status:** todo
- **Context:** DebtRecovery
- **Skill:** bounded-context-scaffolder (update mode, 6a)
- **Covers:** Pause Recovery
- **Done when:** record `PauseRecovery` in `api/usecases`; architecture test green; `./mvnw test` green.

### S3: Pause rules, test-first
- **Status:** todo
- **Context:** DebtRecovery
- **Skill:** ddd-tactical-building-blocks (D1: loop off; one spec example per cycle, verbatim)
- **Covers:** R1, R2, R4, E1.1, E2.1, E4.1, E4.3, Recovery Paused
- **Done when:** one domain test per example; `RecoveryPaused` returned in `DomainEvents`; `./mvnw test` green.

### S4: A paused recovery is never due, test-first
- **Status:** todo
- **Context:** DebtRecovery
- **Skill:** ddd-tactical-building-blocks (D1: loop off)
- **Covers:** R3, E3.1
- **Done when:** domain test for E3.1 (no attempt, no reminder, count kept); the minimal paused state used by `collections-attempts-debt-collection` S5 and `collections-starts-debt-recovery` S3 is replaced by the real pause; `./mvnw test` green.

### S5: Use case, REST, extended query (6b)
- **Status:** todo
- **Context:** DebtRecovery
- **Skill:** bounded-context-scaffolder (6b)
- **Covers:** Pause Recovery, Rider's Debt Recovery, E4.2, E2.2
- **Done when:** `PauseRecoveryUseCase` (D6), `POST /debt-recovery/riders/{riderId}/recovery/pause` with 400/409 mapping (D5), query response with `pauseReason` and `outstandingDebt`; `./mvnw test` green.

### S6: Acceptance tests
- **Status:** todo
- **Context:** DebtRecovery
- **Skill:** none (implementation-planner)
- **Covers:** all examples
- **Done when:** `src/test/java/pl/wojtyna/dddtrainingv3/velocity/debtrecovery/CollectionsAgentPausesDebtRecoverySpecTest.java`, `@VelocitySpringTest`, one method per example, arranged with `UnpaidRideFixture` and `AttemptDueCollectionsUseCase` from plan `collections-attempts-debt-collection` (prerequisite: its S7) to reach the failed-attempt counts; assertions over REST (`GET /debt-recovery/riders/{riderId}/recovery`) and `SimulatedPaymentReminders` (E3.1). Re-enable the `@Disabled` E2.2 of `CollectionsStartsDebtRecoverySpecTest` and E5.1 of `CollectionsAttemptsDebtCollectionSpecTest` (once `rider-pays-off-outstanding-debt` is delivered). `./mvnw test` green.

## Acceptance tests

| Example | Test |
|---|---|
| E1.1 | `CollectionsAgentPausesDebtRecoverySpecTest#e1_1_agent_pauses_running_recovery_for_dispute_keeping_failed_attempts` |
| E2.1 | `CollectionsAgentPausesDebtRecoverySpecTest#e2_1_agent_pauses_recovery_for_payment_plan` |
| E2.2 | `CollectionsAgentPausesDebtRecoverySpecTest#e2_2_pause_for_reason_outside_the_list_is_rejected` |
| E3.1 | `CollectionsAgentPausesDebtRecoverySpecTest#e3_1_paused_recovery_gets_no_attempt_or_reminder` |
| E4.1 | `CollectionsAgentPausesDebtRecoverySpecTest#e4_1_exhausted_recovery_cannot_be_paused` |
| E4.2 | `CollectionsAgentPausesDebtRecoverySpecTest#e4_2_rider_without_recovery_cannot_be_paused` |
| E4.3 | `CollectionsAgentPausesDebtRecoverySpecTest#e4_3_paused_recovery_cannot_be_paused_again` |

## Traceability

| Spec ID | Step | Files |
|---|---|---|
| R1 / E1.1 | S1, S3, S5, S6 | |
| R2 / E2.1 | S1, S3, S5, S6 | |
| R2 / E2.2 | S1, S5 (REST 400), S6 | |
| R3 / E3.1 | S1 (tag), S4, S6 | |
| R4 / E4.1 | S1, S3, S6 | |
| R4 / E4.2 | S1, S5 (use case), S6 | |
| R4 / E4.3 | S1, S3, S6 | |

## Risks and open questions

- **CONTRACT GAP:** `POST /debt-recovery/riders/{riderId}/recovery/pause` carries only `{reason}`; the spec's Recovery Paused needs *paused by*. Planned as `{agentId, reason}`.
- **CONTRACT GAP:** the Recovery Process query result lacks the spec's *outstanding debt* (Rider's Debt Recovery). Planned as an extra field `outstandingDebt` (decimal string, PLN); also the `status` values aren't listed in integration-contracts.md: planned as `RUNNING|PAUSED|SUCCEEDED|EXHAUSTED`.
- **The outstanding debt shown is DebtRecovery's local copy** (last `RentalFeeUnpaid.outstandingDebt` or collection result); a payment in the app between events may make it stale until Outstanding Debt Cleared arrives. Acceptable for a collections agent's view.
- **Billing disputes from Customer Support don't pause recovery automatically** (reference map: deferred; spec Out of scope).
- **No authorisation:** any collections agent may pause (DE-89); the agent ID is taken from the request body, not authenticated.
- No differences from the reference context map.

## Review notes

- 2026-11-08: draft written (autonomous run); self-review against plan-critic.md checklist: AGREE (all elements in DebtRecovery per CML responsibility "Pausing and resuming recovery"; R2 as a closed type with E2.2 tested at the REST edge, recorded in D5; R3 split into its own tactical step since it touches the attempt logic; all 7 examples covered; prerequisites on earlier plans explicit).

## Changelog

### 2026-11-08: planned
- Built from spec dated 2026-11-08.
