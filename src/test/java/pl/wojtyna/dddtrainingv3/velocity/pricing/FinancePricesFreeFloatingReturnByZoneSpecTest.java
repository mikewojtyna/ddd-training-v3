package pl.wojtyna.dddtrainingv3.velocity.pricing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestClient;
import pl.wojtyna.dddtrainingv3.velocity.common.MutableClock;
import pl.wojtyna.dddtrainingv3.velocity.common.VelocitySpringTest;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.VelocityServiceUrls;
import pl.wojtyna.dddtrainingv3.velocity.pricing.infra.adapters.secondary.cityacl.SimulatedCityZoneMap;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.wojtyna.dddtrainingv3.velocity.pricing.PricingDriver.launchPrices;
import static pl.wojtyna.dddtrainingv3.velocity.pricing.PricingDriver.tariff;

/// Acceptance tests of `docs/specs/finance-prices-free-floating-return-by-zone.md` over `GET /pricing/session-price`,
/// with the simulated City zone map (Old Town Square 52.2297/21.0122 r 150 m, Riverside Park 52.2400/21.0300 r 200 m).
/// Examples on 2026-11-09 (UTC); R4's T-2 on the test day 2030-02-09 and E2.2's zone withdrawal on 2030-02-20, for
/// that day only (plan D8).
@VelocitySpringTest
@DisplayName("Spec: finance wants a free-floating return priced by the zone where the ride ended")
class FinancePricesFreeFloatingReturnByZoneSpecTest {

    private static final String RUN = UUID.randomUUID().toString().substring(0, 8);
    private static final String T2 = "T-2-zone-" + RUN;
    private static final double IN_OLD_TOWN_LAT = 52.2298;
    private static final double IN_OLD_TOWN_LON = 21.0123;
    private static final double OUTSIDE_LAT = 52.2500;
    private static final double OUTSIDE_LON = 21.1000;

    @Autowired
    private MutableClock clock;

    @Autowired
    private RestClient velocityRestClient;

    @Autowired
    private VelocityServiceUrls urls;

    @Autowired
    private SimulatedCityZoneMap city;

    private PricingDriver pricing;

    @BeforeEach
    void setUp() {
        pricing = new PricingDriver(velocityRestClient, urls);
        clock.setTo("2026-11-09T09:00:00Z");
    }

    /// T-2, valid from 12:00 on the test day, with a drop zone fee of PLN 3.00 and every other price as in the launch
    /// tariff, followed by the launch prices again from the next midnight.
    private synchronized void t2IsPublished() {
        if (pricing.tariffs().stream().noneMatch(tariff -> tariff.tariffId().equals(T2))) {
            clock.setTo("2030-02-08T10:00:00Z");
            assertThat(pricing.publish(tariff(T2, "2030-02-09T12:00:00Z", "3.00", "5.00", "7.00", "9.00", "3.00",
                                              "40.00", "150.00")).status()).isEqualTo(201);
            assertThat(pricing.publish(launchPrices("L-2030-02-10-" + RUN, "2030-02-10T00:00:00Z")).status())
                .isEqualTo(201);
        }
    }

    @Test
    @DisplayName("E1.1 Given Anna's ride from 10:00 ending free-floating at (52.2298, 21.0123) at 10:30, When it is priced, Then hourly fees PLN 3.00, drop zone fee PLN 2.00, total PLN 5.00")
    void e1_1_return_in_drop_zone_adds_drop_zone_fee() {
        var price = pricing.price("2026-11-09T10:00:00Z", "2026-11-09T10:30:00Z", "FREE_FLOATING",
                                  IN_OLD_TOWN_LAT, IN_OLD_TOWN_LON);

        assertThat(price.hourlyFees()).isEqualTo("3.00");
        assertThat(price.returnFeeKind()).isEqualTo("DROP_ZONE");
        assertThat(price.returnFee()).isEqualTo("2.00");
        assertThat(price.total()).isEqualTo("5.00");
    }

    @Test
    @DisplayName("E1.2 Given Anna's ride from 10:00 ending free-floating at (52.2298, 21.0123) at 10:10, When it is priced, Then hourly fees PLN 0.00, return fee PLN 2.00, total PLN 2.00")
    void e1_2_quick_return_in_drop_zone_still_pays_drop_zone_fee() {
        var price = pricing.price("2026-11-09T10:00:00Z", "2026-11-09T10:10:00Z", "FREE_FLOATING",
                                  IN_OLD_TOWN_LAT, IN_OLD_TOWN_LON);

        assertThat(price.hourlyFees()).isEqualTo("0.00");
        assertThat(price.returnFee()).isEqualTo("2.00");
        assertThat(price.total()).isEqualTo("2.00");
    }

    @Test
    @DisplayName("E1.3 Given Anna's ride from 10:00 ending free-floating at 10:30 at (52.23104, 21.0122), 149 m from the Old Town Square centre, When it is priced, Then drop zone fee PLN 2.00, total PLN 5.00")
    void e1_3_return_on_drop_zone_border_counts_as_inside() {
        var price = pricing.price("2026-11-09T10:00:00Z", "2026-11-09T10:30:00Z", "FREE_FLOATING", 52.23104, 21.0122);

        assertThat(price.returnFeeKind()).isEqualTo("DROP_ZONE");
        assertThat(price.returnFee()).isEqualTo("2.00");
        assertThat(price.total()).isEqualTo("5.00");
    }

    @Test
    @DisplayName("E2.1 Given Ben's ride from 10:00 ending free-floating at (52.2500, 21.1000) at 10:30, When it is priced, Then hourly fees PLN 3.00, unauthorized zone return fee PLN 40.00, total PLN 43.00")
    void e2_1_return_outside_drop_zones_adds_unauthorized_zone_fee() {
        var price = pricing.price("2026-11-09T10:00:00Z", "2026-11-09T10:30:00Z", "FREE_FLOATING",
                                  OUTSIDE_LAT, OUTSIDE_LON);

        assertThat(price.hourlyFees()).isEqualTo("3.00");
        assertThat(price.returnFeeKind()).isEqualTo("UNAUTHORIZED_ZONE");
        assertThat(price.returnFee()).isEqualTo("40.00");
        assertThat(price.total()).isEqualTo("43.00");
    }

    @Test
    @DisplayName("E2.2 Given Old Town Square was removed from the zone map at 10:20 (test day 2030-02-20), When Anna's ride from 10:00 ending free-floating at (52.2298, 21.0123) at 10:30 is priced, Then return fee PLN 40.00, total PLN 43.00")
    void e2_2_zone_map_valid_at_ride_end_applies() {
        // given
        city.withdraw("Old Town Square", Instant.parse("2030-02-20T10:20:00Z"), Instant.parse("2030-02-21T00:00:00Z"));

        // when
        var price = pricing.price("2030-02-20T10:00:00Z", "2030-02-20T10:30:00Z", "FREE_FLOATING",
                                  IN_OLD_TOWN_LAT, IN_OLD_TOWN_LON);

        // then
        assertThat(price.returnFeeKind()).isEqualTo("UNAUTHORIZED_ZONE");
        assertThat(price.returnFee()).isEqualTo("40.00");
        assertThat(price.total()).isEqualTo("43.00");
    }

    @Test
    @DisplayName("E3.1 Given Ben's ride from 10:00 that ended in a lock failure at (52.2500, 21.1000) at 10:30, When it is priced, Then return fee PLN 0.00, total PLN 3.00")
    void e3_1_lock_failure_outside_drop_zones_pays_no_return_fee() {
        var price = pricing.price("2026-11-09T10:00:00Z", "2026-11-09T10:30:00Z", "LOCK_FAILURE",
                                  OUTSIDE_LAT, OUTSIDE_LON);

        assertThat(price.returnFeeKind()).isEqualTo("NONE");
        assertThat(price.returnFee()).isEqualTo("0.00");
        assertThat(price.total()).isEqualTo("3.00");
    }

    @Test
    @DisplayName("E3.2 Given Anna's ride from 10:00 that ended in a lock failure at (52.2298, 21.0123) at 10:30, When it is priced, Then no drop zone fee, total PLN 3.00")
    void e3_2_lock_failure_in_drop_zone_pays_no_drop_zone_fee() {
        var price = pricing.price("2026-11-09T10:00:00Z", "2026-11-09T10:30:00Z", "LOCK_FAILURE",
                                  IN_OLD_TOWN_LAT, IN_OLD_TOWN_LON);

        assertThat(price.returnFee()).isEqualTo("0.00");
        assertThat(price.total()).isEqualTo("3.00");
    }

    @Test
    @DisplayName("E4.1 Given T-2 valid from 12:00 with a drop zone fee of PLN 3.00 (test day 2030-02-09), When Anna's ride from 11:50 ending free-floating in Old Town Square at 12:30 is priced, Then return fee PLN 2.00, total PLN 5.00")
    void e4_1_return_fee_from_launch_tariff_when_started_before_new_tariff() {
        // given
        t2IsPublished();

        // when
        var price = pricing.price("2030-02-09T11:50:00Z", "2030-02-09T12:30:00Z", "FREE_FLOATING",
                                  IN_OLD_TOWN_LAT, IN_OLD_TOWN_LON);

        // then: the tariff valid before T-2 has the launch prices (its id depends on earlier tests, plan D8)
        assertThat(price.tariffId()).isNotEqualTo(T2);
        assertThat(price.returnFee()).isEqualTo("2.00");
        assertThat(price.total()).isEqualTo("5.00");
    }

    @Test
    @DisplayName("E4.2 Given T-2 valid from 12:00 with a drop zone fee of PLN 3.00 (test day 2030-02-09), When Anna's ride from 12:05 ending free-floating in Old Town Square at 12:40 is priced, Then return fee PLN 3.00, total PLN 6.00")
    void e4_2_return_fee_from_new_tariff_when_started_after_it() {
        // given
        t2IsPublished();

        // when
        var price = pricing.price("2030-02-09T12:05:00Z", "2030-02-09T12:40:00Z", "FREE_FLOATING",
                                  IN_OLD_TOWN_LAT, IN_OLD_TOWN_LON);

        // then
        assertThat(price.tariffId()).isEqualTo(T2);
        assertThat(price.returnFee()).isEqualTo("3.00");
        assertThat(price.total()).isEqualTo("6.00");
    }

    @Test
    @DisplayName("Malformed input: a free-floating ending without a position is answered 400")
    void free_floating_without_position_is_bad_request() {
        assertThat(pricing.priceStatus("startedAt=2026-11-09T10:00:00Z&endedAt=2026-11-09T10:30:00Z&ending=FREE_FLOATING"))
            .isEqualTo(400);
    }
}
