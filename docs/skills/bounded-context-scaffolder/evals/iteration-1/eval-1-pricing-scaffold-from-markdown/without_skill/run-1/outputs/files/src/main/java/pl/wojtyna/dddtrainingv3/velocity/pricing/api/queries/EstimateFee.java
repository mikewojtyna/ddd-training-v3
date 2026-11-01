package pl.wojtyna.dddtrainingv3.velocity.pricing.api.queries;

import org.jmolecules.ddd.annotation.ValueObject;

import java.time.Duration;
import java.util.Objects;

/**
 * Estimate Fee: how much would a ride of the given duration, starting now, cost.
 */
@ValueObject
public record EstimateFee(Duration rideDuration) {

    public EstimateFee {
        Objects.requireNonNull(rideDuration, "Ride duration is required");
        if (rideDuration.isNegative()) {
            throw new IllegalArgumentException("Ride duration cannot be negative");
        }
    }
}
