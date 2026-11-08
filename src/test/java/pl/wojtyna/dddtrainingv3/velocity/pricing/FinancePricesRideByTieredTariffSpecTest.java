package pl.wojtyna.dddtrainingv3.velocity.pricing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestClient;
import pl.wojtyna.dddtrainingv3.velocity.common.MutableClock;
import pl.wojtyna.dddtrainingv3.velocity.common.VelocitySpringTest;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.VelocityServiceUrls;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.wojtyna.dddtrainingv3.velocity.pricing.PricingDriver.launchPrices;
import static pl.wojtyna.dddtrainingv3.velocity.pricing.PricingDriver.tariff;

/// Acceptance tests of `docs/specs/finance-prices-ride-by-tiered-tariff.md`, asking Pricing's Open Host Service
/// `GET /pricing/session-price` as Settlement does. Examples on 2026-11-09 (UTC) with the launch tariff; R4's T-2 runs
/// on the test day 2030-01-09 (plan D7). How Settlement asks Pricing when a session ended is tested with
/// `finance-charges-finished-ride`.
@VelocitySpringTest
@DisplayName("Spec: finance wants every finished ride priced by the tiered tariff")
class FinancePricesRideByTieredTariffSpecTest {

    private static final String RUN = UUID.randomUUID().toString().substring(0, 8);
    private static final String T2 = "T-2-tiered-" + RUN;

    @Autowired
    private MutableClock clock;

    @Autowired
    private RestClient velocityRestClient;

    @Autowired
    private VelocityServiceUrls urls;

    private PricingDriver pricing;

    @BeforeEach
    void setUp() {
        pricing = new PricingDriver(velocityRestClient, urls);
        clock.setTo("2026-11-09T09:00:00Z");
    }

    /// T-2, valid from 12:00 on the test day, with hour 1 at PLN 4.00 and every other price as in the launch tariff,
    /// followed by the launch prices again from the next midnight.
    private synchronized void t2IsPublished() {
        if (pricing.tariffs().stream().noneMatch(tariff -> tariff.tariffId().equals(T2))) {
            clock.setTo("2030-01-08T10:00:00Z");
            assertThat(pricing.publish(tariff(T2, "2030-01-09T12:00:00Z", "4.00", "5.00", "7.00", "9.00", "2.00",
                                              "40.00", "150.00")).status()).isEqualTo(201);
            assertThat(pricing.publish(launchPrices("L-2030-01-10-" + RUN, "2030-01-10T00:00:00Z")).status())
                .isEqualTo(201);
        }
    }

    @Test
    @DisplayName("E1.1 Given Anna's ride started at 10:00 and ended with a station return at 10:12, When it is priced, Then hourly fees PLN 0.00, total PLN 0.00")
    void e1_1_ride_of_twelve_minutes_is_free() {
        var price = pricing.price("2026-11-09T10:00:00Z", "2026-11-09T10:12:00Z", "STATION");

        assertThat(price.hourlyFees()).isEqualTo("0.00");
        assertThat(price.total()).isEqualTo("0.00");
    }

    @Test
    @DisplayName("E1.2 Given Anna's ride from 10:00:00 to 10:15:00 (exactly 15 minutes) ending at a station, When it is priced, Then total PLN 0.00")
    void e1_2_ride_of_exactly_fifteen_minutes_is_free() {
        var price = pricing.price("2026-11-09T10:00:00Z", "2026-11-09T10:15:00Z", "STATION");

        assertThat(price.billingHours()).isZero();
        assertThat(price.total()).isEqualTo("0.00");
    }

    @Test
    @DisplayName("E2.1 Given Ben's ride from 10:00:00 to 10:15:01 ending at a station, When it is priced, Then 1 billing hour, total PLN 3.00")
    void e2_1_one_second_after_free_period_starts_first_billing_hour() {
        var price = pricing.price("2026-11-09T10:00:00Z", "2026-11-09T10:15:01Z", "STATION");

        assertThat(price.billingHours()).isEqualTo(1);
        assertThat(price.total()).isEqualTo("3.00");
    }

    @Test
    @DisplayName("E2.2 Given Ben's ride from 10:00:00 to 11:15:00 (exactly 75 minutes) ending at a station, When it is priced, Then 1 billing hour, total PLN 3.00")
    void e2_2_seventy_five_minutes_is_one_billing_hour() {
        var price = pricing.price("2026-11-09T10:00:00Z", "2026-11-09T11:15:00Z", "STATION");

        assertThat(price.billingHours()).isEqualTo(1);
        assertThat(price.total()).isEqualTo("3.00");
    }

    @Test
    @DisplayName("E2.3 Given Ben's ride from 10:00:00 to 11:15:01 ending at a station, When it is priced, Then 2 billing hours, hourly fees PLN 3.00 + 5.00, total PLN 8.00")
    void e2_3_one_second_more_starts_second_billing_hour() {
        var price = pricing.price("2026-11-09T10:00:00Z", "2026-11-09T11:15:01Z", "STATION");

        assertThat(price.billingHours()).isEqualTo(2);
        assertThat(price.hourlyFees()).isEqualTo("8.00");
        assertThat(price.total()).isEqualTo("8.00");
    }

    @Test
    @DisplayName("E3.1 Given Anna's ride from 10:00 to 13:15 ending at a station, When it is priced, Then 3 billing hours, hourly fees PLN 3.00 + 5.00 + 7.00, total PLN 15.00")
    void e3_1_three_billing_hours_cost_fifteen() {
        var price = pricing.price("2026-11-09T10:00:00Z", "2026-11-09T13:15:00Z", "STATION");

        assertThat(price.billingHours()).isEqualTo(3);
        assertThat(price.hourlyFees()).isEqualTo("15.00");
        assertThat(price.total()).isEqualTo("15.00");
    }

    @Test
    @DisplayName("E3.2 Given Anna's ride from 10:00 to 14:20 ending at a station, When it is priced, Then 5 billing hours, hourly fees PLN 3.00 + 5.00 + 7.00 + 9.00 + 9.00, total PLN 33.00")
    void e3_2_fourth_and_further_hours_cost_the_same() {
        var price = pricing.price("2026-11-09T10:00:00Z", "2026-11-09T14:20:00Z", "STATION");

        assertThat(price.billingHours()).isEqualTo(5);
        assertThat(price.hourlyFees()).isEqualTo("33.00");
        assertThat(price.total()).isEqualTo("33.00");
    }

    @Test
    @DisplayName("E4.1 Given T-2 valid from 12:00 with hour 1 at PLN 4.00 (test day 2030-01-09), When Ben's ride from 11:50 to 12:30 ending at a station is priced, Then the launch tariff applies: PLN 3.00")
    void e4_1_ride_started_before_new_tariff_uses_launch_tariff() {
        // given
        t2IsPublished();

        // when
        var price = pricing.price("2030-01-09T11:50:00Z", "2030-01-09T12:30:00Z", "STATION");

        // then
        assertThat(price.tariffId()).isEqualTo("LAUNCH");
        assertThat(price.total()).isEqualTo("3.00");
    }

    @Test
    @DisplayName("E4.2 Given T-2 valid from 12:00 with hour 1 at PLN 4.00 (test day 2030-01-09), When Ben's ride from 12:05 to 12:40 ending at a station is priced, Then T-2 applies: PLN 4.00")
    void e4_2_ride_started_after_new_tariff_uses_it() {
        // given
        t2IsPublished();

        // when
        var price = pricing.price("2030-01-09T12:05:00Z", "2030-01-09T12:40:00Z", "STATION");

        // then
        assertThat(price.tariffId()).isEqualTo(T2);
        assertThat(price.total()).isEqualTo("4.00");
    }

    @Test
    @DisplayName("E5.1 Given Anna's ride from 10:00 to 10:30 ending with a station return, When it is priced, Then return fee PLN 0.00, total PLN 3.00")
    void e5_1_station_return_pays_no_return_fee() {
        var price = pricing.price("2026-11-09T10:00:00Z", "2026-11-09T10:30:00Z", "STATION");

        assertThat(price.returnFee()).isEqualTo("0.00");
        assertThat(price.returnFeeKind()).isEqualTo("NONE");
        assertThat(price.total()).isEqualTo("3.00");
    }

    @Test
    @DisplayName("E5.2 Given Anna's ride from 10:00 to 10:30 ending in a lock failure at (52.2500, 21.1000), When it is priced, Then return fee PLN 0.00, no penalty, total PLN 3.00")
    void e5_2_lock_failure_pays_hourly_fees_only() {
        var price = pricing.price("2026-11-09T10:00:00Z", "2026-11-09T10:30:00Z", "LOCK_FAILURE", 52.2500, 21.1000);

        assertThat(price.returnFee()).isEqualTo("0.00");
        assertThat(price.penalty()).isEqualTo("0.00");
        assertThat(price.total()).isEqualTo("3.00");
    }

    @Test
    @DisplayName("Malformed input: a session that doesn't end after it started, or an unknown ending, is answered 400")
    void malformed_price_question_is_bad_request() {
        assertThat(pricing.priceStatus("startedAt=2026-11-09T10:00:00Z&endedAt=2026-11-09T10:00:00Z&ending=STATION"))
            .isEqualTo(400);
        assertThat(pricing.priceStatus("startedAt=2026-11-09T10:00:00Z&endedAt=2026-11-09T10:30:00Z&ending=TELEPORT"))
            .isEqualTo(400);
    }
}
