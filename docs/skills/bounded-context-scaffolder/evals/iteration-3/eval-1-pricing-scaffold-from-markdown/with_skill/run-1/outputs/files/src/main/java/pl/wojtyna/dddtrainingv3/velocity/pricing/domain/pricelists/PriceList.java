package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelists;

import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.stream.Stream;

/// **Price List**: set of fees (free period length, first/second/third hour fee, subsequent hourly fee) valid from a
/// given moment. Immutable once published, identified by `validFrom` inside `PriceLists`.
@Entity
public record PriceList(@Identity Instant validFrom,
                        FreePeriod freePeriod,
                        Money firstHourFee,
                        Money secondHourFee,
                        Money thirdHourFee,
                        Money subsequentHourlyFee) {

    public PriceList {
        Objects.requireNonNull(validFrom, "validFrom");
        Objects.requireNonNull(freePeriod, "freePeriod");
        Objects.requireNonNull(firstHourFee, "firstHourFee");
        Objects.requireNonNull(secondHourFee, "secondHourFee");
        Objects.requireNonNull(thirdHourFee, "thirdHourFee");
        Objects.requireNonNull(subsequentHourlyFee, "subsequentHourlyFee");
        var fees = Stream.of(firstHourFee, secondHourFee, thirdHourFee, subsequentHourlyFee).toList();
        if (fees.stream().anyMatch(Money::isNegative)) {
            throw new IllegalArgumentException("Price list fees cannot be negative");
        }
        if (fees.stream().map(Money::getCurrencyUnit).distinct().count() != 1) {
            throw new IllegalArgumentException("All fees of a price list must be in one currency");
        }
    }

    public CurrencyUnit currency() {
        return firstHourFee.getCurrencyUnit();
    }

    /// Period 1 is the first-hour fee, 2 the second-hour fee, 3 the third-hour fee, 4 and later the subsequent hourly
    /// fee.
    Money feeFor(BillingPeriod period) {
        if (period.ordinal() == 1) {
            return firstHourFee;
        }
        if (period.ordinal() == 2) {
            return secondHourFee;
        }
        if (period.ordinal() == 3) {
            return thirdHourFee;
        }
        return subsequentHourlyFee;
    }

    TotalFee totalFeeFor(Duration rideDuration, ReturnCharges returnCharges) {
        var billingPeriodFees = BillingPeriod.startedHoursOf(freePeriod.chargeablePartOf(rideDuration))
                                             .stream()
                                             .map(this::feeFor)
                                             .reduce(Money.zero(currency()), Money::plus);
        return new TotalFee(billingPeriodFees.plus(returnCharges.amount()));
    }
}
