---
name: collections-agent-resumes-debt-recovery
intent: collections-agent-resumes-debt-recovery
spec: collections-agent-resumes-debt-recovery
system: velocity
status: done
date: 2026-11-08
spec_date: 2026-11-08
---

# Implementation plan: A collections agent wants to resume a paused debt recovery

<!-- Reviewers: edit this file or answer in the conversation, then set status to "approved" (or say so).
     Invoking implementation-planner again resumes from this file. -->

## Summary

The intent lands entirely in **DebtRecovery** (CML responsibility "Pausing and resuming recovery"). It adds the
Resume Recovery command with its REST resource `POST /debt-recovery/riders/{riderId}/recovery/resume`: a paused
recovery runs again with its next attempt due at once and its failed attempts kept. Prerequisites: plans
`collections-agent-pauses-debt-recovery` (a paused recovery) and `collections-attempts-debt-collection` (attempts and
exhaustion, needed by R2). No integration, no context map change.

## Placement

| Spec element | Owner | Why |
|---|---|---|
| R1 A collections agent can resume a paused recovery; next attempt due at once | DebtRecovery | CML responsibility "Pausing and resuming recovery"; canvas UL "Resumed: the next one is due at once (DE-42)" |
| R2 A resumed recovery keeps its failed attempts | DebtRecovery | guards the failed-attempt count, DebtRecovery state (DE-42); canvas decision "A resumed recovery is due at once and keeps its count" |
| R3 Only a paused recovery can be resumed | DebtRecovery | guards the recovery's state (DE-89) |
| Command: Resume Recovery | DebtRecovery | DebtRecovery owns R3, the rule that rejects it |
| Event: Recovery Resumed | DebtRecovery | DebtRecovery decides the resume; no listener outside DebtRecovery |
| Query: none in the spec | n/a | tests observe through the Recovery Process query (`collections-starts-debt-recovery`, extended by `collections-agent-pauses-debt-recovery`) |

**Outcome:** one existing context.

## Context map changes

- None.

## Translation

| Spec term | Context | Context term | Where translated |
|---|---|---|---|
| Debt recovery | DebtRecovery | Recovery process | canvas UL |
| Resumed | DebtRecovery | Resumed (running again, due at once) | canvas UL (same) |
| Collections agent (resumed by) | DebtRecovery | Collections agent (`AgentId`) | REST adapter (`agentId` → `AgentId`) |

## Contract mapping

| Spec element | Context | Code |
|---|---|---|
| Command: Resume Recovery | DebtRecovery | `ResumeRecoveryUseCase` + record `ResumeRecovery(riderId, agentId, now)` in `api/usecases`; `POST /debt-recovery/riders/{riderId}/recovery/resume` with `{agentId}`; `409 {"error": "RecoveryNotPaused"}` for R3, also when the rider has no recovery process |
| Event: Recovery Resumed | DebtRecovery | domain event `RecoveryResumed(riderId, resumedBy, resumedAt, failedAttempts, nextAttemptDueAt)`; no integration event (no listener outside DebtRecovery) |
| Query: none in the spec | DebtRecovery | observation via `GET /debt-recovery/riders/{riderId}/recovery` |

## Canvas changes

### DebtRecovery (`docs/bounded-contexts/velocity/debtrecovery/canvas.md`)

Already in the canvas, tag only:
- "A resumed recovery is due at once and keeps its count" ← E1.1, E2.1.
- "Recovery is exhausted after the fifth failed attempt" ← E2.2 (the count kept across the pause reaches 5).

Missing, add:
- **Business decisions, add:**
  - `Only a paused recovery can be resumed (spec: collections-agent-resumes-debt-recovery#E3.1, #E3.2)`: Given Anna's recovery is running with 1 failed attempt and its next attempt due at 10:50 on 2026-11-11, When Kate resumes it at 15:00 on 2026-11-09, Then the resume is rejected and the next attempt stays due at 10:50 on 2026-11-11. The same for a recovery that succeeded while paused (E3.2).

## Decisions

| # | Decision | Choice | Reason |
|---|---|---|---|
| D1 | TDD checkpoint loop | **off** | the trainer explicitly asked for a fully autonomous run (2026-11-08); the tactical skill's pushback is recorded in `docs/bounded-contexts/velocity/conversation.md`. Domain still built test-first, one example per cycle |
| D2 | Aggregate style | state-based | simplest; no requirement for an event history |
| D3 | ACLs | one per outbound query/command, in `@AntiCorruptionLayer` packages, calling REST via `RestClient` + `VelocityServiceUrls`; inbound events via `@Async @EventListener` ACL listeners | integration-contracts.md "Integration style" (no ACL needed by this intent) |
| D4 | Persistence | in-memory repositories with `Version` optimistic locking, after the domain is green | memory "aggregate persistence after domain"; repo convention |
| D5 | Resume doesn't attempt by itself | resuming only sets the next attempt due at the resume time; the attempt is made by the next Attempt Due Collections run (scheduled job), as in E2.1 ("When the attempt at 09:00 … fails") | keeps one place that calls Settlement and sends reminders |

## Steps

### S1: Update the DebtRecovery canvas
- **Status:** done
- **Context:** DebtRecovery
- **Skill:** none (apply the approved canvas diff above)
- **Covers:** E3.1, E3.2 (tags for E1.1, E2.1, E2.2)
- **Done when:** canvas contains the new decision, tagged.

### S2: Resume Recovery api record (6a)
- **Status:** done
- **Context:** DebtRecovery
- **Skill:** bounded-context-scaffolder (update mode, 6a)
- **Covers:** Resume Recovery
- **Done when:** record `ResumeRecovery` in `api/usecases`; architecture test green; `./mvnw test` green.

### S3: Resume rules, test-first
- **Status:** done
- **Context:** DebtRecovery
- **Skill:** ddd-tactical-building-blocks (D1: loop off; one spec example per cycle, verbatim)
- **Covers:** R1, R2, R3, E1.1, E2.1, E2.2, E3.1, E3.2, Recovery Resumed
- **Done when:** one domain test per example; E2.1/E2.2 combine resume with the attempt outcome built by `collections-attempts-debt-collection`; `./mvnw test` green.

### S4: Use case and REST (6b)
- **Status:** done
- **Context:** DebtRecovery
- **Skill:** bounded-context-scaffolder (6b)
- **Covers:** Resume Recovery, Recovery Resumed
- **Done when:** `ResumeRecoveryUseCase`, `POST /debt-recovery/riders/{riderId}/recovery/resume` with the 409 mapping; `./mvnw test` green.

### S5: Acceptance tests
- **Status:** done
- **Context:** DebtRecovery
- **Skill:** none (implementation-planner)
- **Covers:** all examples
- **Done when:** `src/test/java/pl/wojtyna/dddtrainingv3/velocity/debtrecovery/CollectionsAgentResumesDebtRecoverySpecTest.java`, `@VelocitySpringTest`, one method per example, arranged with `UnpaidRideFixture`, `AttemptDueCollectionsUseCase` (prerequisite: `collections-attempts-debt-collection` S7) and the pause REST resource (prerequisite: `collections-agent-pauses-debt-recovery` S5); E3.2's "succeeded while paused" via Pay Outstanding Debt (prerequisite: plan `rider-pays-off-outstanding-debt`) with the Settlement → DebtRecovery hand-off asserted with Awaitility; `./mvnw test` green.

## Acceptance tests

| Example | Test |
|---|---|
| E1.1 | `CollectionsAgentResumesDebtRecoverySpecTest#e1_1_agent_resumes_paused_recovery_with_next_attempt_due_at_once` |
| E2.1 | `CollectionsAgentResumesDebtRecoverySpecTest#e2_1_failed_attempt_after_resume_continues_the_kept_count` |
| E2.2 | `CollectionsAgentResumesDebtRecoverySpecTest#e2_2_fifth_failure_after_resume_exhausts_recovery` |
| E3.1 | `CollectionsAgentResumesDebtRecoverySpecTest#e3_1_running_recovery_cannot_be_resumed` |
| E3.2 | `CollectionsAgentResumesDebtRecoverySpecTest#e3_2_succeeded_recovery_cannot_be_resumed` |

## Traceability

| Spec ID | Step | Files |
|---|---|---|
| R1 / E1.1 | S1 (tag), S3, S4, S5 | `domain/recovery/RecoveryProcess.java`, `RecoveryProcessTest`; `api/usecases/ResumeRecoveryUseCase`; `infra/adapters/primary/DebtRecoveryController`; `CollectionsAgentResumesDebtRecoverySpecTest` |
| R2 / E2.1 | S1 (tag), S3, S5 | `domain/recovery/RecoveryProcess.java`, `RecoveryProcessTest`; `api/usecases/ResumeRecoveryUseCase`; `infra/adapters/primary/DebtRecoveryController`; `CollectionsAgentResumesDebtRecoverySpecTest` |
| R2 / E2.2 | S1 (tag), S3, S5 | `domain/recovery/RecoveryProcess.java`, `RecoveryProcessTest`; `api/usecases/ResumeRecoveryUseCase`; `infra/adapters/primary/DebtRecoveryController`; `CollectionsAgentResumesDebtRecoverySpecTest` |
| R3 / E3.1 | S1, S3, S4, S5 | `domain/recovery/RecoveryProcess.java`, `RecoveryProcessTest`; `api/usecases/ResumeRecoveryUseCase`; `infra/adapters/primary/DebtRecoveryController`; `CollectionsAgentResumesDebtRecoverySpecTest` |
| R3 / E3.2 | S1, S3, S4, S5 | `domain/recovery/RecoveryProcess.java`, `RecoveryProcessTest`; `api/usecases/ResumeRecoveryUseCase`; `infra/adapters/primary/DebtRecoveryController`; `CollectionsAgentResumesDebtRecoverySpecTest` |

## Risks and open questions

- **CONTRACT GAP:** `POST /debt-recovery/riders/{riderId}/recovery/resume` has no body; the spec's Recovery Resumed needs *resumed by*. Planned as `{agentId}`.
- **Time between resume and attempt:** with the job's fixed delay, the attempt after a resume happens at the next run, not at the exact resume instant; "due at once" is about due-ness, which the domain asserts.
- No differences from the reference context map.

## Review notes

- 2026-11-08: draft written (autonomous run); self-review against plan-critic.md checklist: AGREE (all elements in DebtRecovery; one tactical run of 3 rules guarding the same pause/run state; all 5 examples covered; prerequisites on the pause and attempt plans explicit).

- 2026-11-08 (DebtRecovery wave, autonomous run): built. Decisions taken: Resume answers `200` with the Recovery Process view; a rider without a recovery process gets `409 RecoveryNotPaused`.

## Changelog

### 2026-11-08: planned
- Built from spec dated 2026-11-08.

### 2026-11-08: delivered (DebtRecovery wave)
- S1-S5 done; acceptance tests `CollectionsAgentResumesDebtRecoverySpecTest` (5).
