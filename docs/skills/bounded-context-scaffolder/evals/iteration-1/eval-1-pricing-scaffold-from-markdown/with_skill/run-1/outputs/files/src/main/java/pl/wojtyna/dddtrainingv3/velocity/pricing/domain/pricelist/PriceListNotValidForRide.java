package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist;

/// Thrown when a ride is priced with a price list that wasn't valid yet when the ride started.
public class PriceListNotValidForRide extends RuntimeException {

    public PriceListNotValidForRide(PriceListId priceListId, Ride ride) {
        super("Price list " + priceListId.value() + " was not valid yet when ride " + ride.rentalSessionId().value()
              + " started at " + ride.startedAt());
    }
}
