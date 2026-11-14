package pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.primary.rentalsctxacl;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.consumer.junit5.ProviderType;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.V4Interaction;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.annotations.PactDirectory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.events.RentalSessionEnded;
import pl.wojtyna.dddtrainingv3.velocity.settlement.api.usecases.ChargeRide;
import pl.wojtyna.dddtrainingv3.velocity.settlement.api.usecases.ChargeRideUseCase;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.EVENT_JSON;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.INSTANT;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.PACT_DIRECTORY;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.RENTALS_EVENTS;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.SETTLEMENT;

/// Consumer side of Rentals' integration event `RentalSessionEnded` (CML `Rentals [U,PL]->[D,CF] Settlement`): whenever
/// a rental session ends, Settlement charges the ride. It needs the session, the rider, the times, the end reason and
/// where the bike was left: the station for a station return, the position for a free-floating return.
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = RENTALS_EVENTS, providerType = ProviderType.ASYNCH, pactVersion = PactSpecVersion.V4)
@PactDirectory(PACT_DIRECTORY)
@DisplayName("Contract: Rentals → Settlement (RentalSessionEnded)")
class RentalSessionEndedPactTest {

    private static final String STARTED_AT = "2026-11-08T10:00:00Z";
    private static final String ENDED_AT = "2026-11-08T10:30:00Z";

    @Pact(consumer = SETTLEMENT)
    V4Pact sessionEndedAtStation(MessagePactBuilder builder) {
        return builder.expectsToReceive("a rental session ended with a return at a station")
                      .withContent(session().stringValue("endReason", "STATION_RETURN").stringType("stationId", "Central"))
                      .toPact(V4Pact.class);
    }

    @Pact(consumer = SETTLEMENT)
    V4Pact sessionEndedFreeFloating(MessagePactBuilder builder) {
        return builder.expectsToReceive("a rental session ended with a free-floating return")
                      .withContent(session().stringValue("endReason", "FREE_FLOATING_RETURN")
                                            .decimalType("latitude", new BigDecimal("52.2298"))
                                            .decimalType("longitude", new BigDecimal("21.0123")))
                      .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "sessionEndedAtStation")
    void chargesARideReturnedAtAStation(V4Interaction.AsynchronousMessage message) throws Exception {
        var chargeRide = mock(ChargeRideUseCase.class);

        new RentalSessionEndedListener(chargeRide).on(EVENT_JSON.readValue(message.contentsAsBytes(), RentalSessionEnded.class));

        verify(chargeRide).handle(new ChargeRide("S-1", "rider-anna", Instant.parse(STARTED_AT), Instant.parse(ENDED_AT),
                                                 "STATION_RETURN", "Central", null, null));
    }

    @Test
    @PactTestFor(pactMethod = "sessionEndedFreeFloating")
    void chargesARideEndedFreeFloating(V4Interaction.AsynchronousMessage message) throws Exception {
        var chargeRide = mock(ChargeRideUseCase.class);

        new RentalSessionEndedListener(chargeRide).on(EVENT_JSON.readValue(message.contentsAsBytes(), RentalSessionEnded.class));

        var command = ArgumentCaptor.forClass(ChargeRide.class);
        verify(chargeRide).handle(command.capture());
        assertThat(command.getValue().endReason()).isEqualTo("FREE_FLOATING_RETURN");
        assertThat(command.getValue().stationId()).isNull();
        assertThat(command.getValue().latitude()).isEqualByComparingTo("52.2298");
        assertThat(command.getValue().longitude()).isEqualByComparingTo("21.0123");
    }

    private static PactDslJsonBody session() {
        return new PactDslJsonBody().stringType("sessionId", "S-1")
                                    .stringType("riderId", "rider-anna")
                                    .stringMatcher("startedAt", INSTANT, STARTED_AT)
                                    .stringMatcher("endedAt", INSTANT, ENDED_AT);
    }
}
