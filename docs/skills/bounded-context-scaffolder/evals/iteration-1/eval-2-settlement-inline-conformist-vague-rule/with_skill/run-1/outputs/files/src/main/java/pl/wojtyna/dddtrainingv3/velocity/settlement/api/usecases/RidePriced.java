package pl.wojtyna.dddtrainingv3.velocity.settlement.api.usecases;

import org.joda.money.Money;

import java.util.Objects;

/// Settlement's own record of the canvas inbound event **Ride Priced** (from Pricing).
///
/// Pricing doesn't exist in code yet, so nothing translates its published event into this record. Once it does, an
/// `@AntiCorruptionLayer` listener in `infra/adapters/primary/pricingctxacl` should do that translation.
public record RidePriced(String rideId, String riderId, Money price) {

    public RidePriced {
        Objects.requireNonNull(rideId, "rideId");
        Objects.requireNonNull(riderId, "riderId");
        Objects.requireNonNull(price, "price");
        if (price.isNegative()) {
            throw new IllegalArgumentException("Ride price cannot be negative");
        }
    }
}
