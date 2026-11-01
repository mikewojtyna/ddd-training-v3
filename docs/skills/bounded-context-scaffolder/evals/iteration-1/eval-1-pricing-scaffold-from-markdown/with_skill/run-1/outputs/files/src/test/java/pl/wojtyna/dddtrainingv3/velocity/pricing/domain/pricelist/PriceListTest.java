package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Price list examples")
class PriceListTest {

    private static final CurrencyUnit PLN = CurrencyUnit.of("PLN");
    private static final Instant MORNING = Instant.parse("2026-11-02T07:00:00Z");

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
        var priceList = priceList("PL-1", MORNING, 10, "5.00", "7.00", "9.00", "12.00");

        // when
        var events = priceList.price(ride(MORNING, Duration.ofMinutes(8)), ReturnCharges.none(PLN));

        // then
        assertThat(hasTotalFee(events, "PLN 0.00")).isTrue();
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
        var priceList = priceList("PL-1", MORNING, 10, "5.00", "7.00", "9.00", "12.00");

        // when
        var events = priceList.price(ride(MORNING, Duration.ofMinutes(45)), ReturnCharges.none(PLN));

        // then
        assertThat(hasTotalFee(events, "PLN 5.00")).isTrue();
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
        var priceList = priceList("PL-1", MORNING, 10, "5.00", "7.00", "9.00", "12.00");

        // when
        var events = priceList.price(ride(MORNING, Duration.ofHours(3).plusMinutes(20)), ReturnCharges.none(PLN));

        // then
        assertThat(hasTotalFee(events, "PLN 33.00")).isTrue();
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
        // The canvas gives no free period or other fees for A and B; any free period shorter than 40 minutes
        // yields 5.00 PLN, so the test uses the 10 minutes from the other examples.
        var priceListA = priceList("A", Instant.parse("2026-11-02T08:00:00Z"), 10, "5.00", "7.00", "9.00", "12.00");
        var priceListB = priceList("B", Instant.parse("2026-11-02T09:00:00Z"), 10, "6.00", "7.00", "9.00", "12.00");
        var ride = new Ride(new RentalSessionId("session-1"), new RiderId("george"),
                            Instant.parse("2026-11-02T08:50:00Z"), Instant.parse("2026-11-02T09:30:00Z"));

        // when
        var priceList = PriceList.validAt(List.of(priceListA, priceListB), ride.startedAt()).orElseThrow();
        var events = priceList.price(ride, ReturnCharges.none(PLN));

        // then
        assertThat(hasTotalFee(events, "PLN 5.00")).isTrue();
    }

    private static PriceList priceList(String id, Instant validFrom, long freePeriodMinutes, String firstHourFee,
                                       String secondHourFee, String thirdHourFee, String subsequentHourlyFee) {
        return new PriceList(new PriceListId(id), validFrom, FreePeriod.ofMinutes(freePeriodMinutes),
                             Money.parse("PLN " + firstHourFee), Money.parse("PLN " + secondHourFee),
                             Money.parse("PLN " + thirdHourFee), Money.parse("PLN " + subsequentHourlyFee));
    }

    private static Ride ride(Instant startedAt, Duration duration) {
        return new Ride(new RentalSessionId("session-1"), new RiderId("george"), startedAt, startedAt.plus(duration));
    }

    private static boolean hasTotalFee(DomainEvents events, String expectedFee) {
        return events.hasAtLeastOneEventMatching(
            event -> event instanceof PriceListEvent.RidePriced ridePriced
                     && ridePriced.totalFee().amount().equals(Money.parse(expectedFee)));
    }
}
