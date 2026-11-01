package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist;

import org.joda.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Price list examples")
class PriceListTest {

    private static final Instant RIDE_START = Instant.parse("2026-11-01T08:00:00Z");

    // @formatter:off
    @DisplayName(
        """
         given a price list with a 10 minute free period,
         when a ride of 8 minutes is priced,
         then the total fee is 0.00 PLN
        """
    )
    // @formatter:on
    @Test
    void freeRide() {
        // given
        var priceList = priceList(10, "5.00", "7.00", "9.00", "12.00");

        // when
        var events = priceRideOf(priceList, Duration.ofMinutes(8), ReturnCharges.none());

        // then
        assertThat(totalFeeIs(events, "0.00")).isTrue();
    }

    // @formatter:off
    @DisplayName(
        """
         given a price list with a 10 minute free period and first-hour fee 5.00 PLN,
         when a ride of 45 minutes is priced,
         then the total fee is 5.00 PLN
        """
    )
    // @formatter:on
    @Test
    void firstHour() {
        // given
        var priceList = priceList(10, "5.00", "7.00", "9.00", "12.00");

        // when
        var events = priceRideOf(priceList, Duration.ofMinutes(45), ReturnCharges.none());

        // then
        assertThat(totalFeeIs(events, "5.00")).isTrue();
    }

    // @formatter:off
    @DisplayName(
        """
         given a price list with a 10 minute free period, first-hour fee 5.00 PLN, second-hour fee 7.00 PLN,
         third-hour fee 9.00 PLN and subsequent hourly fee 12.00 PLN,
         when a ride of 3 hours 20 minutes is priced,
         then the total fee is 33.00 PLN
        """
    )
    // @formatter:on
    @Test
    void tieredHours() {
        // given
        var priceList = priceList(10, "5.00", "7.00", "9.00", "12.00");

        // when
        var events = priceRideOf(priceList, Duration.ofHours(3).plusMinutes(20), ReturnCharges.none());

        // then
        assertThat(totalFeeIs(events, "33.00")).isTrue();
    }

    // @formatter:off
    @DisplayName(
        """
         given a price list with a 10 minute free period and first-hour fee 5.00 PLN,
         when a ride of 45 minutes with a 3.00 PLN drop-zone return charge is priced,
         then the total fee is 8.00 PLN
        """
    )
    // @formatter:on
    @Test
    void returnCharges() {
        // given
        var priceList = priceList(10, "5.00", "7.00", "9.00", "12.00");

        // when
        var events = priceRideOf(priceList, Duration.ofMinutes(45), new ReturnCharges(pln("3.00")));

        // then
        assertThat(totalFeeIs(events, "8.00")).isTrue();
    }

    // @formatter:off
    @DisplayName(
        """
         given a price list with a 10 minute free period, first-hour fee 5.00 PLN and second-hour fee 7.00 PLN,
         when a ride of 65 minutes is priced,
         then the total fee is 5.00 PLN
        """
    )
    // @formatter:on
    @Test
    void billingPeriodsStartAfterFreePeriod() {
        // given
        var priceList = priceList(10, "5.00", "7.00", "9.00", "12.00");

        // when
        var events = priceRideOf(priceList, Duration.ofMinutes(65), ReturnCharges.none());

        // then
        assertThat(totalFeeIs(events, "5.00")).isTrue();
    }

    // @formatter:off
    @DisplayName(
        """
         given a price list with a 10 minute free period,
         when a ride of exactly 10 minutes is priced,
         then the total fee is 0.00 PLN
        """
    )
    // @formatter:on
    @Test
    void rideAsLongAsFreePeriodIsFree() {
        // given
        var priceList = priceList(10, "5.00", "7.00", "9.00", "12.00");

        // when
        var events = priceRideOf(priceList, Duration.ofMinutes(10), ReturnCharges.none());

        // then
        assertThat(totalFeeIs(events, "0.00")).isTrue();
    }

    // @formatter:off
    @DisplayName(
        """
         given a price list with a 10 minute free period, first-hour fee 5.00 PLN and second-hour fee 7.00 PLN,
         when a ride of exactly 70 minutes is priced,
         then the total fee is 5.00 PLN
        """
    )
    // @formatter:on
    @Test
    void rideEndingOnHourBoundary() {
        // given
        var priceList = priceList(10, "5.00", "7.00", "9.00", "12.00");

        // when
        var events = priceRideOf(priceList, Duration.ofMinutes(70), ReturnCharges.none());

        // then
        assertThat(totalFeeIs(events, "5.00")).isTrue();
    }

    private static PriceList priceList(int freePeriodMinutes, String firstHourFee, String secondHourFee,
                                       String thirdHourFee, String subsequentHourlyFee) {
        return new PriceList(new PriceListId("PL-1"),
                             Instant.parse("2026-01-01T00:00:00Z"),
                             FreePeriod.ofMinutes(freePeriodMinutes),
                             pln(firstHourFee),
                             pln(secondHourFee),
                             pln(thirdHourFee),
                             pln(subsequentHourlyFee));
    }

    private static DomainEvents priceRideOf(PriceList priceList, Duration rideDuration, ReturnCharges returnCharges) {
        return priceList.priceRide(new RentalSessionId("session-1"),
                                   new RiderId("george"),
                                   new Ride(RIDE_START, RIDE_START.plus(rideDuration)),
                                   returnCharges);
    }

    private static boolean totalFeeIs(DomainEvents events, String amount) {
        var expected = new TotalFee(pln(amount));
        return events.hasAtLeastOneEventMatching(
            event -> event instanceof PriceListEvent.RidePriced ridePriced && ridePriced.totalFee().equals(expected));
    }

    static Money pln(String amount) {
        return Money.of(PriceList.CURRENCY, new BigDecimal(amount));
    }
}
