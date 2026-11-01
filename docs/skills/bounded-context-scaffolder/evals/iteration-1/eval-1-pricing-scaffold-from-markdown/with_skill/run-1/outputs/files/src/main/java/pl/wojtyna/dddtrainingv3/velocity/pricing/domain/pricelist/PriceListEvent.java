package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist;

import pl.wojtyna.dddtrainingv3.common.domain.DomainEvent;

public sealed interface PriceListEvent extends DomainEvent {

    /// A finished ride got its total fee, calculated with the price list that was valid when the ride started.
    record RidePriced(RentalSessionId rentalSessionId, RiderId riderId, PriceListId priceListId,
                      TotalFee totalFee) implements PriceListEvent {
    }
}
