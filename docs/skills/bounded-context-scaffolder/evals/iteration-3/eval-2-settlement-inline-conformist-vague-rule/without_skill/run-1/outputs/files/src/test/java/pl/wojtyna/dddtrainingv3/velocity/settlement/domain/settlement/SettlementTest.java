package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement;

import org.joda.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.velocity.payments.api.usecases.ChargeResult;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@DisplayName("Settlement examples")
class SettlementTest {

    private static final Instant PRICED_AT = Instant.parse("2026-11-01T10:00:00Z");
    private static final Money FEE = Money.parse("EUR 4.50");
    private static final ChargeRetryPolicy POLICY = new ChargeRetryPolicy(3, Duration.ofMinutes(15));

    private static Settlement georgesRideSettlement() {
        return Settlement.open(new RideId("ride01"), new RiderId("george"), FEE, PRICED_AT, POLICY);
    }

    private static ChargeResult declined() {
        return new ChargeResult.Failed("insufficient funds");
    }

    // @formatter:off
    @DisplayName(
        """
         given George's ride priced at EUR 4.50,
         when the first charge attempt succeeds,
         then George is charged EUR 4.50 and the settlement is closed
        """
    )
    // @formatter:on
    @Test
    void test0() {
        // given
        var settlement = georgesRideSettlement();

        // when
        var events = settlement.record(new ChargeResult.Succeeded("charge01")).at(PRICED_AT);

        // then
        assertThat(events.hasAtLeastOneEventMatching(
            event -> event instanceof SettlementEvent.RiderCharged charged && charged.amount().equals(FEE)
                     && charged.attemptNumber() == 1)).isTrue();
        assertThat(settlement.isClosed()).isTrue();
        assertThat(settlement.isDueForChargeAttempt(PRICED_AT.plus(Duration.ofDays(1)))).isFalse();
    }

    // @formatter:off
    @DisplayName(
        """
         given George's ride priced at EUR 4.50 and a policy of 3 attempts 15 minutes apart,
         when the first charge attempt fails,
         then a retry is scheduled 15 minutes later
        """
    )
    // @formatter:on
    @Test
    void test1() {
        // given
        var settlement = georgesRideSettlement();

        // when
        var events = settlement.record(declined()).at(PRICED_AT);

        // then
        var retryAt = PRICED_AT.plus(Duration.ofMinutes(15));
        assertThat(events.hasAtLeastOneEventMatching(
            event -> event instanceof SettlementEvent.ChargeAttemptFailed failed && failed.attemptNumber() == 1
                     && failed.nextAttemptAt().equals(retryAt))).isTrue();
        assertThat(settlement.isClosed()).isFalse();
        assertThat(settlement.isDueForChargeAttempt(retryAt.minusSeconds(1))).isFalse();
        assertThat(settlement.isDueForChargeAttempt(retryAt)).isTrue();
    }

    // @formatter:off
    @DisplayName(
        """
         given George's first charge attempt failed,
         when the retry succeeds,
         then George is charged on the second attempt
        """
    )
    // @formatter:on
    @Test
    void test2() {
        // given
        var settlement = georgesRideSettlement();
        settlement.record(declined()).at(PRICED_AT);

        // when
        var events = settlement.record(new ChargeResult.Succeeded("charge02"))
                               .at(PRICED_AT.plus(Duration.ofMinutes(15)));

        // then
        assertThat(events.hasAtLeastOneEventMatching(
            event -> event instanceof SettlementEvent.RiderCharged charged
                     && charged.attemptNumber() == 2)).isTrue();
        assertThat(settlement.isClosed()).isTrue();
    }

    // @formatter:off
    @DisplayName(
        """
         given George's ride priced at EUR 4.50 and a policy of 3 attempts,
         when all 3 charge attempts fail,
         then EUR 4.50 is recorded as outstanding debt and the settlement is closed
        """
    )
    // @formatter:on
    @Test
    void test3() {
        // given
        var settlement = georgesRideSettlement();
        settlement.record(declined()).at(PRICED_AT);
        settlement.record(declined()).at(PRICED_AT.plus(Duration.ofMinutes(15)));

        // when
        var events = settlement.record(declined()).at(PRICED_AT.plus(Duration.ofMinutes(30)));

        // then
        assertThat(events.hasAtLeastOneEventMatching(
            event -> event instanceof SettlementEvent.DebtRecorded debt
                     && debt.debt().equals(new OutstandingDebt(FEE)) && debt.attemptsMade() == 3)).isTrue();
        assertThat(events.hasOccurredEventOfType(SettlementEvent.ChargeAttemptFailed.class)).isFalse();
        assertThat(settlement.isClosed()).isTrue();
        assertThat(settlement.attempts()).hasSize(3);
    }

    // @formatter:off
    @DisplayName(
        """
         given George's ride is already charged,
         when another charge attempt is recorded,
         then it is rejected
        """
    )
    // @formatter:on
    @Test
    void test4() {
        // given
        var settlement = georgesRideSettlement();
        settlement.record(new ChargeResult.Succeeded("charge01")).at(PRICED_AT);

        // when
        var exception = catchThrowable(
            () -> settlement.record(new ChargeResult.Succeeded("charge02")).at(PRICED_AT.plusSeconds(60)));

        // then
        assertThat(exception).isInstanceOf(SettlementAlreadyClosedException.class);
    }

    // @formatter:off
    @DisplayName(
        """
         given George's first charge attempt failed and the retry is due in 15 minutes,
         when a charge attempt is recorded 5 minutes later,
         then it is rejected as not due yet
        """
    )
    // @formatter:on
    @Test
    void test5() {
        // given
        var settlement = georgesRideSettlement();
        settlement.record(declined()).at(PRICED_AT);

        // when
        var exception = catchThrowable(
            () -> settlement.record(declined()).at(PRICED_AT.plus(Duration.ofMinutes(5))));

        // then
        assertThat(exception).isInstanceOf(ChargeAttemptNotDueException.class);
    }

    // @formatter:off
    @DisplayName(
        """
         given George's ride priced at EUR 4.50,
         when the next charge attempt is prepared,
         then Payments is asked to charge George EUR 4.50
        """
    )
    // @formatter:on
    @Test
    void test6() {
        // given
        var settlement = georgesRideSettlement();

        // when
        var command = settlement.nextChargeAttempt();

        // then
        assertThat(command.riderId()).isEqualTo("george");
        assertThat(command.amount()).isEqualTo(FEE);
        assertThat(command.chargeReference()).isEqualTo("settlement-ride01/attempt-1");
    }
}
