package pl.wojtyna.dddtrainingv3.velocity.debtrecovery;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.velocity.common.VelocitySpringTest;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.RecoveryEvent.CollectionAttempted;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.RecoveryEvent.RecoveryExhausted;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.RecoveryEvent.RecoverySucceeded;
import pl.wojtyna.dddtrainingv3.velocity.settlement.api.events.OutstandingDebtCleared;

import java.math.BigDecimal;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static pl.wojtyna.dddtrainingv3.velocity.debtrecovery.UnpaidRideFixture.Fee.PLN_5;
import static pl.wojtyna.dddtrainingv3.velocity.debtrecovery.UnpaidRideFixture.at;
import static pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.Pln.pln;

/// Acceptance tests of `docs/specs/collections-attempts-debt-collection.md` (plan D8): real debt made by Settlement
/// (3 declined charges), *Attempt Due Collections* run with the `MutableClock` at the example's time. Reminders are
/// read from `SimulatedPaymentReminders`; Debt Recovery → Settlement collections from the processor's captures and
/// Settlement's Rider Balance; Settlement → Debt Recovery clearances with Awaitility on Debt Recovery's Recovery
/// Process query. A collection succeeds after the rider added a valid card (DE-5).
@VelocitySpringTest
@DisplayName("Spec: Collections wants to remind riders and try to collect their debt on a schedule")
class CollectionsAttemptsDebtCollectionSpecTest extends DebtRecoverySpecTestSupport {

    // @formatter:off
    @DisplayName(
        """
         E1.1 given Anna's recovery started at 10:50 on 2026-11-08 with outstanding debt PLN 5.00 and its first attempt due at once,
         when due collections are attempted at 10:50 on 2026-11-08,
         then Anna gets a payment reminder and collection of PLN 5.00 is tried (Collection Attempted, attempt 1)
        """
    )
    // @formatter:on
    @Test
    void e1_1_due_attempt_reminds_rider_and_asks_for_whole_debt() {
        // given
        var anna = fixture.riderWithDecliningCard("anna");
        fixture.rideStaysUnpaid(anna, PLN_5, "11-08T10:10", "11-08T10:40", "5.00");

        // when
        fixture.dueCollectionsAttemptedAt("11-08T10:50");

        // then
        assertThat(fixture.remindersTo(anna)).singleElement()
                                             .satisfies(reminder -> assertThat(reminder.amountDue()).isEqualByComparingTo("5.00"));
        assertThat(fixture.collectionsCharged(anna)).singleElement().satisfies(capture -> {
            assertThat(capture.amount()).isEqualByComparingTo(new BigDecimal("5.00"));
            assertThat(capture.cardToken()).isEqualTo(fixture.decliningCardOf(anna));
        });
        assertThat(fixture.recoveryEventsOf(anna, CollectionAttempted.class)).singleElement().satisfies(attempted -> {
            assertThat(attempted.attemptNumber()).isEqualTo(1);
            assertThat(attempted.amountAsked()).isEqualTo(pln("5.00"));
            assertThat(attempted.attemptedAt()).isEqualTo(at("11-08T10:50"));
        });
    }

    // @formatter:off
    @DisplayName(
        """
         E1.2 given Anna's recovery is running with 1 failed attempt and its next attempt due at 10:50 on 2026-11-11,
         when due collections are attempted at 10:49 on 2026-11-11,
         then no attempt is made for Anna (72 hours to the minute, DE-90)
        """
    )
    // @formatter:on
    @Test
    void e1_2_no_attempt_a_minute_before_it_is_due() {
        // given
        var anna = fixture.riderWithDecliningCard("anna");
        fixture.rideStaysUnpaid(anna, PLN_5, "11-08T10:10", "11-08T10:40", "5.00");
        fixture.failedAttempts(anna, 1, "11-08T10:50");

        // when
        fixture.dueCollectionsAttemptedAt("11-11T10:49");

        // then
        assertThat(fixture.remindersTo(anna)).hasSize(1);
        assertThat(fixture.collectionsCharged(anna)).hasSize(1);
        assertThat(fixture.recoveryOf(anna)).hasValueSatisfying(recovery -> {
            assertThat(recovery.failedAttempts()).isEqualTo(1);
            assertThat(recovery.nextAttemptDueAt()).isEqualTo(at("11-11T10:50"));
        });
    }

    // @formatter:off
    @DisplayName(
        """
         E2.1 given Anna's recovery has 1 failed attempt and its next attempt is due at 10:50 on 2026-11-11,
         when the attempt at 10:50 on 2026-11-11 collects PLN 5.00 (Outstanding Debt Cleared),
         then Anna was reminded, the recovery succeeded (Collection Attempted, Recovery Succeeded) and no further attempts are due
        """
    )
    // @formatter:on
    @Test
    void e2_1_collected_debt_ends_recovery_as_succeeded() {
        // given
        var anna = fixture.riderWithDecliningCard("anna");
        fixture.rideStaysUnpaid(anna, PLN_5, "11-08T10:10", "11-08T10:40", "5.00");
        fixture.failedAttempts(anna, 1, "11-08T10:50");
        fixture.riderAddsValidCard(anna);

        // when
        fixture.dueCollectionsAttemptedAt("11-11T10:50");

        // then
        assertThat(fixture.remindersTo(anna)).hasSize(2);
        var recovery = fixture.awaitRecovery(anna, "SUCCEEDED");
        assertThat(recovery.nextAttemptDueAt()).isNull();
        assertThat(fixture.recoveryEventsOf(anna, CollectionAttempted.class)).last()
                                                                            .satisfies(attempted -> assertThat(attempted.collected()).isTrue());
        assertThat(fixture.recoveryEventsOf(anna, RecoverySucceeded.class)).singleElement()
                                                                           .satisfies(succeeded -> assertThat(succeeded.succeededAt()).isEqualTo(at("11-11T10:50")));
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(fixture.balanceOf(anna).outstandingDebt()).isEqualTo("0.00"));
        fixture.dueCollectionsAttemptedAt("11-14T10:50");
        assertThat(fixture.remindersTo(anna)).hasSize(2);
    }

    // @formatter:off
    @DisplayName(
        """
         E3.1 given Anna's recovery has 0 failed attempts,
         when the attempt at 10:50 on 2026-11-08 fails,
         then Anna's recovery has 1 failed attempt and the next attempt is due at 10:50 on 2026-11-11 (Collection Attempted)
        """
    )
    // @formatter:on
    @Test
    void e3_1_failed_attempt_counts_and_next_is_due_in_72_hours() {
        // given
        var anna = fixture.riderWithDecliningCard("anna");
        fixture.rideStaysUnpaid(anna, PLN_5, "11-08T10:10", "11-08T10:40", "5.00");

        // when
        fixture.dueCollectionsAttemptedAt("11-08T10:50");

        // then
        var recovery = fixture.awaitRecovery(anna, "RUNNING");
        assertThat(recovery.failedAttempts()).isEqualTo(1);
        assertThat(recovery.nextAttemptDueAt()).isEqualTo(at("11-11T10:50"));
        assertThat(fixture.recoveryEventsOf(anna, CollectionAttempted.class)).singleElement().satisfies(attempted -> {
            assertThat(attempted.collected()).isFalse();
            assertThat(attempted.nextAttemptDueAt()).isEqualTo(at("11-11T10:50"));
        });
        assertThat(fixture.balanceOf(anna).outstandingDebt()).isEqualTo("5.00");
    }

    // @formatter:off
    @DisplayName(
        """
         E4.1 given Anna's recovery has 4 failed attempts (2026-11-08, 11-11, 11-14, 11-17) and PLN 5.00 outstanding,
         when the attempt at 10:50 on 2026-11-20 fails,
         then the recovery is exhausted with 5 failed attempts (Collection Attempted, Recovery Exhausted); Anna stays blocked and still owes PLN 5.00
        """
    )
    // @formatter:on
    @Test
    void e4_1_fifth_failed_attempt_exhausts_recovery() {
        // given
        var anna = fixture.riderWithDecliningCard("anna");
        fixture.rideStaysUnpaid(anna, PLN_5, "11-08T10:10", "11-08T10:40", "5.00");
        fixture.failedAttempts(anna, 4, "11-08T10:50");

        // when
        fixture.dueCollectionsAttemptedAt("11-20T10:50");

        // then
        var recovery = fixture.awaitRecovery(anna, "EXHAUSTED");
        assertThat(recovery.failedAttempts()).isEqualTo(5);
        assertThat(recovery.nextAttemptDueAt()).isNull();
        assertThat(fixture.recoveryEventsOf(anna, RecoveryExhausted.class)).singleElement()
                                                                           .satisfies(exhausted -> assertThat(exhausted.exhaustedAt()).isEqualTo(at("11-20T10:50")));
        staysSo(() -> fixture.accountOf(anna).blocks(), java.util.List.of("OUTSTANDING_DEBT"));
        assertThat(fixture.balanceOf(anna).outstandingDebt()).isEqualTo("5.00");
    }

    // @formatter:off
    @DisplayName(
        """
         E4.2 given Anna's recovery was exhausted at 10:50 on 2026-11-20,
         when due collections are attempted at 10:50 on 2026-11-23,
         then no attempt is made for Anna and she gets no reminder
        """
    )
    // @formatter:on
    @Test
    void e4_2_exhausted_recovery_gets_no_attempt_or_reminder() {
        // given
        var anna = fixture.riderWithDecliningCard("anna");
        fixture.rideStaysUnpaid(anna, PLN_5, "11-08T10:10", "11-08T10:40", "5.00");
        fixture.failedAttempts(anna, 5, "11-08T10:50");
        fixture.awaitRecovery(anna, "EXHAUSTED");

        // when
        fixture.dueCollectionsAttemptedAt("11-23T10:50");

        // then
        assertThat(fixture.remindersTo(anna)).hasSize(5);
        assertThat(fixture.collectionsCharged(anna)).hasSize(5);
        assertThat(fixture.recoveryOf(anna)).hasValueSatisfying(recovery -> assertThat(recovery.status()).isEqualTo("EXHAUSTED"));
    }

    // @formatter:off
    @DisplayName(
        """
         E5.1 given Anna's recovery is paused for a dispute with 2 failed attempts,
         when Anna pays her outstanding PLN 5.00 in the app at 16:00 on 2026-11-15 (Outstanding Debt Cleared),
         then the recovery succeeded (Recovery Succeeded)
        """
    )
    // @formatter:on
    @Test
    void e5_1_debt_cleared_while_paused_ends_recovery_as_succeeded() {
        // given
        var anna = fixture.riderWithDecliningCard("anna");
        fixture.rideStaysUnpaid(anna, PLN_5, "11-08T10:10", "11-08T10:40", "5.00");
        fixture.failedAttempts(anna, 2, "11-08T10:50");
        assertThat(fixture.pause(anna, KATE, "DISPUTE", "11-12T09:00").status()).isEqualTo(200);
        fixture.riderAddsValidCard(anna);

        // when
        fixture.riderPaysOffInApp(anna, "11-15T16:00");

        // then
        fixture.awaitRecovery(anna, "SUCCEEDED");
        assertThat(fixture.recoveryEventsOf(anna, RecoverySucceeded.class)).singleElement().satisfies(succeeded -> {
            assertThat(succeeded.succeededAt()).isEqualTo(at("11-15T16:00"));
            assertThat(succeeded.failedAttempts()).isEqualTo(2);
        });
    }

    // @formatter:off
    @DisplayName(
        """
         E5.2 given Anna's recovery was exhausted on 2026-11-20,
         when Anna pays her outstanding PLN 5.00 in the app on 2026-11-28 (Outstanding Debt Cleared),
         then the recovery is recorded as succeeded (Recovery Succeeded) (DE-45)
        """
    )
    // @formatter:on
    @Test
    void e5_2_debt_cleared_after_exhaustion_records_success() {
        // given
        var anna = fixture.riderWithDecliningCard("anna");
        fixture.rideStaysUnpaid(anna, PLN_5, "11-08T10:10", "11-08T10:40", "5.00");
        fixture.failedAttempts(anna, 5, "11-08T10:50");
        fixture.awaitRecovery(anna, "EXHAUSTED");
        fixture.riderAddsValidCard(anna);

        // when
        fixture.riderPaysOffInApp(anna, "11-28T12:00");

        // then
        var recovery = fixture.awaitRecovery(anna, "SUCCEEDED");
        assertThat(recovery.failedAttempts()).isEqualTo(5);
        assertThat(fixture.recoveryEventsOf(anna, RecoverySucceeded.class)).singleElement()
                                                                           .satisfies(succeeded -> assertThat(succeeded.succeededAt()).isEqualTo(at("11-28T12:00")));
    }

    // @formatter:off
    @DisplayName(
        """
         E5.3 given Anna's recovery succeeded on 2026-11-28,
         when Outstanding Debt Cleared for Anna is heard a second time,
         then nothing changes and no second Recovery Succeeded happens (DE-45)
        """
    )
    // @formatter:on
    @Test
    void e5_3_second_clearance_changes_nothing() {
        // given
        var anna = fixture.riderWithDecliningCard("anna");
        fixture.rideStaysUnpaid(anna, PLN_5, "11-08T10:10", "11-08T10:40", "5.00");
        fixture.riderAddsValidCard(anna);
        fixture.riderPaysOffInApp(anna, "11-28T12:00");
        fixture.awaitRecovery(anna, "SUCCEEDED");
        var before = fixture.recoveryOf(anna).orElseThrow();

        // when
        fixture.settlementPublishes(new OutstandingDebtCleared(anna, at("11-28T12:05")));

        // then
        staysSo(() -> fixture.recoveryOf(anna).orElseThrow(), before);
        assertThat(fixture.recoveryEventsOf(anna, RecoverySucceeded.class)).hasSize(1);
    }
}
