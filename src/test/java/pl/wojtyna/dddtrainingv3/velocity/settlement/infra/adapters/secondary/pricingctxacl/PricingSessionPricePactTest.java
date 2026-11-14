package pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.secondary.pricingctxacl;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.annotations.PactDirectory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.web.client.RestClient;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.RiderId;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.ride.EndReason;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.ride.FinishedRide;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.ride.SessionId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.MONEY;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.PACT_DIRECTORY;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.PRICING;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.SETTLEMENT;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.urlsOf;
import static pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.Pln.pln;

/// Consumer side of `Pricing [U,OHS,PL]->[D,CF] Settlement`: Settlement asks Pricing's *Price for Session* in Pricing's
/// *ending* vocabulary and takes `total` (a PLN decimal string) as the ride's fee. The launch tariff (DE-30) prices a
/// 30-minute ride at PLN 3.00, plus PLN 2.00 when it ends free-floating in a drop zone. `billingHours` must be present
/// too: Settlement's response record reads it as an `int` (found by this pact; Jackson 3 rejects a missing primitive).
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = PRICING, pactVersion = PactSpecVersion.V4)
@PactDirectory(PACT_DIRECTORY)
@DisplayName("Contract: Settlement → Pricing (price for session)")
class PricingSessionPricePactTest {

    private static final String STARTED_AT = "2026-11-09T10:00:00Z";
    private static final String ENDED_AT = "2026-11-09T10:30:00Z";

    @Pact(consumer = SETTLEMENT)
    V4Pact stationReturnIsPriced(PactDslWithProvider builder) {
        return builder.given("the launch tariff is in force")
                      .uponReceiving("a price query for a 30-minute ride returned at a station")
                      .path("/pricing/session-price")
                      .query("startedAt=" + STARTED_AT + "&endedAt=" + ENDED_AT + "&ending=STATION")
                      .method("GET")
                      .willRespondWith()
                      .status(200)
                      .body(price("3.00"))
                      .toPact(V4Pact.class);
    }

    @Pact(consumer = SETTLEMENT)
    V4Pact freeFloatingReturnIsPricedAtItsPosition(PactDslWithProvider builder) {
        return builder.given("the launch tariff is in force")
                      .uponReceiving("a price query for a 30-minute ride ended free-floating in a drop zone")
                      .path("/pricing/session-price")
                      .query("startedAt=" + STARTED_AT + "&endedAt=" + ENDED_AT
                             + "&ending=FREE_FLOATING&latitude=52.2298&longitude=21.0123")
                      .method("GET")
                      .willRespondWith()
                      .status(200)
                      .body(price("5.00"))
                      .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "stationReturnIsPriced")
    void stationReturnIsPriced(MockServer pricing) {
        var ride = ride(EndReason.STATION_RETURN, Optional.of("Central"), Optional.empty());

        assertThat(fees(pricing).feeOf(ride)).isEqualTo(pln("3.00"));
    }

    @Test
    @PactTestFor(pactMethod = "freeFloatingReturnIsPricedAtItsPosition")
    void freeFloatingReturnIsPricedAtItsPosition(MockServer pricing) {
        var ride = ride(EndReason.FREE_FLOATING_RETURN, Optional.empty(),
                        Optional.of(new FinishedRide.Position(new BigDecimal("52.2298"), new BigDecimal("21.0123"))));

        assertThat(fees(pricing).feeOf(ride)).isEqualTo(pln("5.00"));
    }

    /// Settlement uses only `total`, but its response record declares `billingHours` as an `int`, which Jackson can't
    /// leave unset: the field is part of what Settlement needs from Pricing.
    private static PactDslJsonBody price(String total) {
        return new PactDslJsonBody().integerType("billingHours", 1).stringMatcher("total", MONEY, total);
    }

    private static FinishedRide ride(EndReason endReason, Optional<String> stationId, Optional<FinishedRide.Position> position) {
        return new FinishedRide(new SessionId("S-pact"), new RiderId("rider-pact"), Instant.parse(STARTED_AT),
                                Instant.parse(ENDED_AT), endReason, stationId, position);
    }

    private static PricingRideFees fees(MockServer pricing) {
        return new PricingRideFees(RestClient.builder().build(), urlsOf(pricing.getUrl()));
    }
}
