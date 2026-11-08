package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.fee;

import org.jmolecules.ddd.annotation.ValueObject;

import java.time.Duration;
import java.time.Instant;

/// A finished rental session as Pricing sees it: when it started, when it ended and how it ended. Pricing keeps no
/// sessions; it gets these facts with every price question.
@ValueObject
public record Session(Instant startedAt, Instant endedAt, Ending ending) {

    public Session {
        if (startedAt == null || endedAt == null || ending == null) {
            throw new IllegalArgumentException("A session needs a start, an end and an ending");
        }
        if (!endedAt.isAfter(startedAt)) {
            throw new IllegalArgumentException("A session must end after it started");
        }
    }

    /// Billing time is measured to the second (DE-83).
    public Duration duration() {
        return Duration.ofSeconds(Duration.between(startedAt, endedAt).getSeconds());
    }
}
