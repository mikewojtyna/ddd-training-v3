package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery;

import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.RecoveryEvent.RecoveryExhausted;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.RecoveryEvent.RecoverySucceeded;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.RecoveryEvent.RecoveryPaused;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.RecoveryEvent.RecoveryResumed;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.RecoveryEvent.CollectionAttempted;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.RecoveryEvent.RecoveryStarted;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.Pln.pln;

/// Recovery-process examples of `docs/specs/collections-starts-debt-recovery.md`,
/// `docs/specs/collections-attempts-debt-collection.md`, `docs/specs/collections-agent-pauses-debt-recovery.md` and
/// `docs/specs/collections-agent-resumes-debt-recovery.md`.
@DisplayName("Recovery process examples")
class RecoveryProcessTest {

    private static final RiderId ANNA = new RiderId("anna");
    private static final RiderId BEN = new RiderId("ben");
    private static final AgentId KATE = new AgentId("kate");

    private static Instant at(String dateTime) {
        return Instant.parse("2026-" + dateTime + ":00Z");
    }

    // @formatter:off
    @DisplayName(
        """
         Starts E1.1 given rider Anna has no debt recovery and no outstanding debt,
         when her rental fee of PLN 5.00 for ride S-1 is unpaid at 10:50 on 2026-11-08 (outstanding debt PLN 5.00),
         then a debt recovery starts for Anna with 0 failed attempts and the first collection attempt due at 10:50
         on 2026-11-08 (Recovery Started)
        """
    )
    // @formatter:on
    @Test
    void unpaidFeeStartsRecoveryWithFirstAttemptDueAtOnce() {
        // given
        var process = RecoveryProcess.none(ANNA);

        // when
        var events = process.unpaidFee(pln("5.00")).at(at("11-08T10:50"));

        // then
        assertThat(events.stream()).containsExactly(
            new RecoveryStarted(ANNA, pln("5.00"), at("11-08T10:50"), at("11-08T10:50")));
        assertThat(process.status()).isEqualTo(RecoveryStatus.RUNNING);
        assertThat(process.failedAttempts()).isZero();
        assertThat(process.nextAttemptDueAt()).contains(at("11-08T10:50"));
    }

    // @formatter:off
    @DisplayName(
        """
         Attempts E1.1 given Anna's recovery started at 10:50 on 2026-11-08 with outstanding debt PLN 5.00 and its
         first attempt due at once,
         when due collections are attempted at 10:50 on 2026-11-08,
         then Anna gets a payment reminder and collection of PLN 5.00 is tried (Collection Attempted, attempt 1)
        """
    )
    // @formatter:on
    @Test
    void dueAttemptAsksForWholeOutstandingDebt() {
        // given
        var process = started(ANNA, "5.00", "11-08T10:50");

        // when
        var attempt = process.attemptDueAt(at("11-08T10:50"));

        // then
        assertThat(attempt).contains(new CollectionAttempt(ANNA, 1, pln("5.00"), at("11-08T10:50")));
        assertThat(process.record(attempt.orElseThrow()).as(CollectionOutcome.failed(pln("5.00"))).stream())
            .containsExactly(new CollectionAttempted(ANNA, 1, pln("5.00"), false, at("11-08T10:50"), at("11-11T10:50")));
    }

    // @formatter:off
    @DisplayName(
        """
         Attempts E3.1 given Anna's recovery has 0 failed attempts,
         when the attempt at 10:50 on 2026-11-08 fails,
         then Anna's recovery has 1 failed attempt and the next attempt is due at 10:50 on 2026-11-11
         (Collection Attempted)
        """
    )
    // @formatter:on
    @Test
    void failedAttemptCountsAndNextIsDueIn72Hours() {
        // given
        var process = started(ANNA, "5.00", "11-08T10:50");
        var attempt = process.attemptDueAt(at("11-08T10:50")).orElseThrow();

        // when
        var events = process.record(attempt).as(CollectionOutcome.failed(pln("5.00")));

        // then
        assertThat(events.hasOccurredEventOfType(CollectionAttempted.class)).isTrue();
        assertThat(process.failedAttempts()).isEqualTo(1);
        assertThat(process.nextAttemptDueAt()).contains(at("11-11T10:50"));
        assertThat(process.status()).isEqualTo(RecoveryStatus.RUNNING);
    }

    // @formatter:off
    @DisplayName(
        """
         Attempts E1.2 given Anna's recovery is running with 1 failed attempt and its next attempt due at 10:50
         on 2026-11-11,
         when due collections are attempted at 10:49 on 2026-11-11,
         then no attempt is made for Anna (72 hours to the minute, DE-90)
        """
    )
    // @formatter:on
    @Test
    void noAttemptAMinuteBeforeItIsDue() {
        // given
        var process = withFailedAttempts(ANNA, 1, "11-08T10:50");

        // when
        var attempt = process.attemptDueAt(at("11-11T10:49"));

        // then
        assertThat(attempt).isEmpty();
        assertThat(process.attemptDueAt(at("11-11T10:50"))).isPresent();
    }

    /// Started at `startedAt` for PLN 5.00, then `failed` attempts failed, each when it was due (every 72 hours).
    private static RecoveryProcess withFailedAttempts(RiderId rider, int failed, String startedAt) {
        var process = started(rider, "5.00", startedAt);
        for (var i = 0; i < failed; i++) {
            var attempt = process.attemptDueAt(process.nextAttemptDueAt().orElseThrow()).orElseThrow();
            process.record(attempt).as(CollectionOutcome.failed(pln("5.00")));
        }
        return process;
    }

    // @formatter:off
    @DisplayName(
        """
         Attempts E4.1 given Anna's recovery has 4 failed attempts (2026-11-08, 11-11, 11-14, 11-17) and PLN 5.00
         outstanding,
         when the attempt at 10:50 on 2026-11-20 fails,
         then the recovery is exhausted with 5 failed attempts (Collection Attempted, Recovery Exhausted)
         and Anna still owes PLN 5.00
        """
    )
    // @formatter:on
    @Test
    void fifthFailedAttemptExhaustsRecovery() {
        // given
        var process = withFailedAttempts(ANNA, 4, "11-08T10:50");
        var attempt = process.attemptDueAt(at("11-20T10:50")).orElseThrow();

        // when
        var events = process.record(attempt).as(CollectionOutcome.failed(pln("5.00")));

        // then
        assertThat(events.stream()).containsExactly(
            new CollectionAttempted(ANNA, 5, pln("5.00"), false, at("11-20T10:50"), null),
            new RecoveryExhausted(ANNA, at("11-20T10:50"), pln("5.00")));
        assertThat(process.status()).isEqualTo(RecoveryStatus.EXHAUSTED);
        assertThat(process.failedAttempts()).isEqualTo(5);
        assertThat(process.nextAttemptDueAt()).isEmpty();
    }

    // @formatter:off
    @DisplayName(
        """
         Attempts E4.2 given Anna's recovery was exhausted at 10:50 on 2026-11-20,
         when due collections are attempted at 10:50 on 2026-11-23,
         then no attempt is made for Anna and she gets no reminder
        """
    )
    // @formatter:on
    @Test
    void exhaustedRecoveryIsNeverDue() {
        // given
        var process = withFailedAttempts(ANNA, 5, "11-08T10:50");

        // when
        var attempt = process.attemptDueAt(at("11-23T10:50"));

        // then
        assertThat(attempt).isEmpty();
    }

    // @formatter:off
    @DisplayName(
        """
         Attempts E2.1 given Anna's recovery has 1 failed attempt and its next attempt is due at 10:50 on 2026-11-11,
         when the attempt at 10:50 on 2026-11-11 collects PLN 5.00,
         then the recovery succeeded (Collection Attempted, Recovery Succeeded) and no further attempts are due
        """
    )
    // @formatter:on
    @Test
    void collectedDebtEndsRecoveryAsSucceeded() {
        // given
        var process = withFailedAttempts(ANNA, 1, "11-08T10:50");
        var attempt = process.attemptDueAt(at("11-11T10:50")).orElseThrow();

        // when
        var events = process.record(attempt).as(CollectionOutcome.collected(pln("0.00")));

        // then
        assertThat(events.stream()).containsExactly(
            new CollectionAttempted(ANNA, 2, pln("5.00"), true, at("11-11T10:50"), null),
            new RecoverySucceeded(ANNA, at("11-11T10:50"), 1));
        assertThat(process.status()).isEqualTo(RecoveryStatus.SUCCEEDED);
        assertThat(process.attemptDueAt(at("11-30T10:50"))).isEmpty();
    }

    // @formatter:off
    @DisplayName(
        """
         Starts E2.1 given Anna's recovery is running with 2 failed attempts, her outstanding debt is PLN 5.00 and
         her next attempt is due at 10:50 on 2026-11-14,
         when her rental fee of PLN 43.00 for ride S-7 is unpaid at 18:05 on 2026-11-12 (outstanding debt PLN 48.00),
         then no new recovery starts and Anna's recovery keeps 2 failed attempts, its next attempt still due at 10:50
         on 2026-11-14, and now pursues PLN 48.00
        """
    )
    // @formatter:on
    @Test
    void furtherUnpaidFeeJoinsRunningRecovery() {
        // given
        var process = withFailedAttempts(ANNA, 2, "11-08T10:50");

        // when
        var events = process.unpaidFee(pln("48.00")).at(at("11-12T18:05"));

        // then
        assertThat(events.hasOccurredEventOfType(RecoveryStarted.class)).isFalse();
        assertThat(process.status()).isEqualTo(RecoveryStatus.RUNNING);
        assertThat(process.failedAttempts()).isEqualTo(2);
        assertThat(process.nextAttemptDueAt()).contains(at("11-14T10:50"));
        assertThat(process.outstandingDebt()).isEqualTo(pln("48.00"));
    }

    // @formatter:off
    @DisplayName(
        """
         Pauses E1.1 given Anna's recovery is running with 1 failed attempt and its next attempt due at 10:50
         on 2026-11-11,
         when collections agent Kate pauses it for a dispute at 14:00 on 2026-11-09,
         then Anna's recovery is paused for dispute with 1 failed attempt (Recovery Paused)
        """
    )
    // @formatter:on
    @Test
    void agentPausesRunningRecoveryKeepingFailedAttempts() {
        // given
        var process = withFailedAttempts(ANNA, 1, "11-08T10:50");

        // when
        var events = process.pause(PauseReason.DISPUTE).by(KATE).at(at("11-09T14:00"));

        // then
        assertThat(events.stream()).containsExactly(
            new RecoveryPaused(ANNA, PauseReason.DISPUTE, KATE, at("11-09T14:00"), 1));
        assertThat(process.status()).isEqualTo(RecoveryStatus.PAUSED);
        assertThat(process.pauseReason()).contains(PauseReason.DISPUTE);
        assertThat(process.failedAttempts()).isEqualTo(1);
    }

    // @formatter:off
    @DisplayName(
        """
         Pauses E2.1 given Ben's recovery is running with 3 failed attempts,
         when Kate pauses it for a payment plan at 11:00 on 2026-11-10,
         then Ben's recovery is paused for payment plan with 3 failed attempts (Recovery Paused)
        """
    )
    // @formatter:on
    @Test
    void agentPausesRecoveryForPaymentPlan() {
        // given
        var process = withFailedAttempts(BEN, 3, "11-01T11:00");

        // when
        var events = process.pause(PauseReason.PAYMENT_PLAN).by(KATE).at(at("11-10T11:00"));

        // then
        assertThat(events.stream()).containsExactly(
            new RecoveryPaused(BEN, PauseReason.PAYMENT_PLAN, KATE, at("11-10T11:00"), 3));
        assertThat(process.pauseReason()).contains(PauseReason.PAYMENT_PLAN);
        assertThat(process.failedAttempts()).isEqualTo(3);
    }

    // @formatter:off
    @DisplayName(
        """
         Pauses E4.3 given Anna's recovery is paused for a dispute,
         when Kate pauses it for a regulatory hold,
         then the pause is rejected and the reason stays dispute (DE-89)
        """
    )
    // @formatter:on
    @Test
    void pausedRecoveryCannotBePausedAgain() {
        // given
        var process = withFailedAttempts(ANNA, 1, "11-08T10:50");
        process.pause(PauseReason.DISPUTE).by(KATE).at(at("11-09T14:00"));

        // when
        var exception = catchThrowable(() -> process.pause(PauseReason.REGULATORY_HOLD).by(KATE).at(at("11-09T15:00")));

        // then
        assertThat(exception).isInstanceOf(RecoveryNotRunningException.class);
        assertThat(process.pauseReason()).contains(PauseReason.DISPUTE);
    }

    // @formatter:off
    @DisplayName(
        """
         Pauses E4.1 given Anna's recovery was exhausted at 10:50 on 2026-11-20,
         when Kate pauses it for a dispute,
         then the pause is rejected and the recovery stays exhausted
        """
    )
    // @formatter:on
    @Test
    void exhaustedRecoveryCannotBePaused() {
        // given
        var process = withFailedAttempts(ANNA, 5, "11-08T10:50");

        // when
        var exception = catchThrowable(() -> process.pause(PauseReason.DISPUTE).by(KATE).at(at("11-21T09:00")));

        // then
        assertThat(exception).isInstanceOf(RecoveryNotRunningException.class);
        assertThat(process.status()).isEqualTo(RecoveryStatus.EXHAUSTED);
    }

    // @formatter:off
    @DisplayName(
        """
         Pauses E3.1 given Anna's recovery was paused at 14:00 on 2026-11-09 with 1 failed attempt, and its next
         attempt would have been due at 10:50 on 2026-11-11,
         when due collections are attempted at 10:50 on 2026-11-12,
         then no attempt is made for Anna and her recovery still has 1 failed attempt
        """
    )
    // @formatter:on
    @Test
    void pausedRecoveryIsNeverDue() {
        // given
        var process = withFailedAttempts(ANNA, 1, "11-08T10:50");
        process.pause(PauseReason.DISPUTE).by(KATE).at(at("11-09T14:00"));

        // when
        var attempt = process.attemptDueAt(at("11-12T10:50"));

        // then
        assertThat(attempt).isEmpty();
        assertThat(process.failedAttempts()).isEqualTo(1);
    }

    // @formatter:off
    @DisplayName(
        """
         Starts E2.2 given Anna's recovery was paused for a dispute with 1 failed attempt,
         when another rental fee of PLN 3.00 is unpaid,
         then no new recovery starts; the fee joins Anna's recovery, which stays paused for dispute
         with 1 failed attempt (DE-88)
        """
    )
    // @formatter:on
    @Test
    void unpaidFeeJoinsPausedRecoveryWhichStaysPaused() {
        // given
        var process = withFailedAttempts(ANNA, 1, "11-08T10:50");
        process.pause(PauseReason.DISPUTE).by(KATE).at(at("11-09T14:00"));

        // when
        var events = process.unpaidFee(pln("8.00")).at(at("11-10T12:00"));

        // then
        assertThat(events.hasOccurredEventOfType(RecoveryStarted.class)).isFalse();
        assertThat(process.status()).isEqualTo(RecoveryStatus.PAUSED);
        assertThat(process.pauseReason()).contains(PauseReason.DISPUTE);
        assertThat(process.failedAttempts()).isEqualTo(1);
        assertThat(process.outstandingDebt()).isEqualTo(pln("8.00"));
    }

    // @formatter:off
    @DisplayName(
        """
         Attempts E5.1 given Anna's recovery is paused for a dispute with 2 failed attempts,
         when Anna pays her outstanding PLN 5.00 in the app at 16:00 on 2026-11-15 (Outstanding Debt Cleared),
         then the recovery succeeded (Recovery Succeeded)
        """
    )
    // @formatter:on
    @Test
    void debtClearedWhilePausedEndsRecoveryAsSucceeded() {
        // given
        var process = withFailedAttempts(ANNA, 2, "11-08T10:50");
        process.pause(PauseReason.DISPUTE).by(KATE).at(at("11-12T09:00"));

        // when
        var events = process.debtCleared(at("11-15T16:00"));

        // then
        assertThat(events.stream()).containsExactly(new RecoverySucceeded(ANNA, at("11-15T16:00"), 2));
        assertThat(process.status()).isEqualTo(RecoveryStatus.SUCCEEDED);
        assertThat(process.pauseReason()).isEmpty();
    }

    // @formatter:off
    @DisplayName(
        """
         Attempts E5.2 given Anna's recovery was exhausted on 2026-11-20,
         when Anna pays her outstanding PLN 5.00 in the app on 2026-11-28 (Outstanding Debt Cleared),
         then the recovery is recorded as succeeded (Recovery Succeeded) (DE-45)
        """
    )
    // @formatter:on
    @Test
    void debtClearedAfterExhaustionRecordsSuccess() {
        // given
        var process = withFailedAttempts(ANNA, 5, "11-08T10:50");

        // when
        var events = process.debtCleared(at("11-28T12:00"));

        // then
        assertThat(events.stream()).containsExactly(new RecoverySucceeded(ANNA, at("11-28T12:00"), 5));
        assertThat(process.status()).isEqualTo(RecoveryStatus.SUCCEEDED);
    }

    // @formatter:off
    @DisplayName(
        """
         Attempts E5.3 given Anna's recovery succeeded on 2026-11-28,
         when Outstanding Debt Cleared for Anna is heard a second time,
         then nothing changes and no second Recovery Succeeded happens (DE-45)
        """
    )
    // @formatter:on
    @Test
    void secondClearanceChangesNothing() {
        // given
        var process = withFailedAttempts(ANNA, 5, "11-08T10:50");
        process.debtCleared(at("11-28T12:00"));

        // when
        var events = process.debtCleared(at("11-28T12:01"));

        // then
        assertThat(events.stream()).isEmpty();
        assertThat(process.status()).isEqualTo(RecoveryStatus.SUCCEEDED);
    }

    // @formatter:off
    @DisplayName(
        """
         Starts E3.1 given Anna's recovery succeeded on 2026-11-11 and she has no outstanding debt,
         when her rental fee of PLN 7.00 for ride S-9 is unpaid at 09:30 on 2026-11-20,
         then a new debt recovery starts for Anna with 0 failed attempts and the first attempt due at 09:30
         on 2026-11-20 (Recovery Started)
        """
    )
    // @formatter:on
    @Test
    void unpaidFeeAfterSucceededRecoveryStartsNewRecovery() {
        // given
        var process = withFailedAttempts(ANNA, 1, "11-08T10:50");
        process.debtCleared(at("11-11T10:50"));

        // when
        var events = process.unpaidFee(pln("7.00")).at(at("11-20T09:30"));

        // then
        assertThat(events.stream()).containsExactly(
            new RecoveryStarted(ANNA, pln("7.00"), at("11-20T09:30"), at("11-20T09:30")));
        assertThat(process.status()).isEqualTo(RecoveryStatus.RUNNING);
        assertThat(process.failedAttempts()).isZero();
        assertThat(process.nextAttemptDueAt()).contains(at("11-20T09:30"));
    }

    // @formatter:off
    @DisplayName(
        """
         Starts E3.2 given Anna's recovery was exhausted on 2026-11-20 with PLN 5.00 still outstanding,
         when her rental fee of PLN 3.00 is unpaid at 12:00 on 2026-11-25 (outstanding debt PLN 8.00),
         then a new debt recovery starts for Anna for her whole outstanding debt of PLN 8.00, with 0 failed attempts
         and the first attempt due at 12:00 on 2026-11-25 (Recovery Started) (DE-88)
        """
    )
    // @formatter:on
    @Test
    void unpaidFeeAfterExhaustedRecoveryStartsNewRecoveryForWholeDebt() {
        // given
        var process = withFailedAttempts(ANNA, 5, "11-08T10:50");

        // when
        var events = process.unpaidFee(pln("8.00")).at(at("11-25T12:00"));

        // then
        assertThat(events.stream()).containsExactly(
            new RecoveryStarted(ANNA, pln("8.00"), at("11-25T12:00"), at("11-25T12:00")));
        assertThat(process.failedAttempts()).isZero();
        assertThat(process.attemptDueAt(at("11-25T12:00")))
            .contains(new CollectionAttempt(ANNA, 1, pln("8.00"), at("11-25T12:00")));
    }

    // @formatter:off
    @DisplayName(
        """
         Resumes E1.1 given Anna's recovery is paused for a dispute with 2 failed attempts,
         when collections agent Kate resumes it at 09:00 on 2026-11-20,
         then Anna's recovery is running again with its next attempt due at 09:00 on 2026-11-20 (Recovery Resumed)
        """
    )
    // @formatter:on
    @Test
    void agentResumesPausedRecoveryWithNextAttemptDueAtOnce() {
        // given
        var process = withFailedAttempts(ANNA, 2, "11-08T10:50");
        process.pause(PauseReason.DISPUTE).by(KATE).at(at("11-12T09:00"));

        // when
        var events = process.resume().by(KATE).at(at("11-20T09:00"));

        // then
        assertThat(events.stream()).containsExactly(
            new RecoveryResumed(ANNA, KATE, at("11-20T09:00"), 2, at("11-20T09:00")));
        assertThat(process.status()).isEqualTo(RecoveryStatus.RUNNING);
        assertThat(process.pauseReason()).isEmpty();
        assertThat(process.nextAttemptDueAt()).contains(at("11-20T09:00"));
    }

    // @formatter:off
    @DisplayName(
        """
         Resumes E2.1 given Anna's recovery was resumed at 09:00 on 2026-11-20 with 2 failed attempts,
         when the attempt at 09:00 on 2026-11-20 fails,
         then her recovery has 3 failed attempts and the next attempt is due at 09:00 on 2026-11-23
        """
    )
    // @formatter:on
    @Test
    void failedAttemptAfterResumeContinuesTheKeptCount() {
        // given
        var process = withFailedAttempts(ANNA, 2, "11-08T10:50");
        process.pause(PauseReason.DISPUTE).by(KATE).at(at("11-12T09:00"));
        process.resume().by(KATE).at(at("11-20T09:00"));
        var attempt = process.attemptDueAt(at("11-20T09:00")).orElseThrow();

        // when
        process.record(attempt).as(CollectionOutcome.failed(pln("5.00")));

        // then
        assertThat(attempt.attemptNumber()).isEqualTo(3);
        assertThat(process.failedAttempts()).isEqualTo(3);
        assertThat(process.nextAttemptDueAt()).contains(at("11-23T09:00"));
    }

    // @formatter:off
    @DisplayName(
        """
         Resumes E2.2 given Ben's recovery was paused for a regulatory hold with 4 failed attempts and Kate resumed it
         at 12:00 on 2026-11-21,
         when the attempt at 12:00 on 2026-11-21 fails,
         then Ben's recovery is exhausted with 5 failed attempts (Recovery Exhausted)
        """
    )
    // @formatter:on
    @Test
    void fifthFailureAfterResumeExhaustsRecovery() {
        // given
        var process = withFailedAttempts(BEN, 4, "11-01T12:00");
        process.pause(PauseReason.REGULATORY_HOLD).by(KATE).at(at("11-11T08:00"));
        process.resume().by(KATE).at(at("11-21T12:00"));
        var attempt = process.attemptDueAt(at("11-21T12:00")).orElseThrow();

        // when
        var events = process.record(attempt).as(CollectionOutcome.failed(pln("5.00")));

        // then
        assertThat(events.stream()).contains(new RecoveryExhausted(BEN, at("11-21T12:00"), pln("5.00")));
        assertThat(process.status()).isEqualTo(RecoveryStatus.EXHAUSTED);
        assertThat(process.failedAttempts()).isEqualTo(5);
    }

    // @formatter:off
    @DisplayName(
        """
         Resumes E3.1 given Anna's recovery is running with 1 failed attempt and its next attempt due at 10:50
         on 2026-11-11,
         when Kate resumes it at 15:00 on 2026-11-09,
         then the resume is rejected and the next attempt stays due at 10:50 on 2026-11-11
        """
    )
    // @formatter:on
    @Test
    void runningRecoveryCannotBeResumed() {
        // given
        var process = withFailedAttempts(ANNA, 1, "11-08T10:50");

        // when
        var exception = catchThrowable(() -> process.resume().by(KATE).at(at("11-09T15:00")));

        // then
        assertThat(exception).isInstanceOf(RecoveryNotPausedException.class);
        assertThat(process.nextAttemptDueAt()).contains(at("11-11T10:50"));
    }

    // @formatter:off
    @DisplayName(
        """
         Resumes E3.2 given Anna's recovery succeeded on 2026-11-15 while it was paused,
         when Kate resumes it on 2026-11-16,
         then the resume is rejected and the recovery stays succeeded
        """
    )
    // @formatter:on
    @Test
    void succeededRecoveryCannotBeResumed() {
        // given
        var process = withFailedAttempts(ANNA, 2, "11-08T10:50");
        process.pause(PauseReason.DISPUTE).by(KATE).at(at("11-12T09:00"));
        process.debtCleared(at("11-15T16:00"));

        // when
        var exception = catchThrowable(() -> process.resume().by(KATE).at(at("11-16T09:00")));

        // then
        assertThat(exception).isInstanceOf(RecoveryNotPausedException.class);
        assertThat(process.status()).isEqualTo(RecoveryStatus.SUCCEEDED);
    }

    // @formatter:off
    @DisplayName(
        """
         Plan D6 (race) given Anna's attempt at 10:50 on 2026-11-11 is in flight and Kate pauses her recovery meanwhile,
         when the attempt's failure is recorded,
         then it doesn't count: the recovery stays paused with 1 failed attempt;
         and a collection recorded after Outstanding Debt Cleared already ended the recovery is only recorded as made
         (Collection Attempted), with no second Recovery Succeeded
        """
    )
    // @formatter:on
    @Test
    void attemptRecordedAfterRecoveryStoppedRunningDoesNotCount() {
        // given
        var paused = withFailedAttempts(ANNA, 1, "11-08T10:50");
        var inFlight = paused.attemptDueAt(at("11-11T10:50")).orElseThrow();
        paused.pause(PauseReason.DISPUTE).by(KATE).at(at("11-11T10:50"));
        var succeeded = withFailedAttempts(BEN, 1, "11-08T10:50");
        var collecting = succeeded.attemptDueAt(at("11-11T10:50")).orElseThrow();
        succeeded.debtCleared(at("11-11T10:50"));

        // when
        var afterPause = paused.record(inFlight).as(CollectionOutcome.failed(pln("5.00")));
        var afterClearance = succeeded.record(collecting).as(CollectionOutcome.collected(pln("0.00")));

        // then
        assertThat(afterPause.stream()).isEmpty();
        assertThat(paused.status()).isEqualTo(RecoveryStatus.PAUSED);
        assertThat(paused.failedAttempts()).isEqualTo(1);
        assertThat(afterClearance.stream()).containsExactly(
            new CollectionAttempted(BEN, 2, pln("5.00"), true, at("11-11T10:50"), null));
        assertThat(succeeded.status()).isEqualTo(RecoveryStatus.SUCCEEDED);
    }

    // @formatter:off
    @DisplayName(
        """
         Decision (late clearance) given Anna's earlier recovery succeeded on 2026-11-11 and a new recovery started
         at 09:30 on 2026-11-20,
         when Outstanding Debt Cleared from 10:50 on 2026-11-11 arrives late,
         then the new recovery keeps running: a clearance from before a recovery started doesn't end it
        """
    )
    // @formatter:on
    @Test
    void clearanceFromBeforeTheRecoveryStartedDoesNotEndIt() {
        // given
        var process = started(ANNA, "5.00", "11-08T10:50");
        process.debtCleared(at("11-11T10:50"));
        process.unpaidFee(pln("7.00")).at(at("11-20T09:30"));

        // when
        var events = process.debtCleared(at("11-11T10:50"));

        // then
        assertThat(events.stream()).isEmpty();
        assertThat(process.status()).isEqualTo(RecoveryStatus.RUNNING);
    }

    private static RecoveryProcess started(RiderId rider, String outstandingDebt, String startedAt) {
        var process = RecoveryProcess.none(rider);
        process.unpaidFee(pln(outstandingDebt)).at(at(startedAt));
        return process;
    }
}
