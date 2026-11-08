package pl.wojtyna.dddtrainingv3.velocity.debtrecovery;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.velocity.common.VelocitySpringTest;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.RecoveryEvent.RecoveryStarted;
import pl.wojtyna.dddtrainingv3.velocity.settlement.api.events.RentalFeeUnpaid;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.wojtyna.dddtrainingv3.velocity.debtrecovery.UnpaidRideFixture.Fee.PLN_3;
import static pl.wojtyna.dddtrainingv3.velocity.debtrecovery.UnpaidRideFixture.Fee.PLN_43;
import static pl.wojtyna.dddtrainingv3.velocity.debtrecovery.UnpaidRideFixture.Fee.PLN_5;
import static pl.wojtyna.dddtrainingv3.velocity.debtrecovery.UnpaidRideFixture.at;
import static pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.Pln.pln;

/// Acceptance tests of `docs/specs/collections-starts-debt-recovery.md`. Real debt: Settlement makes the fee unpaid
/// after 3 declined charges and publishes Rental Fee Unpaid, which Debt Recovery hears asynchronously (asserted with
/// Awaitility on `GET /debt-recovery/riders/{riderId}/recovery`). E3.1's PLN 7.00 can't be produced by the launch
/// tariff, so that Rental Fee Unpaid is published as Settlement would (plan D6).
@VelocitySpringTest
@DisplayName("Spec: Collections wants debt recovery to start as soon as a rental fee stays unpaid")
class CollectionsStartsDebtRecoverySpecTest extends DebtRecoverySpecTestSupport {

    // @formatter:off
    @DisplayName(
        """
         E1.1 given rider Anna has no debt recovery and no outstanding debt,
         when her rental fee of PLN 5.00 for ride S-1 is unpaid at 10:50 on 2026-11-08 (Rental Fee Unpaid, outstanding debt PLN 5.00),
         then a debt recovery starts for Anna with 0 failed attempts and the first collection attempt due at 10:50 on 2026-11-08 (Recovery Started)
        """
    )
    // @formatter:on
    @Test
    void e1_1_unpaid_fee_starts_recovery_with_first_attempt_due_at_once() {
        // given
        var anna = fixture.riderWithDecliningCard("anna");
        assertThat(fixture.recoveryOf(anna)).isEmpty();

        // when
        fixture.rideStaysUnpaid(anna, PLN_5, "11-08T10:10", "11-08T10:40", "5.00");

        // then
        var recovery = fixture.awaitRecovery(anna, "RUNNING");
        assertThat(recovery.failedAttempts()).isZero();
        assertThat(recovery.nextAttemptDueAt()).isEqualTo(at("11-08T10:50"));
        assertThat(recovery.outstandingDebt()).isEqualTo("5.00");
        assertThat(fixture.recoveryEventsOf(anna, RecoveryStarted.class)).singleElement().satisfies(started -> {
            assertThat(started.startedAt()).isEqualTo(at("11-08T10:50"));
            assertThat(started.firstAttemptDueAt()).isEqualTo(at("11-08T10:50"));
            assertThat(started.outstandingDebt()).isEqualTo(pln("5.00"));
        });
    }

    // @formatter:off
    @DisplayName(
        """
         E2.1 given Anna's recovery is running with 2 failed attempts, her outstanding debt is PLN 5.00 and her next attempt is due at 10:50 on 2026-11-14,
         when her rental fee of PLN 43.00 for ride S-7 is unpaid at 18:05 on 2026-11-12 (Rental Fee Unpaid, outstanding debt PLN 48.00),
         then no new recovery starts and Anna's recovery keeps 2 failed attempts, its next attempt still due at 10:50 on 2026-11-14, and now pursues PLN 48.00
        """
    )
    // @formatter:on
    @Test
    void e2_1_further_unpaid_fee_joins_running_recovery_keeping_count_and_due_time() {
        // given
        var anna = fixture.riderWithDecliningCard("anna");
        fixture.rideStaysUnpaid(anna, PLN_5, "11-08T10:10", "11-08T10:40", "5.00");
        fixture.failedAttempts(anna, 2, "11-08T10:50");
        assertThat(fixture.recoveryOf(anna)).hasValueSatisfying(recovery -> {
            assertThat(recovery.failedAttempts()).isEqualTo(2);
            assertThat(recovery.nextAttemptDueAt()).isEqualTo(at("11-14T10:50"));
        });

        // when
        fixture.rideStaysUnpaid(anna, PLN_43, "11-12T17:25", "11-12T17:55", "48.00");

        // then
        var recovery = fixture.awaitRecovery(anna, "RUNNING");
        assertThat(recovery.failedAttempts()).isEqualTo(2);
        assertThat(recovery.nextAttemptDueAt()).isEqualTo(at("11-14T10:50"));
        assertThat(recovery.outstandingDebt()).isEqualTo("48.00");
        assertThat(fixture.recoveryEventsOf(anna, RecoveryStarted.class)).hasSize(1);
    }

    // @formatter:off
    @DisplayName(
        """
         E2.2 given Anna's recovery was paused for a dispute with 1 failed attempt,
         when another rental fee of PLN 3.00 is unpaid,
         then no new recovery starts; the fee joins Anna's recovery, which stays paused for dispute with 1 failed attempt (DE-88)
        """
    )
    // @formatter:on
    @Test
    void e2_2_unpaid_fee_joins_paused_recovery_which_stays_paused() {
        // given
        var anna = fixture.riderWithDecliningCard("anna");
        fixture.rideStaysUnpaid(anna, PLN_5, "11-08T10:10", "11-08T10:40", "5.00");
        fixture.failedAttempts(anna, 1, "11-08T10:50");
        assertThat(fixture.pause(anna, KATE, "DISPUTE", "11-09T14:00").status()).isEqualTo(200);

        // when
        fixture.rideStaysUnpaid(anna, PLN_3, "11-10T11:20", "11-10T11:50", "8.00");

        // then
        var recovery = fixture.awaitRecovery(anna, "PAUSED");
        assertThat(recovery.pauseReason()).isEqualTo("DISPUTE");
        assertThat(recovery.failedAttempts()).isEqualTo(1);
        assertThat(recovery.outstandingDebt()).isEqualTo("8.00");
        assertThat(fixture.recoveryEventsOf(anna, RecoveryStarted.class)).hasSize(1);
    }

    // @formatter:off
    @DisplayName(
        """
         E3.1 given Anna's recovery succeeded on 2026-11-11 and she has no outstanding debt,
         when her rental fee of PLN 7.00 for ride S-9 is unpaid at 09:30 on 2026-11-20,
         then a new debt recovery starts for Anna with 0 failed attempts and the first attempt due at 09:30 on 2026-11-20 (Recovery Started)
        """
    )
    // @formatter:on
    @Test
    void e3_1_unpaid_fee_after_succeeded_recovery_starts_new_recovery() {
        // given
        var anna = fixture.riderWithDecliningCard("anna");
        fixture.rideStaysUnpaid(anna, PLN_5, "11-08T10:10", "11-08T10:40", "5.00");
        fixture.failedAttempts(anna, 1, "11-08T10:50");
        fixture.riderAddsValidCard(anna);
        fixture.dueCollectionsAttemptedAt("11-11T10:50");
        fixture.awaitRecovery(anna, "SUCCEEDED");
        staysSo(() -> fixture.balanceOf(anna).outstandingDebt(), "0.00");

        // when
        fixture.settlementPublishes(new RentalFeeUnpaid(anna, "S-9-" + fixture.suffix(), new BigDecimal("7.00"),
                                                        new BigDecimal("7.00"), at("11-20T09:30")));

        // then
        var recovery = fixture.awaitRecovery(anna, "RUNNING");
        assertThat(recovery.failedAttempts()).isZero();
        assertThat(recovery.nextAttemptDueAt()).isEqualTo(at("11-20T09:30"));
        assertThat(recovery.outstandingDebt()).isEqualTo("7.00");
        assertThat(fixture.recoveryEventsOf(anna, RecoveryStarted.class)).hasSize(2);
    }

    // @formatter:off
    @DisplayName(
        """
         E3.2 given Anna's recovery was exhausted on 2026-11-20 with PLN 5.00 still outstanding,
         when her rental fee of PLN 3.00 is unpaid at 12:00 on 2026-11-25 (outstanding debt PLN 8.00),
         then a new debt recovery starts for Anna for her whole outstanding debt of PLN 8.00, with 0 failed attempts
         and the first attempt due at 12:00 on 2026-11-25 (Recovery Started) (DE-88)
        """
    )
    // @formatter:on
    @Test
    void e3_2_unpaid_fee_after_exhausted_recovery_starts_new_recovery_for_whole_debt() {
        // given
        var anna = fixture.riderWithDecliningCard("anna");
        fixture.rideStaysUnpaid(anna, PLN_5, "11-08T10:10", "11-08T10:40", "5.00");
        fixture.failedAttempts(anna, 5, "11-08T10:50");
        fixture.awaitRecovery(anna, "EXHAUSTED");

        // when
        fixture.rideStaysUnpaid(anna, PLN_3, "11-25T11:20", "11-25T11:50", "8.00");

        // then
        var recovery = fixture.awaitRecovery(anna, "RUNNING");
        assertThat(recovery.failedAttempts()).isZero();
        assertThat(recovery.nextAttemptDueAt()).isEqualTo(at("11-25T12:00"));
        assertThat(recovery.outstandingDebt()).isEqualTo("8.00");
        assertThat(fixture.recoveryEventsOf(anna, RecoveryStarted.class)).hasSize(2);
    }
}
