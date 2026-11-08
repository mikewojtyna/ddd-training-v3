package pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases;

import java.time.Instant;

/// How a rider's session ended; the end reason tells the rider why.
public record SessionEnd(String sessionId, String bikeId, String endReason, Instant endedAt) {
}
