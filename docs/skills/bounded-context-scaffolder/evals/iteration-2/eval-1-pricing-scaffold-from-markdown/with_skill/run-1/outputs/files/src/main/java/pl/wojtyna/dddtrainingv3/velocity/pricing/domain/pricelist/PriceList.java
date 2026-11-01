package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/// **Price List**: set of fees (free period length, first/second/third hour fee, subsequent hourly fee) valid from a
/// given moment. A price list is immutable once published; pricing a ride doesn't change it.
@AggregateRoot
public class PriceList {

    public static final CurrencyUnit CURRENCY = CurrencyUnit.of("PLN");

    private final PriceListId id;
    private final Instant validFrom;
    private final FreePeriod freePeriod;
    private final Money firstHourFee;
    private final Money secondHourFee;
    private final Money thirdHourFee;
    private final Money subsequentHourlyFee;

    public PriceList(PriceListId id, Instant validFrom, FreePeriod freePeriod, Money firstHourFee,
                     Money secondHourFee, Money thirdHourFee, Money subsequentHourlyFee) {
        this.id = Objects.requireNonNull(id, "id");
        this.validFrom = Objects.requireNonNull(validFrom, "validFrom");
        this.freePeriod = Objects.requireNonNull(freePeriod, "freePeriod");
        this.firstHourFee = requireValidFee(firstHourFee, "firstHourFee");
        this.secondHourFee = requireValidFee(secondHourFee, "secondHourFee");
        this.thirdHourFee = requireValidFee(thirdHourFee, "thirdHourFee");
        this.subsequentHourlyFee = requireValidFee(subsequentHourlyFee, "subsequentHourlyFee");
    }

    /// Prices a finished ride: the total fee is the sum of the fees of all billing periods started after the free
    /// period, plus the return charges.
    public DomainEvents priceRide(RentalSessionId rentalSessionId, RiderId riderId, Ride ride,
                                  ReturnCharges returnCharges) {
        Objects.requireNonNull(rentalSessionId, "rentalSessionId");
        Objects.requireNonNull(riderId, "riderId");
        Objects.requireNonNull(ride, "ride");
        var totalFee = totalFeeFor(ride.duration(), returnCharges);
        return DomainEvents.of(new PriceListEvent.RidePriced(rentalSessionId, riderId, id, totalFee));
    }

    /// Calculates the total fee of a ride of the given duration, without recording anything. Used for estimates.
    public TotalFee totalFeeFor(Duration rideDuration, ReturnCharges returnCharges) {
        Objects.requireNonNull(rideDuration, "rideDuration");
        Objects.requireNonNull(returnCharges, "returnCharges");
        var billingPeriodFees = BillingPeriod.startedWithin(freePeriod.chargeablePartOf(rideDuration))
                                             .map(this::feeFor)
                                             .reduce(Money.zero(CURRENCY), Money::plus);
        return new TotalFee(billingPeriodFees.plus(returnCharges.amount()));
    }

    boolean isValidAt(Instant moment) {
        return !validFrom.isAfter(moment);
    }

    Instant validFrom() {
        return validFrom;
    }

    @Identity
    public PriceListId id() {
        return id;
    }

    private Money feeFor(BillingPeriod billingPeriod) {
        return switch (billingPeriod.ordinal()) {
            case 1 -> firstHourFee;
            case 2 -> secondHourFee;
            case 3 -> thirdHourFee;
            default -> subsequentHourlyFee;
        };
    }

    private static Money requireValidFee(Money fee, String name) {
        Objects.requireNonNull(fee, name);
        if (!CURRENCY.equals(fee.getCurrencyUnit())) {
            throw new IllegalArgumentException(name + " must be in " + CURRENCY);
        }
        if (fee.isNegative()) {
            throw new IllegalArgumentException(name + " cannot be negative");
        }
        return fee;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PriceList other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
