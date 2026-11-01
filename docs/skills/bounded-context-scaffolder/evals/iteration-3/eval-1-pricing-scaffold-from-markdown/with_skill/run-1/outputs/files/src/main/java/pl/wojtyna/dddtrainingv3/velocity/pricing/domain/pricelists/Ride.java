package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelists;

import org.jmolecules.ddd.annotation.ValueObject;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/// A finished ride (a rental session) to be priced.
@ValueObject
public record Ride(RentalSessionId rentalSessionId, RiderId riderId, Instant startedAt, Instant endedAt) {

    public Ride {
        Objects.requireNonNull(rentalSessionId, "rentalSessionId");
        Objects.requireNonNull(riderId, "riderId");
        Objects.requireNonNull(startedAt, "startedAt");
        Objects.requireNonNull(endedAt, "endedAt");
        if (endedAt.isBefore(startedAt)) {
            throw new IllegalArgumentException("A ride cannot end before it started");
        }
    }

    public Duration duration() {
        return Duration.between(startedAt, endedAt);
    }
}
