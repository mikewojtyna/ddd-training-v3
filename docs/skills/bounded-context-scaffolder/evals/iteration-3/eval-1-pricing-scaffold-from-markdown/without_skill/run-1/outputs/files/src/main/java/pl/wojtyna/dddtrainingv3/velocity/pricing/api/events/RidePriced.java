package pl.wojtyna.dddtrainingv3.velocity.pricing.api.events;

import org.jmolecules.ddd.annotation.ValueObject;
import pl.wojtyna.dddtrainingv3.common.integration.IntegrationEvent;

import java.math.BigDecimal;

/**
 * Ride Priced: published language of Pricing, consumed by Settlement.
 */
@ValueObject
public record RidePriced(String rentalSessionId, String riderId, String priceListId, BigDecimal totalFee,
                         String currency) implements IntegrationEvent {
}
