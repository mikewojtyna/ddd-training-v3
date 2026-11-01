package pl.wojtyna.dddtrainingv3.velocity.pricing.api.usecases;

import java.time.Instant;

/// Pricing's own record of the fact "Rental Session Ended" published by Rentals (canvas inbound event).
///
/// Rentals doesn't publish this event in code yet. When it does, an `@AntiCorruptionLayer` listener in
/// `infra/adapters/primary/rentalsctxacl` should translate Rentals' event into this record.
public record RentalSessionEnded(String rentalSessionId, String riderId, Instant startedAt, Instant endedAt) {
}
