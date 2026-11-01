package pl.wojtyna.dddtrainingv3.velocity.settlement.api.usecases;

import java.math.BigDecimal;

/// Settlement's own record of the inbound canvas event "Ride Priced" from Pricing.
/// A listener translating Pricing's published event into this record is a follow-up: Pricing isn't in code yet.
public record RidePriced(String rideId, String riderId, BigDecimal amount, String currency) {
}
