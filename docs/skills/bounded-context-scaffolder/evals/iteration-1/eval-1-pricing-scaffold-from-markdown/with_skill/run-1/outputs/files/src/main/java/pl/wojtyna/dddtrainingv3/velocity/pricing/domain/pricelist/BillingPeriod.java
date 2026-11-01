package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist;

import org.jmolecules.ddd.annotation.ValueObject;

import java.time.Duration;
import java.util.List;
import java.util.stream.IntStream;

/// **Billing Period**: one started hour of a ride after the free period.
///
/// `number` is 1-based: billing period 1 is the first started hour after the free period.
@ValueObject
public record BillingPeriod(int number) {

    private static final Duration ONE_HOUR = Duration.ofHours(1);

    public BillingPeriod {
        if (number < 1) {
            throw new IllegalArgumentException("Billing period number starts at 1");
        }
    }

    /// Every billing period started by a ride of `rideDuration`. Started hours are counted from the end of the free
    /// period, as the definition says. A ride not longer than the free period has no billing periods.
    public static List<BillingPeriod> startedWithin(Duration rideDuration, FreePeriod freePeriod) {
        var afterFreePeriod = rideDuration.minus(freePeriod.length());
        if (afterFreePeriod.isNegative() || afterFreePeriod.isZero()) {
            return List.of();
        }
        var fullHours = afterFreePeriod.dividedBy(ONE_HOUR);
        var startedHours = afterFreePeriod.minus(ONE_HOUR.multipliedBy(fullHours)).isZero() ? fullHours : fullHours + 1;
        return IntStream.rangeClosed(1, Math.toIntExact(startedHours)).mapToObj(BillingPeriod::new).toList();
    }
}
