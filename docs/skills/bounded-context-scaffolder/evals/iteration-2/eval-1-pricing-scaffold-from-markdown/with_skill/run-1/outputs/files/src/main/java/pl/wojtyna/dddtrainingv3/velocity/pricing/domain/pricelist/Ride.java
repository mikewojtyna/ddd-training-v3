package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist;

import org.jmolecules.ddd.annotation.ValueObject;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/// A finished ride of a rental session: when it started and when it ended.
@ValueObject
public record Ride(Instant startedAt, Instant endedAt) {

    public Ride {
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
