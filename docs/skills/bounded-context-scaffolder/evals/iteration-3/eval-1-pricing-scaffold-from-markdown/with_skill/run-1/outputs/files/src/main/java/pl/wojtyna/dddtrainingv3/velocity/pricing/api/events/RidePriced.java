package pl.wojtyna.dddtrainingv3.velocity.pricing.api.events;

import org.jmolecules.ddd.annotation.ValueObject;
import pl.wojtyna.dddtrainingv3.common.integration.IntegrationEvent;

import java.math.BigDecimal;

/// Canvas outbound event "Ride Priced" (to Settlement). Published language: primitives and strings only.
@ValueObject
public record RidePriced(String rentalSessionId, String riderId, BigDecimal totalFee, String currency)
    implements IntegrationEvent {
}
