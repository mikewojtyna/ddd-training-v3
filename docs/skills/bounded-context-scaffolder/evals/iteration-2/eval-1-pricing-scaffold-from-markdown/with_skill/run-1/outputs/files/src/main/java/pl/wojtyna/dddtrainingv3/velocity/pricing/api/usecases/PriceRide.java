package pl.wojtyna.dddtrainingv3.velocity.pricing.api.usecases;

import java.time.Instant;

/// Pricing's own form of Rentals' "Rental Session Ended" event (session id, rider id, start and end time).
public record PriceRide(String rentalSessionId, String riderId, Instant startedAt, Instant endedAt) {
}
