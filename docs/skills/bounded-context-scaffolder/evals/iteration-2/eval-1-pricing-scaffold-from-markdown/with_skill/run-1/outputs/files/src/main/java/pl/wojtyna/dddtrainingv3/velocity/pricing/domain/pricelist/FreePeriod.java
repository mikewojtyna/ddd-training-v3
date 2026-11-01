package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist;

import org.jmolecules.ddd.annotation.ValueObject;

import java.time.Duration;
import java.util.Objects;

/// **Free Period**: the initial minutes of a rental session during which no fee is applied.
/// The free period is inclusive: a ride exactly as long as the free period is free.
@ValueObject
public record FreePeriod(Duration length) {

    public FreePeriod {
        Objects.requireNonNull(length, "length");
        if (length.isNegative()) {
            throw new IllegalArgumentException("Free period cannot be negative");
        }
    }

    public static FreePeriod ofMinutes(long minutes) {
        return new FreePeriod(Duration.ofMinutes(minutes));
    }

    /// The part of the ride that comes after the free period, never negative.
    Duration chargeablePartOf(Duration rideDuration) {
        var chargeable = rideDuration.minus(length);
        return chargeable.isNegative() ? Duration.ZERO : chargeable;
    }
}
