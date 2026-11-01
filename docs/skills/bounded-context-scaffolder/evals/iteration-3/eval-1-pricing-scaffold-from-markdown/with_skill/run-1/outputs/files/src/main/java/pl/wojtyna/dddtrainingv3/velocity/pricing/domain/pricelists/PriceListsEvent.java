package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelists;

import pl.wojtyna.dddtrainingv3.common.domain.DomainEvent;

public sealed interface PriceListsEvent extends DomainEvent {

    record PriceListPublished(PriceListsId priceListsId, PriceList priceList) implements PriceListsEvent {
    }

    record RidePriced(RentalSessionId rentalSessionId, RiderId riderId, TotalFee totalFee) implements PriceListsEvent {
    }
}
