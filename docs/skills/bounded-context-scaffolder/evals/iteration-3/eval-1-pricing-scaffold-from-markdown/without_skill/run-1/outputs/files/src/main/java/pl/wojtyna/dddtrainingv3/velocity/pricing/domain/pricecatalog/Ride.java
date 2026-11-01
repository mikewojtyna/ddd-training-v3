package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricecatalog;

import org.jmolecules.ddd.annotation.ValueObject;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * A finished rental session, as far as Pricing is concerned.
 */
@ValueObject
public record Ride(RentalSessionId sessionId, RiderId riderId, Instant startedAt, Instant endedAt) {

    public Ride {
        Objects.requireNonNull(sessionId, "Rental session id is required");
        Objects.requireNonNull(riderId, "Rider id is required");
        Objects.requireNonNull(startedAt, "Ride start is required");
        Objects.requireNonNull(endedAt, "Ride end is required");
        if (endedAt.isBefore(startedAt)) {
            throw new IllegalArgumentException("Ride cannot end before it started");
        }
    }

    public Duration duration() {
        return Duration.between(startedAt, endedAt);
    }
}
