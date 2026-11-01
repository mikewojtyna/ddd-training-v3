package pl.wojtyna.dddtrainingv3.velocity.settlement.api.usecases;

import org.joda.money.Money;

/// Our own record for Pricing's "Ride Priced" event (canvas inbound). Pricing isn't in code yet, so no listener
/// translates its event into this record yet.
public record RidePriced(String rideId, String riderId, Money price) {
}
