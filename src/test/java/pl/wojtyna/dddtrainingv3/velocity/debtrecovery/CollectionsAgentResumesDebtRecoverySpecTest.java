package pl.wojtyna.dddtrainingv3.velocity.debtrecovery;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.velocity.common.VelocitySpringTest;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.RecoveryEvent.RecoveryExhausted;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.RecoveryEvent.RecoveryResumed;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.wojtyna.dddtrainingv3.velocity.debtrecovery.UnpaidRideFixture.Fee.PLN_5;
import static pl.wojtyna.dddtrainingv3.velocity.debtrecovery.UnpaidRideFixture.at;

/// Acceptance tests of `docs/specs/collections-agent-resumes-debt-recovery.md`. The collections agent pauses and
/// resumes over REST; attempts after the resume are made by *Attempt Due Collections* (plan D5). E3.2's "succeeded
/// while paused" comes from the rider paying in the app (Settlement → Debt Recovery, asserted with Awaitility).
@VelocitySpringTest
@DisplayName("Spec: a collections agent wants to resume a paused debt recovery")
class CollectionsAgentResumesDebtRecoverySpecTest extends DebtRecoverySpecTestSupport {

    // @formatter:off
    @DisplayName(
        """
         E1.1 given Anna's recovery is paused for a dispute with 2 failed attempts,
         when collections agent Kate resumes it at 09:00 on 2026-11-20,
         then Anna's recovery is running again with its next attempt due at 09:00 on 2026-11-20 (Recovery Resumed)
        """
    )
    // @formatter:on
    @Test
    void e1_1_agent_resumes_paused_recovery_with_next_attempt_due_at_once() {
        // given
        var anna = pausedWithFailedAttempts("anna", 2);

        // when
        var answer = fixture.resume(anna, KATE, "11-20T09:00");

        // then
        assertThat(answer.status()).isEqualTo(200);
        assertThat(answer.recovery().status()).isEqualTo("RUNNING");
        assertThat(answer.recovery().nextAttemptDueAt()).isEqualTo(at("11-20T09:00"));
        assertThat(answer.recovery().failedAttempts()).isEqualTo(2);
        assertThat(answer.recovery().pauseReason()).isNull();
        assertThat(fixture.recoveryEventsOf(anna, RecoveryResumed.class)).singleElement().satisfies(resumed -> {
            assertThat(resumed.resumedBy().value()).isEqualTo(KATE);
            assertThat(resumed.resumedAt()).isEqualTo(at("11-20T09:00"));
            assertThat(resumed.nextAttemptDueAt()).isEqualTo(at("11-20T09:00"));
            assertThat(resumed.failedAttempts()).isEqualTo(2);
        });
    }

    // @formatter:off
    @DisplayName(
        """
         E2.1 given Anna's recovery was resumed at 09:00 on 2026-11-20 with 2 failed attempts,
         when the attempt at 09:00 on 2026-11-20 fails,
         then her recovery has 3 failed attempts and the next attempt is due at 09:00 on 2026-11-23
        """
    )
    // @formatter:on
    @Test
    void e2_1_failed_attempt_after_resume_continues_the_kept_count() {
        // given
        var anna = pausedWithFailedAttempts("anna", 2);
        assertThat(fixture.resume(anna, KATE, "11-20T09:00").status()).isEqualTo(200);

        // when
        fixture.dueCollectionsAttemptedAt("11-20T09:00");

        // then
        var recovery = fixture.awaitRecovery(anna, "RUNNING");
        assertThat(recovery.failedAttempts()).isEqualTo(3);
        assertThat(recovery.nextAttemptDueAt()).isEqualTo(at("11-23T09:00"));
        assertThat(fixture.remindersTo(anna)).hasSize(3);
    }

    // @formatter:off
    @DisplayName(
        """
         E2.2 given Ben's recovery was paused for a regulatory hold with 4 failed attempts and Kate resumed it at 12:00 on 2026-11-21,
         when the attempt at 12:00 on 2026-11-21 fails,
         then Ben's recovery is exhausted with 5 failed attempts (Recovery Exhausted)
        """
    )
    // @formatter:on
    @Test
    void e2_2_fifth_failure_after_resume_exhausts_recovery() {
        // given
        var ben = fixture.riderWithDecliningCard("ben");
        fixture.rideStaysUnpaid(ben, PLN_5, "11-01T11:20", "11-01T11:50", "5.00");
        fixture.failedAttempts(ben, 4, "11-01T12:00");
        assertThat(fixture.pause(ben, KATE, "REGULATORY_HOLD", "11-11T08:00").status()).isEqualTo(200);
        assertThat(fixture.resume(ben, KATE, "11-21T12:00").status()).isEqualTo(200);

        // when
        fixture.dueCollectionsAttemptedAt("11-21T12:00");

        // then
        var recovery = fixture.awaitRecovery(ben, "EXHAUSTED");
        assertThat(recovery.failedAttempts()).isEqualTo(5);
        assertThat(fixture.recoveryEventsOf(ben, RecoveryExhausted.class)).singleElement()
                                                                          .satisfies(exhausted -> assertThat(exhausted.exhaustedAt()).isEqualTo(at("11-21T12:00")));
    }

    // @formatter:off
    @DisplayName(
        """
         E3.1 given Anna's recovery is running with 1 failed attempt and its next attempt due at 10:50 on 2026-11-11,
         when Kate resumes it at 15:00 on 2026-11-09,
         then the resume is rejected and the next attempt stays due at 10:50 on 2026-11-11
        """
    )
    // @formatter:on
    @Test
    void e3_1_running_recovery_cannot_be_resumed() {
        // given
        var anna = fixture.riderWithDecliningCard("anna");
        fixture.rideStaysUnpaid(anna, PLN_5, "11-08T10:10", "11-08T10:40", "5.00");
        fixture.failedAttempts(anna, 1, "11-08T10:50");

        // when
        var answer = fixture.resume(anna, KATE, "11-09T15:00");

        // then
        assertThat(answer.status()).isEqualTo(409);
        assertThat(answer.error()).isEqualTo("RecoveryNotPaused");
        assertThat(fixture.recoveryOf(anna)).hasValueSatisfying(
            recovery -> assertThat(recovery.nextAttemptDueAt()).isEqualTo(at("11-11T10:50")));
    }

    // @formatter:off
    @DisplayName(
        """
         E3.2 given Anna's recovery succeeded on 2026-11-15 while it was paused,
         when Kate resumes it on 2026-11-16,
         then the resume is rejected and the recovery stays succeeded
        """
    )
    // @formatter:on
    @Test
    void e3_2_succeeded_recovery_cannot_be_resumed() {
        // given
        var anna = pausedWithFailedAttempts("anna", 1);
        fixture.riderAddsValidCard(anna);
        fixture.riderPaysOffInApp(anna, "11-15T16:00");
        fixture.awaitRecovery(anna, "SUCCEEDED");

        // when
        var answer = fixture.resume(anna, KATE, "11-16T09:00");

        // then
        assertThat(answer.status()).isEqualTo(409);
        assertThat(answer.error()).isEqualTo("RecoveryNotPaused");
        assertThat(fixture.recoveryOf(anna)).hasValueSatisfying(recovery -> assertThat(recovery.status()).isEqualTo("SUCCEEDED"));
    }

    /// A rider with real PLN 5.00 debt (unpaid at 10:50 on 2026-11-08), `failed` failed attempts from then on, and the
    /// recovery paused for a dispute at 09:00 on 2026-11-12 (after the attempts of 11-08 and 11-11).
    private String pausedWithFailedAttempts(String name, int failed) {
        var rider = fixture.riderWithDecliningCard(name);
        fixture.rideStaysUnpaid(rider, PLN_5, "11-08T10:10", "11-08T10:40", "5.00");
        fixture.failedAttempts(rider, failed, "11-08T10:50");
        assertThat(fixture.pause(rider, KATE, "DISPUTE", "11-12T09:00").status()).isEqualTo(200);
        return rider;
    }
}
