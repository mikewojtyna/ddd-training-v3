package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist;

import org.jmolecules.ddd.annotation.ValueObject;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/// A finished ride to be priced: Pricing's view of a rental session that Rentals reported as ended.
///
/// The canvas purpose and every business rule use "ride", but the canvas's ubiquitous language doesn't define it
/// yet (see the plan's open points).
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
