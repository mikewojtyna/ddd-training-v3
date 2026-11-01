package pl.wojtyna.dddtrainingv3.velocity.pricing.api.usecases;

import java.time.Instant;

/// Canvas inbound event "Rental Session Ended" (from Rentals), in Pricing's own words. A listener translating Rentals'
/// published event into this record is a follow-up: Rentals doesn't publish it in its `api` yet.
public record RentalSessionEnded(String rentalSessionId, String riderId, Instant startedAt, Instant endedAt) {
}
