package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelists;

import org.jmolecules.ddd.annotation.ValueObject;

import java.time.Duration;
import java.util.List;
import java.util.stream.LongStream;

/// **Billing Period**: one started hour of a ride after the free period. `ordinal` is 1-based: period 1 is the first
/// started hour after the free period.
@ValueObject
public record BillingPeriod(long ordinal) {

    private static final Duration HOUR = Duration.ofHours(1);

    public BillingPeriod {
        if (ordinal < 1) {
            throw new IllegalArgumentException("Billing period ordinal starts at 1");
        }
    }

    /// Every started hour of the chargeable part of a ride is one billing period.
    static List<BillingPeriod> startedHoursOf(Duration chargeable) {
        var fullHours = chargeable.dividedBy(HOUR);
        var startedHours = chargeable.equals(HOUR.multipliedBy(fullHours)) ? fullHours : fullHours + 1;
        return LongStream.rangeClosed(1, startedHours).mapToObj(BillingPeriod::new).toList();
    }
}
