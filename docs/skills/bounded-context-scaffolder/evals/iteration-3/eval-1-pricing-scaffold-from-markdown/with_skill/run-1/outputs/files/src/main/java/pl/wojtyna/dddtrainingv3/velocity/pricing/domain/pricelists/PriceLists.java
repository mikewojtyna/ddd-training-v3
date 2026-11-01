package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelists;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;

import java.time.Duration;
import java.time.Instant;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

/// The timeline of every published Price List. It is the consistency boundary for the rule "the rider always pays the
/// price list that was valid when the ride started": choosing the price list needs all of them.
@AggregateRoot
public class PriceLists {

    private final PriceListsId id;
    private final NavigableMap<Instant, PriceList> byValidFrom = new TreeMap<>();

    public PriceLists(PriceListsId id) {
        this.id = Objects.requireNonNull(id, "id");
    }

    /// Publishes a price list. One published later with the same `validFrom` replaces the earlier one; a `validFrom`
    /// in the past is accepted (review defaults, 2026-11-01).
    public DomainEvents publish(PriceList priceList) {
        Objects.requireNonNull(priceList, "priceList");
        byValidFrom.put(priceList.validFrom(), priceList);
        return DomainEvents.of(new PriceListsEvent.PriceListPublished(id, priceList));
    }

    /// Prices a finished ride with the price list valid at ride start. Pricing doesn't change the aggregate.
    public DomainEvents price(Ride ride, ReturnCharges returnCharges) {
        Objects.requireNonNull(ride, "ride");
        Objects.requireNonNull(returnCharges, "returnCharges");
        var totalFee = validAt(ride.startedAt()).totalFeeFor(ride.duration(), returnCharges);
        return DomainEvents.of(new PriceListsEvent.RidePriced(ride.rentalSessionId(), ride.riderId(), totalFee));
    }

    /// Estimates the fee of a ride of the given length starting at `startingAt`, without return charges.
    public TotalFee estimate(Duration rideDuration, Instant startingAt) {
        var priceList = validAt(startingAt);
        return priceList.totalFeeFor(rideDuration, ReturnCharges.none(priceList.currency()));
    }

    private PriceList validAt(Instant moment) {
        var entry = byValidFrom.floorEntry(moment);
        if (entry == null) {
            throw new NoPriceListValidAt(moment);
        }
        return entry.getValue();
    }

    @Identity
    public PriceListsId id() {
        return id;
    }
}
