package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelists;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@DisplayName("Pricing examples")
class PriceListsTest {

    private static final CurrencyUnit PLN = CurrencyUnit.of("PLN");
    private static final Instant EIGHT_O_CLOCK = Instant.parse("2026-11-01T08:00:00Z");

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
        var priceLists = priceListsWith(priceList(EIGHT_O_CLOCK, 10, "5.00", "7.00", "9.00", "12.00"));

        // when
        var events = priceLists.price(rideOf(Duration.ofMinutes(8)), noReturnCharges());

        // then
        assertThat(totalFeeIn(events)).isEqualTo(pln("0.00"));
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
        var priceLists = priceListsWith(priceList(EIGHT_O_CLOCK, 10, "5.00", "7.00", "9.00", "12.00"));

        // when
        var events = priceLists.price(rideOf(Duration.ofMinutes(45)), noReturnCharges());

        // then
        assertThat(totalFeeIn(events)).isEqualTo(pln("5.00"));
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
        var priceLists = priceListsWith(priceList(EIGHT_O_CLOCK, 10, "5.00", "7.00", "9.00", "12.00"));

        // when
        var events = priceLists.price(rideOf(Duration.ofHours(3).plusMinutes(20)), noReturnCharges());

        // then
        assertThat(totalFeeIn(events)).isEqualTo(pln("33.00"));
    }

    // @formatter:off
    @DisplayName(
        """
         given price list A (first-hour fee 5.00 PLN) valid from 08:00
         and price list B (first-hour fee 6.00 PLN) valid from 09:00,
         when a ride that started at 08:50 and ended at 09:30 is priced,
         then the total fee is 5.00 PLN
        """
    )
    // @formatter:on
    @Test
    void priceValidAtRideStart() {
        // given (free period 10 minutes for both, agreed in plan review Q3)
        var priceLists = priceListsWith(priceList(EIGHT_O_CLOCK, 10, "5.00", "7.00", "9.00", "12.00"),
                                        priceList(at("09:00"), 10, "6.00", "7.00", "9.00", "12.00"));

        // when
        var events = priceLists.price(ride(at("08:50"), at("09:30")), noReturnCharges());

        // then
        assertThat(totalFeeIn(events)).isEqualTo(pln("5.00"));
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
    void billingHoursStartAfterFreePeriod() {
        // given
        var priceLists = priceListsWith(priceList(EIGHT_O_CLOCK, 10, "5.00", "7.00", "9.00", "12.00"));

        // when
        var events = priceLists.price(rideOf(Duration.ofMinutes(65)), noReturnCharges());

        // then
        assertThat(totalFeeIn(events)).isEqualTo(pln("5.00"));
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
        var priceLists = priceListsWith(priceList(EIGHT_O_CLOCK, 10, "5.00", "7.00", "9.00", "12.00"));

        // when
        var events = priceLists.price(rideOf(Duration.ofMinutes(45)), new ReturnCharges(pln("3.00")));

        // then
        assertThat(totalFeeIn(events)).isEqualTo(pln("8.00"));
    }

    // @formatter:off
    @DisplayName(
        """
         given a price list valid from 08:00,
         when a ride that started at 07:50 and ended at 08:30 is priced,
         then pricing is rejected because no price list was valid at ride start
        """
    )
    // @formatter:on
    @Test
    void noPriceListValidAtRideStart() {
        // given
        var priceLists = priceListsWith(priceList(EIGHT_O_CLOCK, 10, "5.00", "7.00", "9.00", "12.00"));

        // when
        var exception = catchThrowable(() -> priceLists.price(ride(at("07:50"), at("08:30")), noReturnCharges()));

        // then
        assertThat(exception).isInstanceOf(NoPriceListValidAt.class);
    }

    // @formatter:off
    @DisplayName(
        """
         given price list A (first-hour fee 5.00 PLN) valid from 08:00,
         and then price list B (first-hour fee 6.00 PLN) published with the same valid-from 08:00,
         when a ride that started at 08:30 and ended at 09:00 is priced,
         then the total fee is 6.00 PLN
        """
    )
    // @formatter:on
    @Test
    void republishedPriceListReplacesEarlierOne() {
        // given
        var priceLists = priceListsWith(priceList(EIGHT_O_CLOCK, 10, "5.00", "7.00", "9.00", "12.00"),
                                        priceList(EIGHT_O_CLOCK, 10, "6.00", "7.00", "9.00", "12.00"));

        // when
        var events = priceLists.price(ride(at("08:30"), at("09:00")), noReturnCharges());

        // then
        assertThat(totalFeeIn(events)).isEqualTo(pln("6.00"));
    }

    private static PriceLists priceListsWith(PriceList... priceLists) {
        var aggregate = new PriceLists(PriceListsId.VELOCITY);
        for (var priceList : priceLists) {
            aggregate.publish(priceList);
        }
        return aggregate;
    }

    private static PriceList priceList(Instant validFrom, long freePeriodMinutes, String firstHourFee,
                                       String secondHourFee, String thirdHourFee, String subsequentHourlyFee) {
        return new PriceList(validFrom, FreePeriod.ofMinutes(freePeriodMinutes), pln(firstHourFee),
                             pln(secondHourFee), pln(thirdHourFee), pln(subsequentHourlyFee));
    }

    private static Ride rideOf(Duration duration) {
        var start = at("08:30");
        return ride(start, start.plus(duration));
    }

    private static Ride ride(Instant startedAt, Instant endedAt) {
        return new Ride(new RentalSessionId("session-1"), new RiderId("george"), startedAt, endedAt);
    }

    private static Instant at(String time) {
        return Instant.parse("2026-11-01T" + time + ":00Z");
    }

    private static ReturnCharges noReturnCharges() {
        return ReturnCharges.none(PLN);
    }

    private static Money pln(String amount) {
        return Money.parse("PLN " + amount);
    }

    private static Money totalFeeIn(DomainEvents events) {
        assertThat(events.hasOccurredEventOfType(PriceListsEvent.RidePriced.class)).isTrue();
        return events.stream()
                     .filter(PriceListsEvent.RidePriced.class::isInstance)
                     .map(PriceListsEvent.RidePriced.class::cast)
                     .findFirst()
                     .orElseThrow()
                     .totalFee()
                     .amount();
    }
}
