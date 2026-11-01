package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricecatalog;

import org.joda.money.Money;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvent;

import java.time.Instant;

public sealed interface PricingEvent extends DomainEvent {

    record PriceListPublished(PriceListId priceListId, Instant validFrom) implements PricingEvent {
    }

    record RidePriced(RentalSessionId sessionId, RiderId riderId, PriceListId appliedPriceList, Money totalFee)
        implements PricingEvent {
    }
}
