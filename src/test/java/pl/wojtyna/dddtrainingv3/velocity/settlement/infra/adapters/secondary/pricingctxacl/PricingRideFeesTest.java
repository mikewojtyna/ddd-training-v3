package pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.secondary.pricingctxacl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestClient;
import pl.wojtyna.dddtrainingv3.velocity.common.VelocitySpringTest;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.VelocityServiceUrls;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.RiderId;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.ride.EndReason;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.ride.FinishedRide;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.ride.SessionId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.Pln.pln;

/// The Pricing ACL against Pricing's real `GET /pricing/session-price`: every Rentals end reason is translated into
/// Pricing's ending (canvas UL "Ending (when asking Pricing)"), priced by the launch tariff (DE-30) on 2026-11-09.
@VelocitySpringTest
@DisplayName("Settlement's Pricing ACL")
class PricingRideFeesTest {

    @Autowired
    private RestClient restClient;
    @Autowired
    private VelocityServiceUrls urls;

    @ParameterizedTest(name = "{0} 10:00-10:30 → PLN {3}")
    @CsvSource(nullValues = "-", value = {
        "STATION_RETURN,         -,       -,       3.00",
        "FREE_FLOATING_RETURN,   52.2298, 21.0123, 5.00",
        "LOCK_FAILURE,           52.2500, 21.1000, 3.00",
        "MAX_RENTAL_TIME_EXCEEDED, -,     -,       153.00"
    })
    void translatesEndReasonIntoPricingsEnding(EndReason endReason, BigDecimal latitude, BigDecimal longitude, String fee) {
        // given
        var adapter = new PricingRideFees(restClient, urls);
        var ride = new FinishedRide(new SessionId("S-acl"), new RiderId("rider-acl"),
                                    Instant.parse("2026-11-09T10:00:00Z"), Instant.parse("2026-11-09T10:30:00Z"),
                                    endReason, Optional.empty(),
                                    latitude == null ? Optional.empty() : Optional.of(new FinishedRide.Position(latitude, longitude)));

        // when
        var price = adapter.feeOf(ride);

        // then
        assertThat(price).isEqualTo(pln(fee));
    }
}
