package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricecatalog;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.joda.money.Money;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricecatalog.FluentLanguage.WithReturnCharges;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * All published price lists. Guarantees that at any moment exactly one price list (the latest one that became
 * valid) applies, so a ride is always priced with the price list that was valid when the ride started.
 */
@AggregateRoot
public class PriceCatalog {

    private final PriceCatalogId id;
    private final TreeMap<Instant, PriceList> priceListsByValidFrom = new TreeMap<>();

    public PriceCatalog(PriceCatalogId id) {
        this.id = Objects.requireNonNull(id, "Price catalog id is required");
    }

    public DomainEvents publish(PriceList priceList) {
        if (priceListsByValidFrom.containsKey(priceList.validFrom())) {
            throw new PriceListAlreadyPublishedException(
                "A price list valid from %s is already published".formatted(priceList.validFrom()));
        }
        if (priceListsByValidFrom.containsValue(priceList)) {
            throw new PriceListAlreadyPublishedException(
                "Price list %s is already published".formatted(priceList.id().value()));
        }
        priceListsByValidFrom.put(priceList.validFrom(), priceList);
        return DomainEvents.of(new PricingEvent.PriceListPublished(priceList.id(), priceList.validFrom()));
    }

    public WithReturnCharges<ReturnCharges, DomainEvents> price(Ride ride) {
        return returnCharges -> {
            var priceList = validAt(ride.startedAt());
            var totalFee = priceList.feeFor(ride.duration()).plus(returnCharges.amount());
            return DomainEvents.of(
                new PricingEvent.RidePriced(ride.sessionId(), ride.riderId(), priceList.id(), totalFee));
        };
    }

    /**
     * Fee of a ride of the given duration that starts at the given moment. Return charges are not included because
     * they are not known before the ride ends.
     */
    public Money estimateFee(Duration rideDuration, Instant rideStart) {
        return validAt(rideStart).feeFor(rideDuration);
    }

    private PriceList validAt(Instant moment) {
        return Optional.ofNullable(priceListsByValidFrom.floorEntry(moment))
                       .map(Map.Entry::getValue)
                       .orElseThrow(() -> new NoValidPriceListException(
                           "No price list is valid at %s".formatted(moment)));
    }

    public PriceCatalogId id() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PriceCatalog other && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
