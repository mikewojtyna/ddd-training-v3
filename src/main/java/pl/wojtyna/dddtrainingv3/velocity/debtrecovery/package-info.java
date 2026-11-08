/// # DebtRecovery bounded context
///
/// ## Purpose
/// Actively pursues money riders owe after the quick automatic retries gave up: reminds the rider and asks Settlement
/// to collect, on a schedule, until the debt is paid or recovery is exhausted, and lets the business pause the pursuit
/// for disputes, payment-plan talks or regulatory holds without abandoning it.
///
/// ## Strategic classification
/// - Domain: supporting
/// - Business model: revenue
/// - Evolution: custom built
///
/// ## Domain roles
/// - Execution Model: runs each recovery process.
///
/// ## Ubiquitous language
/// - **Recovery process**: the pursuit of one rider's outstanding debt; one open process per rider (`RecoveryProcess`,
///   identified by `RiderId`).
/// - **Open**: running or paused; neither succeeded nor exhausted (`RecoveryStatus#isOpen`).
/// - **Collection attempt**: a payment reminder (port `PaymentReminders`) plus a request to Settlement to collect the
///   whole outstanding debt (port `DebtCollection`) (`CollectionAttempt`, `CollectionOutcome`).
/// - **Due**: the next collection attempt's time has come: at once on start or resume, then 72 hours after a failed
///   attempt (DE-40, DE-90) (`RecoveryProcess#attemptDueAt`).
/// - **Paused**: no attempts are made; reason *dispute*, *payment plan* or *regulatory hold* (`PauseReason`).
/// - **Resumed**: attempts continue; the next one is due at once (DE-42).
/// - **Succeeded**: the debt was cleared.
/// - **Exhausted**: 5 collection attempts failed (DE-41).
///
/// ## Business decisions
/// All enforced by the aggregate `RecoveryProcess` (`domain/recovery`), see `RecoveryProcessTest`.
/// - **An unpaid fee starts recovery with an immediate attempt**: given Anna has no recovery process, when her fee of
///   PLN 5.00 is unpaid at 10:50 on 2026-11-08, then a recovery starts (Recovery Started), first attempt due at 10:50.
///   See `#unpaidFeeStartsRecoveryWithFirstAttemptDueAtOnce`.
/// - **A due attempt asks for the whole debt; not a minute early**: see `#dueAttemptAsksForWholeOutstandingDebt`,
///   `#noAttemptAMinuteBeforeItIsDue`.
/// - **A failed collection schedules the next attempt in 72 hours**: see `#failedAttemptCountsAndNextIsDueIn72Hours`.
/// - **Recovery is exhausted after the fifth failed attempt** and is never due again: see
///   `#fifthFailedAttemptExhaustsRecovery`, `#exhaustedRecoveryIsNeverDue`.
/// - **A successful collection ends recovery** (Recovery Succeeded): see `#collectedDebtEndsRecoveryAsSucceeded`.
/// - **A second unpaid fee joins the open recovery** (running or paused, count and due time kept, DE-44, DE-88): see
///   `#furtherUnpaidFeeJoinsRunningRecovery`, `#unpaidFeeJoinsPausedRecoveryWhichStaysPaused`.
/// - **After success or exhaustion a new unpaid fee starts a new recovery** (DE-88): see
///   `#unpaidFeeAfterSucceededRecoveryStartsNewRecovery`, `#unpaidFeeAfterExhaustedRecoveryStartsNewRecoveryForWholeDebt`.
/// - **Pausing keeps the failed attempts; only a running recovery can be paused** (DE-89): see
///   `#agentPausesRunningRecoveryKeepingFailedAttempts`, `#agentPausesRecoveryForPaymentPlan`,
///   `#pausedRecoveryCannotBePausedAgain`, `#exhaustedRecoveryCannotBePaused` (`RecoveryNotRunningException`).
/// - **Only the three pause reasons are accepted**: the closed type `PauseReason`; anything else is `400
///   UnknownPauseReason` at the REST edge (`CollectionsAgentPausesDebtRecoverySpecTest#e2_2_...`).
/// - **A paused recovery makes no attempts**: see `#pausedRecoveryIsNeverDue`.
/// - **A cleared debt ends recovery**, also when paused or after exhaustion; a second clearance changes nothing
///   (DE-45): see `#debtClearedWhilePausedEndsRecoveryAsSucceeded`, `#debtClearedAfterExhaustionRecordsSuccess`,
///   `#secondClearanceChangesNothing`; a clearance from before the recovery started doesn't end it:
///   `#clearanceFromBeforeTheRecoveryStartedDoesNotEndIt`.
/// - **A resumed recovery is due at once and keeps its count; only a paused recovery can be resumed** (DE-42, DE-89):
///   see `#agentResumesPausedRecoveryWithNextAttemptDueAtOnce`, `#failedAttemptAfterResumeContinuesTheKeptCount`,
///   `#fifthFailureAfterResumeExhaustsRecovery`, `#runningRecoveryCannotBeResumed`,
///   `#succeededRecoveryCannotBeResumed` (`RecoveryNotPausedException`).
/// - **An attempt in flight when the recovery was paused or ended doesn't count** (a collection that collected is
///   still recorded as made): see `#attemptRecordedAfterRecoveryStoppedRunningDoesNotCount`.
///
/// ## Assumptions
/// - DE-40 to DE-45, DE-88 to DE-90 (role-played domain expert).
/// - Exhausted debt stays with the rider, who stays blocked (Settlement/Accounts).
///
/// ## Verification metrics
/// - Share of recovery processes that succeed.
/// - Average days to recover a debt.
///
/// ## Open questions
/// - Billing disputes raised in Customer Support should pause recovery automatically (reference map: deferred).
/// - Payment-plan instalments (DE-43: out of scope).
///
/// Source canvas: `docs/bounded-contexts/velocity/debtrecovery/canvas.md`
@BoundedContext(name = "DebtRecovery")
@Module
package pl.wojtyna.dddtrainingv3.velocity.debtrecovery;

import org.jmolecules.ddd.annotation.BoundedContext;
import org.jmolecules.ddd.annotation.Module;
