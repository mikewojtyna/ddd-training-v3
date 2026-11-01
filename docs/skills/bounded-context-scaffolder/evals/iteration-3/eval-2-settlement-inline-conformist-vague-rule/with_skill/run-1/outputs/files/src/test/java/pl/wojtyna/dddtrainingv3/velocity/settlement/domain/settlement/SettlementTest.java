package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement;

import org.joda.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Settlement examples")
class SettlementTest {

    private static final Money PRICE = Money.parse("PLN 12.50");

    // @formatter:off
    @DisplayName(
        """
         given a priced ride of 12.50 PLN with 2 failed charge attempts,
         when the 3rd charge attempt fails,
         then an Outstanding Debt of 12.50 PLN is recorded
        """
    )
    // @formatter:on
    @Test
    void debtRecordedWhenThirdChargeAttemptFails() {
        // given
        var settlement = pricedRide();
        settlement.chargeAttemptFailed();
        settlement.chargeAttemptFailed();

        // when
        var events = settlement.chargeAttemptFailed();

        // then
        assertThat(events.hasAtLeastOneEventMatching(
            event -> event instanceof SettlementEvent.OutstandingDebtRecorded recorded
                     && recorded.debt().equals(new OutstandingDebt(Money.parse("PLN 12.50")))
                     && recorded.rideId().equals(new RideId("R-1"))
                     && recorded.riderId().equals(new RiderId("rider-1")))).isTrue();
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
    void rideSettledWhenSecondChargeAttemptSucceeds() {
        // given
        var settlement = pricedRide();
        var eventsAfterFirstAttempt = settlement.chargeAttemptFailed();

        // when
        var events = settlement.chargeAttemptSucceeded();

        // then
        assertThat(events.hasOccurredEventOfType(SettlementEvent.RideSettled.class)).isTrue();
        assertThat(events.hasOccurredEventOfType(SettlementEvent.OutstandingDebtRecorded.class)).isFalse();
        assertThat(eventsAfterFirstAttempt.hasOccurredEventOfType(SettlementEvent.OutstandingDebtRecorded.class)).isFalse();
    }

    private static Settlement pricedRide() {
        return new Settlement(new RideId("R-1"), new RiderId("rider-1"), PRICE);
    }
}
