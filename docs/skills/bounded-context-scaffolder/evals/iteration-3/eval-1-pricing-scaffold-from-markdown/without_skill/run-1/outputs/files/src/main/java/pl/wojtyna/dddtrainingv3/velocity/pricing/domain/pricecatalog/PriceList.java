package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricecatalog;

import org.jmolecules.ddd.annotation.Entity;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Set of fees (free period length, first/second/third hour fee, subsequent hourly fee) valid from a given moment.
 * Part of the {@link PriceCatalog} aggregate.
 */
@Entity
public final class PriceList {

    private static final Duration BILLING_PERIOD = Duration.ofHours(1);

    private final PriceListId id;
    private final Instant validFrom;
    private final FreePeriod freePeriod;
    private final HourlyFees hourlyFees;

    public PriceList(PriceListId id, Instant validFrom, FreePeriod freePeriod, HourlyFees hourlyFees) {
        this.id = Objects.requireNonNull(id, "Price list id is required");
        this.validFrom = Objects.requireNonNull(validFrom, "Price list validity start is required");
        this.freePeriod = Objects.requireNonNull(freePeriod, "Free period is required");
        this.hourlyFees = Objects.requireNonNull(hourlyFees, "Hourly fees are required");
    }

    /**
     * Sum of billing period fees for a ride of the given duration. Every started hour after the free period is one
     * billing period. Return charges are not included.
     */
    public Money feeFor(Duration rideDuration) {
        var fee = Money.zero(currency());
        var billingPeriods = billingPeriodsIn(rideDuration);
        for (long billingPeriod = 1; billingPeriod <= billingPeriods; billingPeriod++) {
            fee = fee.plus(hourlyFees.forBillingPeriod(billingPeriod));
        }
        return fee;
    }

    private long billingPeriodsIn(Duration rideDuration) {
        var billable = rideDuration.minus(freePeriod.length());
        if (billable.isNegative() || billable.isZero()) {
            return 0;
        }
        var fullPeriods = billable.dividedBy(BILLING_PERIOD);
        var startedPeriod = billable.minus(BILLING_PERIOD.multipliedBy(fullPeriods)).isZero() ? 0 : 1;
        return fullPeriods + startedPeriod;
    }

    public PriceListId id() {
        return id;
    }

    public Instant validFrom() {
        return validFrom;
    }

    public FreePeriod freePeriod() {
        return freePeriod;
    }

    public HourlyFees hourlyFees() {
        return hourlyFees;
    }

    public CurrencyUnit currency() {
        return hourlyFees.currency();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PriceList other && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "PriceList[id=%s, validFrom=%s]".formatted(id.value(), validFrom);
    }
}
