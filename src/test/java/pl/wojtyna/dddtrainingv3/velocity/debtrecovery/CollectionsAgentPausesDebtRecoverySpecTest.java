package pl.wojtyna.dddtrainingv3.velocity.debtrecovery;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.velocity.common.VelocitySpringTest;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.PauseReason;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.RecoveryEvent.RecoveryPaused;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.wojtyna.dddtrainingv3.velocity.debtrecovery.UnpaidRideFixture.Fee.PLN_5;
import static pl.wojtyna.dddtrainingv3.velocity.debtrecovery.UnpaidRideFixture.at;

/// Acceptance tests of `docs/specs/collections-agent-pauses-debt-recovery.md`. The collections agent pauses over REST
/// (`POST /debt-recovery/riders/{riderId}/recovery/pause`) and reads the Rider's Debt Recovery
/// (`GET /debt-recovery/riders/{riderId}/recovery`); failed-attempt counts are reached with real debt and
/// *Attempt Due Collections*.
@VelocitySpringTest
@DisplayName("Spec: a collections agent wants to pause a rider's debt recovery")
class CollectionsAgentPausesDebtRecoverySpecTest extends DebtRecoverySpecTestSupport {

    // @formatter:off
    @DisplayName(
        """
         E1.1 given Anna's recovery is running with 1 failed attempt and its next attempt due at 10:50 on 2026-11-11,
         when collections agent Kate pauses it for a dispute at 14:00 on 2026-11-09,
         then Anna's recovery is paused for dispute with 1 failed attempt (Recovery Paused)
        """
    )
    // @formatter:on
    @Test
    void e1_1_agent_pauses_running_recovery_for_dispute_keeping_failed_attempts() {
        // given
        var anna = fixture.riderWithDecliningCard("anna");
        fixture.rideStaysUnpaid(anna, PLN_5, "11-08T10:10", "11-08T10:40", "5.00");
        fixture.failedAttempts(anna, 1, "11-08T10:50");

        // when
        var answer = fixture.pause(anna, KATE, "DISPUTE", "11-09T14:00");

        // then
        assertThat(answer.status()).isEqualTo(200);
        var recovery = fixture.recoveryOf(anna).orElseThrow();
        assertThat(recovery.status()).isEqualTo("PAUSED");
        assertThat(recovery.pauseReason()).isEqualTo("DISPUTE");
        assertThat(recovery.failedAttempts()).isEqualTo(1);
        assertThat(recovery.nextAttemptDueAt()).isNull();
        assertThat(recovery.outstandingDebt()).isEqualTo("5.00");
        assertThat(fixture.recoveryEventsOf(anna, RecoveryPaused.class)).singleElement().satisfies(paused -> {
            assertThat(paused.reason()).isEqualTo(PauseReason.DISPUTE);
            assertThat(paused.pausedBy().value()).isEqualTo(KATE);
            assertThat(paused.pausedAt()).isEqualTo(at("11-09T14:00"));
            assertThat(paused.failedAttempts()).isEqualTo(1);
        });
    }

    // @formatter:off
    @DisplayName(
        """
         E2.1 given Ben's recovery is running with 3 failed attempts,
         when Kate pauses it for a payment plan at 11:00 on 2026-11-10,
         then Ben's recovery is paused for payment plan with 3 failed attempts (Recovery Paused)
        """
    )
    // @formatter:on
    @Test
    void e2_1_agent_pauses_recovery_for_payment_plan() {
        // given
        var ben = fixture.riderWithDecliningCard("ben");
        fixture.rideStaysUnpaid(ben, PLN_5, "11-01T10:10", "11-01T10:40", "5.00");
        fixture.failedAttempts(ben, 3, "11-01T10:50");

        // when
        var answer = fixture.pause(ben, KATE, "PAYMENT_PLAN", "11-10T11:00");

        // then
        assertThat(answer.status()).isEqualTo(200);
        assertThat(answer.recovery().status()).isEqualTo("PAUSED");
        assertThat(answer.recovery().pauseReason()).isEqualTo("PAYMENT_PLAN");
        assertThat(answer.recovery().failedAttempts()).isEqualTo(3);
        assertThat(fixture.recoveryEventsOf(ben, RecoveryPaused.class)).hasSize(1);
    }

    // @formatter:off
    @DisplayName(
        """
         E2.2 given Ben's recovery is running,
         when Kate pauses it giving the reason "rider is on holiday",
         then the pause is rejected and Ben's recovery keeps running
        """
    )
    // @formatter:on
    @Test
    void e2_2_pause_for_reason_outside_the_list_is_rejected() {
        // given
        var ben = fixture.riderWithDecliningCard("ben");
        fixture.rideStaysUnpaid(ben, PLN_5, "11-08T10:10", "11-08T10:40", "5.00");

        // when
        var answer = fixture.pause(ben, KATE, "rider is on holiday", "11-09T09:00");

        // then
        assertThat(answer.status()).isEqualTo(400);
        assertThat(answer.error()).isEqualTo("UnknownPauseReason");
        assertThat(fixture.recoveryOf(ben)).hasValueSatisfying(recovery -> assertThat(recovery.status()).isEqualTo("RUNNING"));
        assertThat(fixture.recoveryEventsOf(ben, RecoveryPaused.class)).isEmpty();
    }

    // @formatter:off
    @DisplayName(
        """
         E3.1 given Anna's recovery was paused at 14:00 on 2026-11-09 with 1 failed attempt, and its next attempt would have been due at 10:50 on 2026-11-11,
         when due collections are attempted at 10:50 on 2026-11-12,
         then no attempt is made for Anna, she gets no reminder and her recovery still has 1 failed attempt
        """
    )
    // @formatter:on
    @Test
    void e3_1_paused_recovery_gets_no_attempt_or_reminder() {
        // given
        var anna = fixture.riderWithDecliningCard("anna");
        fixture.rideStaysUnpaid(anna, PLN_5, "11-08T10:10", "11-08T10:40", "5.00");
        fixture.failedAttempts(anna, 1, "11-08T10:50");
        assertThat(fixture.pause(anna, KATE, "DISPUTE", "11-09T14:00").status()).isEqualTo(200);

        // when
        fixture.dueCollectionsAttemptedAt("11-12T10:50");

        // then
        assertThat(fixture.remindersTo(anna)).hasSize(1);
        assertThat(fixture.collectionsCharged(anna)).hasSize(1);
        assertThat(fixture.recoveryOf(anna)).hasValueSatisfying(recovery -> {
            assertThat(recovery.status()).isEqualTo("PAUSED");
            assertThat(recovery.failedAttempts()).isEqualTo(1);
        });
    }

    // @formatter:off
    @DisplayName(
        """
         E4.1 given Anna's recovery was exhausted at 10:50 on 2026-11-20,
         when Kate pauses it for a dispute,
         then the pause is rejected and the recovery stays exhausted
        """
    )
    // @formatter:on
    @Test
    void e4_1_exhausted_recovery_cannot_be_paused() {
        // given
        var anna = fixture.riderWithDecliningCard("anna");
        fixture.rideStaysUnpaid(anna, PLN_5, "11-08T10:10", "11-08T10:40", "5.00");
        fixture.failedAttempts(anna, 5, "11-08T10:50");
        fixture.awaitRecovery(anna, "EXHAUSTED");

        // when
        var answer = fixture.pause(anna, KATE, "DISPUTE", "11-21T09:00");

        // then
        assertThat(answer.status()).isEqualTo(409);
        assertThat(answer.error()).isEqualTo("RecoveryNotRunning");
        assertThat(fixture.recoveryOf(anna)).hasValueSatisfying(recovery -> assertThat(recovery.status()).isEqualTo("EXHAUSTED"));
    }

    // @formatter:off
    @DisplayName(
        """
         E4.2 given rider Ben has no debt recovery,
         when Kate pauses Ben's recovery for a regulatory hold,
         then the pause is rejected
        """
    )
    // @formatter:on
    @Test
    void e4_2_rider_without_recovery_cannot_be_paused() {
        // given
        var ben = "ben-" + UUID.randomUUID();

        // when
        var answer = fixture.pause(ben, KATE, "REGULATORY_HOLD", "11-09T09:00");

        // then
        assertThat(answer.status()).isEqualTo(409);
        assertThat(answer.error()).isEqualTo("RecoveryNotRunning");
        assertThat(fixture.recoveryOf(ben)).isEmpty();
    }

    // @formatter:off
    @DisplayName(
        """
         E4.3 given Anna's recovery is paused for a dispute,
         when Kate pauses it for a regulatory hold,
         then the pause is rejected and the reason stays dispute (DE-89)
        """
    )
    // @formatter:on
    @Test
    void e4_3_paused_recovery_cannot_be_paused_again() {
        // given
        var anna = fixture.riderWithDecliningCard("anna");
        fixture.rideStaysUnpaid(anna, PLN_5, "11-08T10:10", "11-08T10:40", "5.00");
        assertThat(fixture.pause(anna, KATE, "DISPUTE", "11-09T14:00").status()).isEqualTo(200);

        // when
        var answer = fixture.pause(anna, KATE, "REGULATORY_HOLD", "11-09T15:00");

        // then
        assertThat(answer.status()).isEqualTo(409);
        assertThat(answer.error()).isEqualTo("RecoveryNotRunning");
        assertThat(fixture.recoveryOf(anna)).hasValueSatisfying(recovery -> assertThat(recovery.pauseReason()).isEqualTo("DISPUTE"));
    }
}
