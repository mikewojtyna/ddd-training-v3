package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist.PriceListTest.pln;

@DisplayName("Price lists examples")
class PriceListsTest {

    private final PriceList priceListA = priceList("A", "2026-11-01T08:00:00Z", "5.00");
    private final PriceList priceListB = priceList("B", "2026-11-01T09:00:00Z", "6.00");

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
        var priceLists = PriceLists.of(priceListA, priceListB);
        var ride = new Ride(Instant.parse("2026-11-01T08:50:00Z"), Instant.parse("2026-11-01T09:30:00Z"));

        // when
        var events = priceLists.validAt(ride.startedAt()).priceRide(sessionId(), riderId(), ride, ReturnCharges.none());

        // then
        assertThat(ridePricedWith(events, priceListA.id(), "5.00")).isTrue();
    }

    // @formatter:off
    @DisplayName(
        """
         given price list A (first-hour fee 5.00 PLN) valid from 08:00
         and price list B (first-hour fee 6.00 PLN) valid from 09:00,
         when a ride that started at 09:00 and ended at 09:40 is priced,
         then price list B is used and the total fee is 6.00 PLN
        """
    )
    // @formatter:on
    @Test
    void rideStartingWhenNewPriceListBecomesValid() {
        // given
        var priceLists = PriceLists.of(priceListA, priceListB);
        var ride = new Ride(Instant.parse("2026-11-01T09:00:00Z"), Instant.parse("2026-11-01T09:40:00Z"));

        // when
        var events = priceLists.validAt(ride.startedAt()).priceRide(sessionId(), riderId(), ride, ReturnCharges.none());

        // then
        assertThat(ridePricedWith(events, priceListB.id(), "6.00")).isTrue();
    }

    // @formatter:off
    @DisplayName(
        """
         given price list A valid from 08:00 and price list B valid from 09:00,
         when a ride that started at 07:30 is priced,
         then pricing is rejected because no price list is valid at the ride start
        """
    )
    // @formatter:on
    @Test
    void noPriceListValidAtRideStart() {
        // given
        var priceLists = PriceLists.of(priceListA, priceListB);
        var rideStart = Instant.parse("2026-11-01T07:30:00Z");

        // when
        var exception = catchThrowable(() -> priceLists.validAt(rideStart));

        // then
        assertThat(exception).isInstanceOf(NoPriceListValidAt.class);
    }

    private static PriceList priceList(String id, String validFrom, String firstHourFee) {
        return new PriceList(new PriceListId(id),
                             Instant.parse(validFrom),
                             FreePeriod.ofMinutes(10),
                             pln(firstHourFee),
                             pln("7.00"),
                             pln("9.00"),
                             pln("12.00"));
    }

    private static boolean ridePricedWith(DomainEvents events, PriceListId priceListId, String totalFee) {
        var expected = new TotalFee(pln(totalFee));
        return events.hasAtLeastOneEventMatching(
            event -> event instanceof PriceListEvent.RidePriced ridePriced
                && ridePriced.priceListId().equals(priceListId)
                && ridePriced.totalFee().equals(expected));
    }

    private static RentalSessionId sessionId() {
        return new RentalSessionId("session-1");
    }

    private static RiderId riderId() {
        return new RiderId("george");
    }
}
