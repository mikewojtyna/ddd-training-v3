# DebtRecovery bounded context: implementation plan

- **System / package**: `velocity.debtrecovery`
- **Mode**: scaffold
- **Source canvas**: `docs/bounded-contexts/velocity/debtrecovery/canvas.md`
- **Last updated**: 2026-11-08
- **Status**: implemented

## Review notes
- 2026-11-08: approved via docs/plans/collections-starts-debt-recovery.md, docs/plans/collections-attempts-debt-collection.md, docs/plans/collections-agent-pauses-debt-recovery.md and docs/plans/collections-agent-resumes-debt-recovery.md (autonomous run, 2026-11-08). Structural decisions and the TDD-loop choice (off, D1 in every plan) come from those plans.
- Decisions the intent plans left open, taken here (autonomous run): see the Decisions table rows marked *(new)*.

## Canvas summary
DebtRecovery actively pursues riders' outstanding debt once Settlement's retries gave up: a collection attempt
(reminder + collection through Settlement) at once and then every 72 hours, success when the debt is cleared,
exhaustion after 5 failed attempts, pause/resume by collections agents. Supporting domain, revenue, custom built;
role: Execution Model.

## Decisions
| Decision | Choice | Reason |
|---|---|---|
| Modules | 1 module = 1 context | default |
| ACL towards Settlement (inbound, Rental Fee Unpaid / Outstanding Debt Cleared) | yes: `infra/adapters/primary/settlementctxacl/RentalFeeUnpaidListener`, `OutstandingDebtClearedListener` (`@Async @EventListener`) | integration-contracts.md; CML `Settlement [U,S,PL]->[D,C] DebtRecovery` |
| ACL towards Settlement (Collect Outstanding Debt) | yes: port `domain/recovery/DebtCollection`, adapter `infra/adapters/secondary/settlementctxacl/SettlementDebtCollection` (own response record; an error status or no answer = unanswered, failed collection) | plans' D3; conformist in terms, ACL in structure |
| Messaging provider (Payment Reminder) | port `domain/recovery/PaymentReminders`, simulated recording adapter `infra/adapters/secondary/messagingacl/SimulatedPaymentReminders` | integration-contracts.md "Additions" |
| Boundary exceptions | none | |
| Domain layer | delegated to `ddd-tactical-building-blocks` (always) | |
| TDD checkpoint loop | off (trainer's autonomy request, 2026-11-08, plans' D1) | domain still built test-first, one example per cycle, red seen before green (see TDD log) |
| Aggregate style | state-based (plans' D2) | |
| Aggregate identity *(new)* | one `RecoveryProcess` per rider, identified by `RiderId`; a new recovery after success/exhaustion restarts the same aggregate (Recovery Started, count 0) | "one open recovery per rider" is then guarded by the `Version` check on one key (two concurrent unpaid fees can't start two processes); no rule reads an ended process's history |
| Outstanding debt pursued *(new)* | the absolute `outstandingDebt` of the latest Rental Fee Unpaid (local copy); an attempt asks for it, Settlement collects whatever is owed at that moment | DE-40 "the whole outstanding debt"; plan pauses' risk note (stale copy acceptable) |
| Who decides success after a collection | the attempt (plan attempts D5): Settlement's `collected=true` or `outstandingDebt=0.00` ends the recovery; the Outstanding Debt Cleared for the same collection finds it succeeded and changes nothing | |
| Attempt in flight while paused/ended *(new)* | recording a failed attempt on a recovery that is no longer running changes nothing; a successful one is still recorded as made (Collection Attempted) and succeeds the recovery if not yet succeeded | race between the attempt and an agent's pause or a clearance (plan attempts D6) |
| Late clearance *(new)* | an Outstanding Debt Cleared from before the recovery started doesn't end it (state `startedAt`) | async events may arrive late (e.g. the clearance of the previous recovery after a new one started) |
| Version conflicts | `RecoveryProcessChange` (api/usecases) reloads and re-runs the command up to 3 times, for every use case | plans' D6 (attempts), S5 (starts) |
| Ports called by the use case only | `AttemptDueCollectionsUseCase`: fresh load → `attemptDueAt(now)` → `PaymentReminders.remind` → `DebtCollection.collect` → `record(attempt).as(outcome)` → save → publish; one rider's failure is logged and doesn't stop the others | plan attempts D7 |
| Rider without a recovery process *(new)* | Pause → `409 RecoveryNotRunning` (plan pauses D6); Resume → `409 RecoveryNotPaused`; Outstanding Debt Cleared → ignored | same rules, one error name per command |
| Pause reason outside the list | `UnknownPauseReason` (api/usecases, extends `IllegalArgumentException`) → `400 {"error": "UnknownPauseReason"}` | plan pauses D5 |
| Pause/resume answer *(new)* | `200` with the Recovery Process view | the contract gives no body; the agent sees the result at once |
| Integration events | none (`api/events` not created) | canvas Outbound: no listener outside DebtRecovery |
| Job interval *(new)* | `DueCollectionsJob`, `fixedDelay` 60 s (`velocity.debtrecovery.collection-attempt-delay-ms`) | due-ness is checked against the clock; the interval only bounds lateness |
| Persistence | `InMemoryRecoveryProcesses` (memento `Snapshot`, `Version` optimistic locking), also the Recovery Process read model | plans' D4 |

## Mapping: canvas → code
### Inbound
| Collaborator | Type | Message | Code |
|---|---|---|---|
| Settlement | Event | Rental Fee Unpaid | `RentalFeeUnpaidListener` → `StartDebtRecovery` + `StartDebtRecoveryUseCase` |
| Settlement | Event | Outstanding Debt Cleared | `OutstandingDebtClearedListener` → `RecordDebtCleared` + `RecordDebtClearedUseCase` |
| Collections (clock) | Command | Attempt Due Collections | `AttemptDueCollections` + `AttemptDueCollectionsUseCase`, triggered by `infra/adapters/primary/scheduling/DueCollectionsJob` |
| Collections agent | Command | Pause Recovery (reason) | `PauseRecovery` + `PauseRecoveryUseCase`; `POST /debt-recovery/riders/{riderId}/recovery/pause` `{agentId, reason}` |
| Collections agent | Command | Resume Recovery | `ResumeRecovery` + `ResumeRecoveryUseCase`; `POST /debt-recovery/riders/{riderId}/recovery/resume` `{agentId}` |
| Collections agent | Query | Recovery Process | `RecoveryProcessQuery`, `RecoveryProcessView`, `RecoveryProcessesReadModel`, `RecoveryProcessQueryHandler`; `GET /debt-recovery/riders/{riderId}/recovery` (`404` without a process) |

### Outbound
| Message | Type | Collaborator | Port | Adapter (ACL package) |
|---|---|---|---|---|
| Collect Outstanding Debt | Command | Settlement | `domain/recovery/DebtCollection` | `SettlementDebtCollection` (`settlementctxacl`) |
| Payment Reminder | Command | messaging provider | `domain/recovery/PaymentReminders` | `SimulatedPaymentReminders` (`messagingacl`) |
| Recovery Started / Succeeded / Exhausted / Paused / Resumed, Collection Attempted | Event | none outside DebtRecovery | n/a | domain events only (`RecoveryEvent`), published through `DomainEventPublisher` |

### Ubiquitous language
| Term | Code type | Where it appears |
|---|---|---|
| Recovery process | `RecoveryProcess` (aggregate root), `RiderId` | domain, `RecoveryProcessView` |
| Open | `RecoveryStatus#isOpen` | domain |
| Collection attempt | `CollectionAttempt`, `CollectionOutcome`, `RecoveryProcess#attemptDueAt` / `#record` | domain |
| Due | `RecoveryProcess#attemptDueAt`, `nextAttemptDueAt` | domain, view |
| Paused / pause reason | `RecoveryStatus.PAUSED`, `PauseReason` | domain, view |
| Resumed | `RecoveryProcess#resume`, `RecoveryResumed` | domain |
| Succeeded / Exhausted | `RecoveryStatus.SUCCEEDED` / `EXHAUSTED`, `RecoverySucceeded` / `RecoveryExhausted` | domain, view |
| Collections agent | `AgentId` | domain, REST body |

### Business rules → delegated to `ddd-tactical-building-blocks`
| Rule (GWT) | Status | Building block | Test |
|---|---|---|---|
| Unpaid fee starts recovery, first attempt at once (starts E1.1) | green | `RecoveryProcess#unpaidFee` | `RecoveryProcessTest#unpaidFeeStartsRecoveryWithFirstAttemptDueAtOnce` |
| Due attempt asks for the whole debt (attempts E1.1) | green | `#attemptDueAt`, `#record` | `#dueAttemptAsksForWholeOutstandingDebt` |
| Failed attempt counts, next in 72 h (attempts E3.1) | green | `#record` | `#failedAttemptCountsAndNextIsDueIn72Hours` |
| Not a minute early (attempts E1.2) | green | `#attemptDueAt` | `#noAttemptAMinuteBeforeItIsDue` |
| 5th failure exhausts (attempts E4.1; resumes E2.2) | green | `#record` | `#fifthFailedAttemptExhaustsRecovery`, `#fifthFailureAfterResumeExhaustsRecovery` |
| Exhausted never due (attempts E4.2) | green | `#attemptDueAt` | `#exhaustedRecoveryIsNeverDue` |
| Collected debt ends recovery (attempts E2.1) | green | `#record` | `#collectedDebtEndsRecoveryAsSucceeded` |
| Fee joins running recovery (starts E2.1) | green | `#unpaidFee` | `#furtherUnpaidFeeJoinsRunningRecovery` |
| Pause keeps count (pauses E1.1, E2.1) | green | `#pause` | `#agentPausesRunningRecoveryKeepingFailedAttempts`, `#agentPausesRecoveryForPaymentPlan` |
| Only running can be paused (pauses E4.3, E4.1) | green | `#pause`, `RecoveryNotRunningException` | `#pausedRecoveryCannotBePausedAgain`, `#exhaustedRecoveryCannotBePaused` |
| Paused never due (pauses E3.1) | green | `#attemptDueAt` | `#pausedRecoveryIsNeverDue` |
| Fee joins paused recovery (starts E2.2) | green | `#unpaidFee` | `#unpaidFeeJoinsPausedRecoveryWhichStaysPaused` |
| Cleared debt ends recovery, idempotent (attempts E5.1-E5.3) | green | `#debtCleared` | `#debtClearedWhilePausedEndsRecoveryAsSucceeded`, `#debtClearedAfterExhaustionRecordsSuccess`, `#secondClearanceChangesNothing` |
| New recovery after success/exhaustion (starts E3.1, E3.2) | green | `#unpaidFee` | `#unpaidFeeAfterSucceededRecoveryStartsNewRecovery`, `#unpaidFeeAfterExhaustedRecoveryStartsNewRecoveryForWholeDebt` |
| Resume due at once, count kept (resumes E1.1, E2.1) | green | `#resume` | `#agentResumesPausedRecoveryWithNextAttemptDueAtOnce`, `#failedAttemptAfterResumeContinuesTheKeptCount` |
| Only paused can be resumed (resumes E3.1, E3.2) | green | `#resume`, `RecoveryNotPausedException` | `#runningRecoveryCannotBeResumed`, `#succeededRecoveryCannotBeResumed` |
| Attempt in flight while paused/ended (decision) | green | `#record` | `#attemptRecordedAfterRecoveryStoppedRunningDoesNotCount` |
| Late clearance (decision) | green | `#debtCleared` | `#clearanceFromBeforeTheRecoveryStartedDoesNotEndIt` |

### Rules awaiting GWT
- None.

## Deferred until tactical blocks are built
- All generated (see Files).

## Differences from the reference context map
| Canvas says | Reference map says | Covered by "Accepted Alternatives"? |
|---|---|---|
| none | | |

## Files
```
src/main/java/pl/wojtyna/dddtrainingv3/velocity/debtrecovery/
  package-info.java
  api/usecases/StartDebtRecovery(.java|UseCase), AttemptDueCollections(.java|UseCase), RecordDebtCleared(.java|UseCase),
               PauseRecovery(.java|UseCase), ResumeRecovery(.java|UseCase), UnknownPauseReason, RecoveryProcessChange
  api/queries/RecoveryProcessQuery, RecoveryProcessView, RecoveryProcessesReadModel, RecoveryProcessQueryHandler
  domain/recovery/RecoveryProcess, RecoveryEvent, RecoveryStatus, PauseReason, RiderId, AgentId, Pln,
                  CollectionAttempt, CollectionOutcome, FluentLanguage, RecoveryNotRunningException,
                  RecoveryNotPausedException, RecoveryProcesses, DebtCollection, PaymentReminders
  infra/adapters/primary/DebtRecoveryController
  infra/adapters/primary/scheduling/DueCollectionsJob
  infra/adapters/primary/settlementctxacl/package-info, RentalFeeUnpaidListener, OutstandingDebtClearedListener
  infra/adapters/secondary/persistence/InMemoryRecoveryProcesses
  infra/adapters/secondary/settlementctxacl/package-info, SettlementDebtCollection
  infra/adapters/secondary/messagingacl/package-info, SimulatedPaymentReminders
  infra/config/DebtRecoveryContextConfig
src/test/java/pl/wojtyna/dddtrainingv3/velocity/debtrecovery/
  DebtRecoveryArchitectureTest, UnpaidRideFixture, DebtRecoverySpecTestSupport
  CollectionsStartsDebtRecoverySpecTest, CollectionsAttemptsDebtCollectionSpecTest,
  CollectionsAgentPausesDebtRecoverySpecTest, CollectionsAgentResumesDebtRecoverySpecTest
  domain/recovery/RecoveryProcessTest
  infra/adapters/secondary/persistence/InMemoryRecoveryProcessesTest
```

## Stubs and follow-ups
- No stubs. The payment reminder is simulated (recorded in memory); a real messaging provider is a follow-up.
- Rentals is built in parallel: the acceptance tests publish `rentals.api.events.RentalSessionEnded` themselves to make real Settlement debt (`UnpaidRideFixture`).
- Billing disputes from Customer Support don't pause recovery automatically (reference map: deferred).
- CML / context map: no change needed.

## Changelog
### 2026-11-08: scaffolded
- Added: everything listed under Files.
- Triggered by: intents `docs/plans/collections-starts-debt-recovery.md`, `docs/plans/collections-attempts-debt-collection.md` (all DebtRecovery steps; S2 was Settlement's), `docs/plans/collections-agent-pauses-debt-recovery.md`, `docs/plans/collections-agent-resumes-debt-recovery.md`.
- Canvas: UL "Open"; existing decisions tagged with spec IDs; 7 decisions added from the four plans.

#### TDD log (checkpoint loop off, all implemented by Claude)
Signatures sketched up front (`unpaidFee(debt).at(t)`, `attemptDueAt(now)`, `record(attempt).as(outcome)`,
`debtCleared(t)`, `pause(reason).by(agent).at(t)`, `resume().by(agent).at(t)`) with dumb bodies; each cycle ran the
new test alone (red), then `RecoveryProcessTest` (green); the whole suite ran after the domain, after persistence and
at the end.

| # | Scenario | Block | Test | Red seen |
|---|---|---|---|---|
| 1 | starts E1.1 unpaid fee starts recovery | `#unpaidFee` | `unpaidFeeStartsRecoveryWithFirstAttemptDueAtOnce` | yes (no events) |
| 2 | attempts E1.1 due attempt asks for PLN 5.00 | `#attemptDueAt`, `#record` | `dueAttemptAsksForWholeOutstandingDebt` | yes (empty attempt) |
| 3 | attempts E3.1 failed attempt, next in 72 h | `#record` | `failedAttemptCountsAndNextIsDueIn72Hours` | no: green at once (cycle 2) |
| 4 | attempts E1.2 not at 10:49 | `#attemptDueAt` | `noAttemptAMinuteBeforeItIsDue` | yes (attempt returned) |
| 5 | attempts E4.1 5th failure exhausts | `#record` | `fifthFailedAttemptExhaustsRecovery` | yes (no Recovery Exhausted) |
| 6 | attempts E4.2 exhausted never due | `#attemptDueAt` | `exhaustedRecoveryIsNeverDue` | yes (NullPointerException in the when: no due time) |
| 7 | attempts E2.1 collected → succeeded | `#record` | `collectedDebtEndsRecoveryAsSucceeded` | yes (failed attempt recorded) |
| 8 | starts E2.1 fee joins running recovery | `#unpaidFee` | `furtherUnpaidFeeJoinsRunningRecovery` | yes (Recovery Started again) |
| 9 | pauses E1.1 pause keeps count | `#pause` | `agentPausesRunningRecoveryKeepingFailedAttempts` | yes (no events) |
| 10 | pauses E2.1 payment plan | `#pause` | `agentPausesRecoveryForPaymentPlan` | no separate red: written with cycle 9, green with its implementation |
| 11-12 | pauses E4.3, E4.1 only running can be paused | `#pause` | `pausedRecoveryCannotBePausedAgain`, `exhaustedRecoveryCannotBePaused` | yes, both (no exception) |
| 13 | pauses E3.1 paused never due | `#attemptDueAt` | `pausedRecoveryIsNeverDue` | no: green at once (cycle 6's status check) |
| 14 | starts E2.2 fee joins paused recovery | `#unpaidFee` | `unpaidFeeJoinsPausedRecoveryWhichStaysPaused` | no: green at once (cycle 8's "open") |
| 15-17 | attempts E5.1-E5.3 cleared debt | `#debtCleared` | `debtClearedWhilePausedEndsRecoveryAsSucceeded`, `debtClearedAfterExhaustionRecordsSuccess`, `secondClearanceChangesNothing` | yes, all three (no events / status) |
| 18-19 | starts E3.1, E3.2 new recovery after an ended one | `#unpaidFee` | `unpaidFeeAfterSucceededRecoveryStartsNewRecovery`, `unpaidFeeAfterExhaustedRecoveryStartsNewRecoveryForWholeDebt` | no: green at once (cycle 8) |
| 20 | resumes E1.1 due at once | `#resume` | `agentResumesPausedRecoveryWithNextAttemptDueAtOnce` | yes (no events) |
| 21-22 | resumes E2.1, E2.2 count kept after resume | `#record` | `failedAttemptAfterResumeContinuesTheKeptCount`, `fifthFailureAfterResumeExhaustsRecovery` | no: green at once (cycles 2, 5, 20) |
| 23-24 | resumes E3.1, E3.2 only paused can be resumed | `#resume` | `runningRecoveryCannotBeResumed`, `succeededRecoveryCannotBeResumed` | yes, both (no exception) |
| 25 | decision: attempt in flight while paused doesn't count | `#record` | `attemptRecordedAfterRecoveryStoppedRunningDoesNotCount` | yes (failure counted) |
| 26 | decision: late clearance | `#debtCleared` | `clearanceFromBeforeTheRecoveryStartedDoesNotEndIt` | yes (Recovery Succeeded) |
| 27 | decision refined: a collection in flight is still recorded as made | `#record` | `attemptRecordedAfterRecoveryStoppedRunningDoesNotCount` (expectation changed) | yes (no Collection Attempted) |
| - | optimistic locking, running candidates | `InMemoryRecoveryProcesses` | `InMemoryRecoveryProcessesTest` | written after the domain (persistence after domain) |

Deviations: cycles 9-10, 11-12, 15-17, 21-24 were written as batches (one red run showing each failing for its own
reason, or green at once, then one implementation). Cycles 25-27 came from the acceptance tests' races and are
decisions, not spec examples.
