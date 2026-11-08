package pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases;

import java.time.Instant;

/// The reservation Reserve Bike made.
public record BikeReservation(String bikeId, String holderType, String holderId, Instant reservedAt, Instant expiresAt) {
}
