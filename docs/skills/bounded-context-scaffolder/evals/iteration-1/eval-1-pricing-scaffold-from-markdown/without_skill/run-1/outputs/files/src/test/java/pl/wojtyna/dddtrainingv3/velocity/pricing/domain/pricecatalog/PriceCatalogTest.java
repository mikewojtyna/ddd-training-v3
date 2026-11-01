package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricecatalog;

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
class PriceCatalogTest {

    private static final CurrencyUnit PLN = CurrencyUnit.of("PLN");
    private static final Instant MIDNIGHT = Instant.parse("2026-11-01T00:00:00Z");

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
        var catalog = catalogWith(priceList("A", MIDNIGHT, 10, "5.00", "7.00", "9.00", "12.00"));

        // when
        var events = catalog.price(ride(at("08:00"), Duration.ofMinutes(8))).withReturnCharges(noReturnCharges());

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
        var catalog = catalogWith(priceList("A", MIDNIGHT, 10, "5.00", "7.00", "9.00", "12.00"));

        // when
        var events = catalog.price(ride(at("08:00"), Duration.ofMinutes(45))).withReturnCharges(noReturnCharges());

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
        var catalog = catalogWith(priceList("A", MIDNIGHT, 10, "5.00", "7.00", "9.00", "12.00"));

        // when
        var events = catalog.price(ride(at("08:00"), Duration.ofHours(3).plusMinutes(20)))
                            .withReturnCharges(noReturnCharges());

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
        // given
        var catalog = catalogWith(priceList("A", at("08:00"), 10, "5.00", "7.00", "9.00", "12.00"),
                                  priceList("B", at("09:00"), 10, "6.00", "7.00", "9.00", "12.00"));

        // when
        var ride = new Ride(new RentalSessionId("session01"), new RiderId("george"), at("08:50"), at("09:30"));
        var events = catalog.price(ride).withReturnCharges(noReturnCharges());

        // then
        assertThat(totalFeeIn(events)).isEqualTo(pln("5.00"));
        assertThat(ridePricedIn(events).appliedPriceList()).isEqualTo(new PriceListId("A"));
    }

    // @formatter:off
    @DisplayName(
        """
         given a price list with a 10 minute free period and first-hour fee 5.00 PLN,
         when a ride of 45 minutes with 20.00 PLN return charges is priced,
         then the total fee is 25.00 PLN
        """
    )
    // @formatter:on
    @Test
    void returnChargesAreAddedToTotalFee() {
        // given
        var catalog = catalogWith(priceList("A", MIDNIGHT, 10, "5.00", "7.00", "9.00", "12.00"));

        // when
        var events = catalog.price(ride(at("08:00"), Duration.ofMinutes(45)))
                            .withReturnCharges(new ReturnCharges(pln("20.00")));

        // then
        assertThat(totalFeeIn(events)).isEqualTo(pln("25.00"));
    }

    // @formatter:off
    @DisplayName(
        """
         given a price list valid from 09:00 only,
         when a ride that started at 08:50 is priced,
         then the ride is not priced
        """
    )
    // @formatter:on
    @Test
    void noPriceListValidAtRideStart() {
        // given
        var catalog = catalogWith(priceList("B", at("09:00"), 10, "6.00", "7.00", "9.00", "12.00"));

        // when
        var exception = catchThrowable(() -> catalog.price(ride(at("08:50"), Duration.ofMinutes(40)))
                                                    .withReturnCharges(noReturnCharges()));

        // then
        assertThat(exception).isInstanceOf(NoValidPriceListException.class);
    }

    // @formatter:off
    @DisplayName(
        """
         given a price list valid from 08:00,
         when another price list valid from 08:00 is published,
         then it is rejected
        """
    )
    // @formatter:on
    @Test
    void twoPriceListsCannotBecomeValidAtTheSameMoment() {
        // given
        var catalog = catalogWith(priceList("A", at("08:00"), 10, "5.00", "7.00", "9.00", "12.00"));

        // when
        var exception = catchThrowable(
            () -> catalog.publish(priceList("B", at("08:00"), 10, "6.00", "7.00", "9.00", "12.00")));

        // then
        assertThat(exception).isInstanceOf(PriceListAlreadyPublishedException.class);
    }

    // @formatter:off
    @DisplayName(
        """
         given an empty price catalog,
         when a price list is published,
         then price list published event occurs
        """
    )
    // @formatter:on
    @Test
    void publishPriceList() {
        // given
        var catalog = new PriceCatalog(PriceCatalogId.VELOCITY);

        // when
        var events = catalog.publish(priceList("A", at("08:00"), 10, "5.00", "7.00", "9.00", "12.00"));

        // then
        assertThat(events.hasOccurredEventOfType(PricingEvent.PriceListPublished.class)).isTrue();
    }

    // @formatter:off
    @DisplayName(
        """
         given a price list with a 10 minute free period, first-hour fee 5.00 PLN and second-hour fee 7.00 PLN,
         when the rider asks for an estimate of a 90 minute ride,
         then the estimated fee is 12.00 PLN
        """
    )
    // @formatter:on
    @Test
    void estimateFee() {
        // given
        var catalog = catalogWith(priceList("A", MIDNIGHT, 10, "5.00", "7.00", "9.00", "12.00"));

        // when
        var fee = catalog.estimateFee(Duration.ofMinutes(90), at("08:00"));

        // then
        assertThat(fee).isEqualTo(pln("12.00"));
    }

    private static PriceCatalog catalogWith(PriceList... priceLists) {
        var catalog = new PriceCatalog(PriceCatalogId.VELOCITY);
        for (var priceList : priceLists) {
            catalog.publish(priceList);
        }
        return catalog;
    }

    private static PriceList priceList(String id, Instant validFrom, long freeMinutes, String firstHour,
                                       String secondHour, String thirdHour, String subsequentHour) {
        return new PriceList(new PriceListId(id), validFrom, FreePeriod.ofMinutes(freeMinutes),
                             new HourlyFees(pln(firstHour), pln(secondHour), pln(thirdHour), pln(subsequentHour)));
    }

    private static Ride ride(Instant start, Duration duration) {
        return new Ride(new RentalSessionId("session01"), new RiderId("george"), start, start.plus(duration));
    }

    private static Instant at(String time) {
        return Instant.parse("2026-11-01T" + time + ":00Z");
    }

    private static Money pln(String amount) {
        return Money.parse("PLN " + amount);
    }

    private static ReturnCharges noReturnCharges() {
        return ReturnCharges.none(PLN);
    }

    private static PricingEvent.RidePriced ridePricedIn(DomainEvents events) {
        return events.stream()
                     .filter(PricingEvent.RidePriced.class::isInstance)
                     .map(PricingEvent.RidePriced.class::cast)
                     .findFirst()
                     .orElseThrow();
    }

    private static Money totalFeeIn(DomainEvents events) {
        return ridePricedIn(events).totalFee();
    }
}
