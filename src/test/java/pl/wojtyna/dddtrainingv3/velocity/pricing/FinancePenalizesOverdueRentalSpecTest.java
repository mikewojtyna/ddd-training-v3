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

/// Acceptance tests of `docs/specs/finance-penalizes-overdue-rental.md` over `GET /pricing/session-price`. Pricing
/// prices the end time it is given (plan D5): Rentals ends an overdue session at the 12-hour mark, so E1.2's session
/// arrives ending at 20:00:00. Examples on 2026-11-09 (UTC); R4's T-2 on the test day 2030-03-09 (plan D7).
@VelocitySpringTest
@DisplayName("Spec: finance wants a penalty charged for a rental that exceeded the maximum rental time")
class FinancePenalizesOverdueRentalSpecTest {

    private static final String RUN = UUID.randomUUID().toString().substring(0, 8);
    private static final String T2 = "T-2-penalty-" + RUN;

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
        clock.setTo("2026-11-09T21:00:00Z");
    }

    @Test
    @DisplayName("E1.1 Given Ben's ride started at 08:00:00 and ended automatically at the 12-hour mark, 20:00:00, When it is priced, Then hourly fees PLN 96.00 (12 billing hours), penalty PLN 150.00, total PLN 246.00")
    void e1_1_overdue_rental_pays_twelve_hours_plus_penalty() {
        var price = pricing.price("2026-11-09T08:00:00Z", "2026-11-09T20:00:00Z", "MAX_RENTAL_TIME");

        assertThat(price.billingHours()).isEqualTo(12);
        assertThat(price.hourlyFees()).isEqualTo("96.00");
        assertThat(price.penalty()).isEqualTo("150.00");
        assertThat(price.total()).isEqualTo("246.00");
    }

    @Test
    @DisplayName("E1.2 Given Ben's ride started at 08:00:00, the overrun noticed at 20:20 and the ride ended at the 12-hour mark, 20:00:00, When it is priced, Then hourly fees PLN 96.00 (not 105.00), penalty PLN 150.00, total PLN 246.00")
    void e1_2_overdue_rental_noticed_late_is_priced_to_twelve_hour_mark() {
        // given: noticed at 20:20, but Rentals ends the session at started + 12 h (DE-82)
        clock.setTo("2026-11-09T20:20:00Z");

        // when
        var price = pricing.price("2026-11-09T08:00:00Z", "2026-11-09T20:00:00Z", "MAX_RENTAL_TIME");

        // then
        assertThat(price.hourlyFees()).isEqualTo("96.00");
        assertThat(price.penalty()).isEqualTo("150.00");
        assertThat(price.total()).isEqualTo("246.00");
    }

    @Test
    @DisplayName("E2.1 Given Ben's ride from 08:00:00 ended at the 12-hour mark, 20:00:00, the bike last at (52.2500, 21.1000), When it is priced, Then return fee PLN 0.00, total PLN 246.00")
    void e2_1_overdue_rental_pays_no_return_fee() {
        var price = pricing.price("2026-11-09T08:00:00Z", "2026-11-09T20:00:00Z", "MAX_RENTAL_TIME", 52.2500, 21.1000);

        assertThat(price.returnFeeKind()).isEqualTo("NONE");
        assertThat(price.returnFee()).isEqualTo("0.00");
        assertThat(price.total()).isEqualTo("246.00");
    }

    @Test
    @DisplayName("E3.1 Given Anna's ride from 08:00:00 ended with a station return at Central at 20:00:00, When it is priced, Then hourly fees PLN 96.00, no penalty, total PLN 96.00")
    void e3_1_ride_of_exactly_twelve_hours_has_no_penalty() {
        var price = pricing.price("2026-11-09T08:00:00Z", "2026-11-09T20:00:00Z", "STATION");

        assertThat(price.hourlyFees()).isEqualTo("96.00");
        assertThat(price.penalty()).isEqualTo("0.00");
        assertThat(price.total()).isEqualTo("96.00");
    }

    @Test
    @DisplayName("E4.1 Given T-2 valid from 12:00 with a penalty of PLN 200.00 (test day 2030-03-09), When Ben's ride from 08:00:00 ended at the 12-hour mark, 20:00:00, is priced, Then penalty PLN 150.00, total PLN 246.00")
    void e4_1_penalty_from_tariff_valid_at_start() {
        // given
        clock.setTo("2030-03-08T10:00:00Z");
        assertThat(pricing.publish(tariff(T2, "2030-03-09T12:00:00Z", "3.00", "5.00", "7.00", "9.00", "2.00", "40.00",
                                          "200.00")).status()).isEqualTo(201);
        assertThat(pricing.publish(launchPrices("L-2030-03-10-" + RUN, "2030-03-10T00:00:00Z")).status())
            .isEqualTo(201);

        // when
        var price = pricing.price("2030-03-09T08:00:00Z", "2030-03-09T20:00:00Z", "MAX_RENTAL_TIME");

        // then
        assertThat(price.tariffId()).isNotEqualTo(T2);
        assertThat(price.penalty()).isEqualTo("150.00");
        assertThat(price.total()).isEqualTo("246.00");
    }
}
