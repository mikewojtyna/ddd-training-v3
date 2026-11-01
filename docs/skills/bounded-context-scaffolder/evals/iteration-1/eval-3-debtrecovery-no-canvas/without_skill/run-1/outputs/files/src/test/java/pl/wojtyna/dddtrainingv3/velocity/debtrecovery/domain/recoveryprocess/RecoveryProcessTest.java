package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recoveryprocess;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@DisplayName("Recovery process examples")
class RecoveryProcessTest {

    private static final RiderId GEORGE = new RiderId("george");

    private static RecoveryProcess activeProcessFor(RiderId rider) {
        return RecoveryProcess.start(new RecoveryProcessId("recovery01"), rider).process();
    }

    // @formatter:off
    @DisplayName(
        """
         given George owes money,
         when recovery starts,
         then the process is active and recovery started
        """
    )
    // @formatter:on
    @Test
    void test0() {
        // when
        var started = RecoveryProcess.start(new RecoveryProcessId("recovery01"), GEORGE);

        // then
        assertThat(started.process().status()).isEqualTo(RecoveryStatus.ACTIVE);
        assertThat(started.events().hasOccurredEventOfType(RecoveryProcessEvent.RecoveryStarted.class)).isTrue();
    }

    // @formatter:off
    @DisplayName(
        """
         given an active recovery process,
         when collection is attempted,
         then collection attempted
        """
    )
    // @formatter:on
    @Test
    void test1() {
        // given
        var process = activeProcessFor(GEORGE);

        // when
        var events = process.attemptCollection();

        // then
        assertThat(events.hasOccurredEventOfType(RecoveryProcessEvent.CollectionAttempted.class)).isTrue();
    }

    // @formatter:off
    @DisplayName(
        """
         given an active recovery process,
         when it is paused because a dispute is under review,
         then the process is paused with that reason
        """
    )
    // @formatter:on
    @Test
    void test2() {
        // given
        var process = activeProcessFor(GEORGE);

        // when
        var events = process.pause().because(PauseReason.DISPUTE_UNDER_REVIEW);

        // then
        assertThat(process.status()).isEqualTo(RecoveryStatus.PAUSED);
        assertThat(process.pauseReason()).contains(PauseReason.DISPUTE_UNDER_REVIEW);
        assertThat(events.hasOccurredEventOfType(RecoveryProcessEvent.RecoveryPaused.class)).isTrue();
    }

    // @formatter:off
    @DisplayName(
        """
         given a paused recovery process,
         when collection is attempted,
         then it is refused
        """
    )
    // @formatter:on
    @Test
    void test3() {
        // given
        var process = activeProcessFor(GEORGE);
        process.pause().because(PauseReason.REGULATORY_HOLD);

        // when
        var thrown = catchThrowable(process::attemptCollection);

        // then
        assertThat(thrown).isInstanceOf(RecoveryProcessPausedException.class);
    }

    // @formatter:off
    @DisplayName(
        """
         given a paused recovery process,
         when it is resumed,
         then the process is active again
        """
    )
    // @formatter:on
    @Test
    void test4() {
        // given
        var process = activeProcessFor(GEORGE);
        process.pause().because(PauseReason.PAYMENT_PLAN_NEGOTIATION);

        // when
        var events = process.resume();

        // then
        assertThat(process.status()).isEqualTo(RecoveryStatus.ACTIVE);
        assertThat(process.pauseReason()).isEmpty();
        assertThat(events.hasOccurredEventOfType(RecoveryProcessEvent.RecoveryResumed.class)).isTrue();
    }

    // @formatter:off
    @DisplayName(
        """
         given an active recovery process,
         when it is resumed,
         then it is refused
        """
    )
    // @formatter:on
    @Test
    void test5() {
        // given
        var process = activeProcessFor(GEORGE);

        // when
        var thrown = catchThrowable(process::resume);

        // then
        assertThat(thrown).isInstanceOf(RecoveryProcessNotPausedException.class);
    }

    // @formatter:off
    @DisplayName(
        """
         given a paused recovery process,
         when Settlement reports George's debt as cleared,
         then recovery succeeded
        """
    )
    // @formatter:on
    @Test
    void test6() {
        // given
        var process = activeProcessFor(GEORGE);
        process.pause().because(PauseReason.DISPUTE_UNDER_REVIEW);

        // when
        var events = process.debtCleared();

        // then
        assertThat(process.status()).isEqualTo(RecoveryStatus.SUCCEEDED);
        assertThat(process.isOpen()).isFalse();
        assertThat(events.hasOccurredEventOfType(RecoveryProcessEvent.RecoverySucceeded.class)).isTrue();
    }

    // @formatter:off
    @DisplayName(
        """
         given an active recovery process,
         when recovery is exhausted,
         then the process is exhausted
        """
    )
    // @formatter:on
    @Test
    void test7() {
        // given
        var process = activeProcessFor(GEORGE);

        // when
        var events = process.exhaust();

        // then
        assertThat(process.status()).isEqualTo(RecoveryStatus.EXHAUSTED);
        assertThat(events.hasOccurredEventOfType(RecoveryProcessEvent.RecoveryExhausted.class)).isTrue();
    }

    // @formatter:off
    @DisplayName(
        """
         given a recovery process that succeeded,
         when it is paused,
         then it is refused
        """
    )
    // @formatter:on
    @Test
    void test8() {
        // given
        var process = activeProcessFor(GEORGE);
        process.debtCleared();

        // when
        var thrown = catchThrowable(() -> process.pause().because(PauseReason.DISPUTE_UNDER_REVIEW));

        // then
        assertThat(thrown).isInstanceOf(RecoveryProcessClosedException.class);
    }
}
