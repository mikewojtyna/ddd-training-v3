package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/// **Price List**: set of fees (free period length, first/second/third hour fee, subsequent hourly fee) valid from a
/// given moment.
///
/// Invariants:
/// - all fees are non-negative and in one currency
/// - a ride is priced only with a price list that was already valid when the ride started
@AggregateRoot
public class PriceList {

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
        this.firstHourFee = Objects.requireNonNull(firstHourFee, "firstHourFee");
        this.secondHourFee = Objects.requireNonNull(secondHourFee, "secondHourFee");
        this.thirdHourFee = Objects.requireNonNull(thirdHourFee, "thirdHourFee");
        this.subsequentHourlyFee = Objects.requireNonNull(subsequentHourlyFee, "subsequentHourlyFee");
        var fees = List.of(firstHourFee, secondHourFee, thirdHourFee, subsequentHourlyFee);
        if (fees.stream().anyMatch(Money::isNegative)) {
            throw new IllegalArgumentException("Price list fees cannot be negative");
        }
        if (fees.stream().map(Money::getCurrencyUnit).distinct().count() > 1) {
            throw new IllegalArgumentException("All fees of a price list must be in one currency");
        }
    }

    /// The price list valid at `moment`: the one with the latest valid-from moment that isn't after `moment`.
    ///
    /// Business rule "Price valid at ride start": the rider always pays the price list that was valid when the ride
    /// started.
    public static Optional<PriceList> validAt(Collection<PriceList> priceLists, Instant moment) {
        return priceLists.stream()
                         .filter(priceList -> priceList.hasTakenEffectBy(moment))
                         .max(Comparator.comparing(priceList -> priceList.validFrom));
    }

    /// Prices a finished ride. Total fee = sum of billing period fees + return charges.
    public DomainEvents price(Ride ride, ReturnCharges returnCharges) {
        if (!hasTakenEffectBy(ride.startedAt())) {
            throw new PriceListNotValidForRide(id, ride);
        }
        var totalFee = new TotalFee(billingPeriodsFee(ride.duration()).plus(returnCharges.amount()));
        return DomainEvents.of(new PriceListEvent.RidePriced(ride.rentalSessionId(), ride.riderId(), id, totalFee));
    }

    /// Estimates the fee of a ride lasting `rideDuration`. Return charges are left out, because they're only known
    /// once the ride has ended.
    public TotalFee estimate(Duration rideDuration) {
        return new TotalFee(billingPeriodsFee(rideDuration));
    }

    private boolean hasTakenEffectBy(Instant moment) {
        return !validFrom.isAfter(moment);
    }

    private Money billingPeriodsFee(Duration rideDuration) {
        return BillingPeriod.startedWithin(rideDuration, freePeriod)
                            .stream()
                            .map(this::feeFor)
                            .reduce(Money.zero(currency()), Money::plus);
    }

    private Money feeFor(BillingPeriod billingPeriod) {
        return switch (billingPeriod.number()) {
            case 1 -> firstHourFee;
            case 2 -> secondHourFee;
            case 3 -> thirdHourFee;
            default -> subsequentHourlyFee;
        };
    }

    private CurrencyUnit currency() {
        return firstHourFee.getCurrencyUnit();
    }

    @Identity
    public PriceListId id() {
        return id;
    }
}
