package pl.wojtyna.dddtrainingv3.velocity.pricing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestClient;
import pl.wojtyna.dddtrainingv3.velocity.common.MutableClock;
import pl.wojtyna.dddtrainingv3.velocity.common.VelocitySpringTest;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.VelocityServiceUrls;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.wojtyna.dddtrainingv3.velocity.pricing.PricingDriver.launchPrices;
import static pl.wojtyna.dddtrainingv3.velocity.pricing.PricingDriver.tariff;

/// Acceptance tests of `docs/specs/pricing-manager-publishes-tariff.md`, through Pricing's REST resources.
///
/// The application context is shared by every Spring test, so tariffs published here would apply to every later
/// ride (plan D8): examples that publish T-2 run on test days in 2030 instead of 2026-12-01, keeping the time of day,
/// and every such T-2 is followed by "launch prices again" from the next midnight. The shared T-2 (valid from
/// 2030-04-01 00:00) is published once for E2.1, E2.2 and E5.1; E1.1 publishes its own on 2030-04-11, so it can
/// observe a successful publication whatever the order of the tests.
@VelocitySpringTest
@DisplayName("Spec: a pricing manager wants to publish a new tariff")
class PricingManagerPublishesTariffSpecTest {

    private static final String RUN = UUID.randomUUID().toString().substring(0, 8);
    private static final String SHARED_T2 = "T-2-" + RUN;
    private static final String SHARED_T2_VALID_FROM = "2030-04-01T00:00:00Z";

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
    }

    /// T-2: free period 15 min; hour 1 PLN 3.50; hour 2 PLN 5.50; hour 3 PLN 7.50; every further hour PLN 9.50; drop zone
    /// fee PLN 2.50; unauthorized zone return fee PLN 45.00; penalty PLN 150.00.
    private static Map<String, Object> t2(String tariffId, String validFrom) {
        return tariff(tariffId, validFrom, "3.50", "5.50", "7.50", "9.50", "2.50", "45.00", "150.00");
    }

    private synchronized void sharedT2IsPublished() {
        if (pricing.tariffs().stream().noneMatch(tariff -> tariff.tariffId().equals(SHARED_T2))) {
            clock.setTo("2026-11-08T10:00:00Z");
            assertThat(pricing.publish(t2(SHARED_T2, SHARED_T2_VALID_FROM)).status()).isEqualTo(201);
            assertThat(pricing.publish(launchPrices("L-2030-04-02-" + RUN, "2030-04-02T00:00:00Z")).status())
                .isEqualTo(201);
        }
    }

    @Test
    @DisplayName("E1.1 Given it is 2026-11-08 10:00, When Paula publishes T-2 valid from 2026-12-01 00:00 (test day 2030-04-11), Then T-2 is published and listed with its valid from")
    void e1_1_publishes_complete_tariff_valid_from_future_moment() {
        // given
        clock.setTo("2026-11-08T10:00:00Z");
        var tariffId = "T-2-E1-" + RUN;

        // when
        var publication = pricing.publish(t2(tariffId, "2030-04-11T00:00:00Z"));
        pricing.publish(launchPrices("L-2030-04-12-" + RUN, "2030-04-12T00:00:00Z"));

        // then
        assertThat(publication.status()).isEqualTo(201);
        assertThat(pricing.tariffs()).anySatisfy(tariff -> {
            assertThat(tariff.tariffId()).isEqualTo(tariffId);
            assertThat(tariff.validFrom()).isEqualTo("2030-04-11T00:00:00Z");
            assertThat(tariff.firstHourFee()).isEqualTo("3.50");
            assertThat(tariff.penalty()).isEqualTo("150.00");
        });
    }

    @Test
    @DisplayName("E2.1 Given T-2 valid from 2026-12-01 00:00 (test day 2030-04-01), When Anna's ride from 23:50 to 00:30 ending at a station is priced, Then the launch prices apply: PLN 3.00")
    void e2_1_ride_started_before_new_tariff_keeps_launch_tariff() {
        // given
        sharedT2IsPublished();

        // when
        var price = pricing.price("2030-03-31T23:50:00Z", "2030-04-01T00:30:00Z", "STATION");

        // then: the tariff valid before T-2 has the launch prices (its id depends on which tests ran before, plan D8)
        assertThat(price.tariffId()).isNotEqualTo(SHARED_T2);
        assertThat(price.total()).isEqualTo("3.00");
    }

    @Test
    @DisplayName("E2.2 Given T-2 valid from 2026-12-01 00:00 (test day 2030-04-01), When Ben's ride from 00:05 to 00:40 ending at a station is priced, Then T-2 applies: PLN 3.50")
    void e2_2_ride_started_after_new_tariff_uses_it() {
        // given
        sharedT2IsPublished();

        // when
        var price = pricing.price("2030-04-01T00:05:00Z", "2030-04-01T00:40:00Z", "STATION");

        // then
        assertThat(price.tariffId()).isEqualTo(SHARED_T2);
        assertThat(price.total()).isEqualTo("3.50");
    }

    @Test
    @DisplayName("E3.1 Given it is 2026-11-08 10:00 and Anna's ride started at 09:30, When Paula publishes T-3 valid from 09:00, Then the publication is rejected and the ride keeps the launch tariff")
    void e3_1_retroactive_tariff_rejected_and_ride_keeps_launch_tariff() {
        // given
        clock.setTo("2026-11-08T10:00:00Z");

        // when
        var publication = pricing.publish(t2("T-3-E3.1-" + RUN, "2026-11-08T09:00:00Z"));

        // then
        assertThat(publication.status()).isEqualTo(409);
        assertThat(publication.error()).isEqualTo("ValidFromNotInFuture");
        var annasRide = pricing.price("2026-11-08T09:30:00Z", "2026-11-08T10:00:00Z", "STATION");
        assertThat(annasRide.tariffId()).isEqualTo("LAUNCH");
        assertThat(annasRide.total()).isEqualTo("3.00");
    }

    @Test
    @DisplayName("E3.2 Given it is 2026-11-08 10:00, When Paula publishes T-3 valid from 2026-11-08 10:00, Then the publication is rejected")
    void e3_2_valid_from_now_is_rejected() {
        // given
        clock.setTo("2026-11-08T10:00:00Z");

        // when
        var publication = pricing.publish(t2("T-3-E3.2-" + RUN, "2026-11-08T10:00:00Z"));

        // then
        assertThat(publication.status()).isEqualTo(409);
        assertThat(publication.error()).isEqualTo("ValidFromNotInFuture");
    }

    @Test
    @DisplayName("E4.1 Given it is 2026-11-08 10:00, When Paula publishes T-4 valid from 2026-12-01 00:00 without a penalty, Then the publication is rejected")
    void e4_1_tariff_without_penalty_rejected() {
        // given
        clock.setTo("2026-11-08T10:00:00Z");
        var tariff = t2("T-4-E4.1-" + RUN, "2026-12-01T00:00:00Z");
        tariff.remove("penalty");

        // when
        var publication = pricing.publish(tariff);

        // then
        assertThat(publication.status()).isEqualTo(409);
        assertThat(publication.error()).isEqualTo("IncompleteTariff");
        assertThat(pricing.tariffs()).noneMatch(published -> published.tariffId().equals("T-4-E4.1-" + RUN));
    }

    @Test
    @DisplayName("E4.2 Given it is 2026-11-08 10:00, When Paula publishes T-4 valid from 2026-12-01 00:00 with a drop zone fee of PLN -1.00, Then the publication is rejected")
    void e4_2_negative_drop_zone_fee_rejected() {
        // given
        clock.setTo("2026-11-08T10:00:00Z");
        var tariff = t2("T-4-E4.2-" + RUN, "2026-12-01T00:00:00Z");
        tariff.put("dropZoneFee", "-1.00");

        // when
        var publication = pricing.publish(tariff);

        // then
        assertThat(publication.status()).isEqualTo(409);
        assertThat(publication.error()).isEqualTo("NegativeFee");
    }

    @Test
    @DisplayName("E5.1 Given T-2 valid from 2026-12-01 00:00 (test day 2030-04-01), When on 2026-11-08 at 11:00 Paula publishes T-5 valid from the same moment, Then it is rejected and T-2 stays valid from that moment")
    void e5_1_second_tariff_with_same_valid_from_rejected() {
        // given
        sharedT2IsPublished();
        clock.setTo("2026-11-08T11:00:00Z");

        // when
        var publication = pricing.publish(launchPrices("T-5-" + RUN, SHARED_T2_VALID_FROM));

        // then
        assertThat(publication.status()).isEqualTo(409);
        assertThat(publication.error()).isEqualTo("DuplicateValidFrom");
        assertThat(pricing.tariffs()).filteredOn(tariff -> tariff.validFrom().equals(SHARED_T2_VALID_FROM))
                                     .extracting(PricingDriver.Tariff::tariffId)
                                     .containsExactly(SHARED_T2);
        assertThat(pricing.price("2030-04-01T00:05:00Z", "2030-04-01T00:40:00Z", "STATION").tariffId())
            .isEqualTo(SHARED_T2);
    }
}
