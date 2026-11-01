package pl.wojtyna.dddtrainingv3.velocity.pricing.api.events;

import org.jmolecules.ddd.annotation.ValueObject;
import org.joda.money.Money;
import pl.wojtyna.dddtrainingv3.common.integration.IntegrationEvent;

import java.time.Instant;
import java.util.Objects;

/**
 * Published by Pricing once the total fee of a finished ride is known.
 */
@ValueObject
public record RidePriced(String rideId, String riderId, Money totalFee, Instant pricedAt) implements IntegrationEvent {

    public RidePriced {
        Objects.requireNonNull(rideId, "rideId");
        Objects.requireNonNull(riderId, "riderId");
        Objects.requireNonNull(totalFee, "totalFee");
        Objects.requireNonNull(pricedAt, "pricedAt");
    }
}
