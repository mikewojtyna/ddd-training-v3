package pl.wojtyna.dddtrainingv3.velocity.rentals.api.queries;

import java.time.Instant;

/// `kind`: `RESERVATION` (with `expiresAt`) or `SESSION` (with `sessionId`).
public record RidersActiveRental(String riderId, String bikeId, String kind, String sessionId, Instant since, Instant expiresAt) {
}
