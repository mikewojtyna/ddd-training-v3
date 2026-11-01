package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist;

import org.jmolecules.ddd.annotation.ValueObject;

import java.time.Duration;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/// **Billing Period**: one started hour of a ride after the free period. The ordinal is 1-based: the first started
/// hour after the free period is billing period 1.
@ValueObject
public record BillingPeriod(int ordinal) {

    static final Duration LENGTH = Duration.ofHours(1);

    public BillingPeriod {
        if (ordinal < 1) {
            throw new IllegalArgumentException("Billing period ordinal must be at least 1");
        }
    }

    /// All billing periods started within the given chargeable time (the time after the free period). An hour that
    /// ends exactly at the end of the ride is complete, so the next hour hasn't started.
    static Stream<BillingPeriod> startedWithin(Duration chargeable) {
        var started = Math.ceilDiv(chargeable.toNanos(), LENGTH.toNanos());
        return IntStream.rangeClosed(1, Math.toIntExact(started)).mapToObj(BillingPeriod::new);
    }
}
