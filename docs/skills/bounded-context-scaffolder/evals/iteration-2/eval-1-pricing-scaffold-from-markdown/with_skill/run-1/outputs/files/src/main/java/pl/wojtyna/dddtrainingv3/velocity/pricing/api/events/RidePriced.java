package pl.wojtyna.dddtrainingv3.velocity.pricing.api.events;

import org.jmolecules.ddd.annotation.ValueObject;
import pl.wojtyna.dddtrainingv3.common.integration.IntegrationEvent;

import java.math.BigDecimal;

/// Published language for the canvas outbound event "Ride Priced" (to Settlement).
@ValueObject
public record RidePriced(String rentalSessionId, String riderId, BigDecimal totalFeeAmount, String currency,
                         String priceListId) implements IntegrationEvent {
}
