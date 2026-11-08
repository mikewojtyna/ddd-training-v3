package pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases;

import java.math.BigDecimal;

/// The riding rider ends the ride where they are (free-floating return).
public record EndRideHere(String bikeId, String riderId, BigDecimal latitude, BigDecimal longitude) {
}
