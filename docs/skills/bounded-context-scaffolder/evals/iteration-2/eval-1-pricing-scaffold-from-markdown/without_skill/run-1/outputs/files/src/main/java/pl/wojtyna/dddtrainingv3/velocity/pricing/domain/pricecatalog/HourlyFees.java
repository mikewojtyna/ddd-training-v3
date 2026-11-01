package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricecatalog;

import org.jmolecules.ddd.annotation.ValueObject;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * Fees of the billing periods of a price list: first, second and third hour, and every subsequent hour.
 */
@ValueObject
public record HourlyFees(Money firstHour, Money secondHour, Money thirdHour, Money subsequentHour) {

    public HourlyFees {
        var fees = Stream.of(firstHour, secondHour, thirdHour, subsequentHour)
                         .map(fee -> Objects.requireNonNull(fee, "All hourly fees are required"))
                         .toList();
        if (fees.stream().anyMatch(Money::isNegative)) {
            throw new IllegalArgumentException("Hourly fees cannot be negative");
        }
        if (fees.stream().map(Money::getCurrencyUnit).distinct().count() > 1) {
            throw new IllegalArgumentException("All hourly fees must be in the same currency");
        }
    }

    /**
     * @param billingPeriod 1-based number of the billing period (a started hour after the free period)
     */
    public Money forBillingPeriod(long billingPeriod) {
        if (billingPeriod < 1) {
            throw new IllegalArgumentException("Billing periods are numbered from 1");
        }
        if (billingPeriod == 1) {
            return firstHour;
        }
        if (billingPeriod == 2) {
            return secondHour;
        }
        if (billingPeriod == 3) {
            return thirdHour;
        }
        return subsequentHour;
    }

    public CurrencyUnit currency() {
        return firstHour.getCurrencyUnit();
    }
}
