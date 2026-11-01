package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement;

import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.RideId;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.RiderId;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Settlement examples")
class SettlementTest {

    private static final Money PRICE_12_50_PLN = Money.of(CurrencyUnit.of("PLN"), new BigDecimal("12.50"));

    // @formatter:off
    @DisplayName(
        """
         given a priced ride of 12.50 PLN with 2 failed charge attempts,
         when the 3rd charge attempt fails,
         then an Outstanding Debt of 12.50 PLN is recorded and Debt Recorded is published
        """
    )
    // @formatter:on
    @Test
    void thirdFailedChargeAttemptRecordsOutstandingDebt() {
        // given
        var settlement = Settlement.forPricedRide(new RideId("R-1"), new RiderId("A"), PRICE_12_50_PLN);
        settlement.chargeAttemptFailed();
        settlement.chargeAttemptFailed();

        // when
        var events = settlement.chargeAttemptFailed();

        // then
        assertThat(events.hasAtLeastOneEventMatching(
            event -> event instanceof SettlementEvent.DebtRecorded debtRecorded
                     && debtRecorded.rideId().equals(new RideId("R-1"))
                     && debtRecorded.riderId().equals(new RiderId("A"))
                     && debtRecorded.outstandingDebt().equals(new OutstandingDebt(PRICE_12_50_PLN)))).isTrue();
    }

    // @formatter:off
    @DisplayName(
        """
         given a priced ride of 12.50 PLN with 1 failed charge attempt,
         when the 2nd charge attempt succeeds,
         then the ride is settled and no Outstanding Debt is recorded
        """
    )
    // @formatter:on
    @Test
    void successfulSecondChargeAttemptSettlesRide() {
        // given
        var settlement = Settlement.forPricedRide(new RideId("R-1"), new RiderId("A"), PRICE_12_50_PLN);
        settlement.chargeAttemptFailed();

        // when
        var events = settlement.chargeAttemptSucceeded();

        // then
        assertThat(events.hasAtLeastOneEventMatching(
            event -> event instanceof SettlementEvent.RideSettled settled
                     && settled.rideId().equals(new RideId("R-1")))).isTrue();
        assertThat(events.hasOccurredEventOfType(SettlementEvent.DebtRecorded.class)).isFalse();
    }
}
