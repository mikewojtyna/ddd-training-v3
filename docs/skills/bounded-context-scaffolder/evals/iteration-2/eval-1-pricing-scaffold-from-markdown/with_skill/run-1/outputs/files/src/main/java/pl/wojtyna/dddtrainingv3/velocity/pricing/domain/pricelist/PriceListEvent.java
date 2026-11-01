package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist;

import pl.wojtyna.dddtrainingv3.common.domain.DomainEvent;

public sealed interface PriceListEvent extends DomainEvent {

    /// Canvas outbound event "Ride Priced" (to Settlement), in Pricing's internal language.
    record RidePriced(RentalSessionId rentalSessionId, RiderId riderId, PriceListId priceListId, TotalFee totalFee)
        implements PriceListEvent {
    }
}
