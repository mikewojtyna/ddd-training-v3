package pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases;

import java.math.BigDecimal;

/// A client (Fleet Maintenance) puts a serviced bike back into circulation, at a station or at a position.
public record MakeBikeAvailable(String bikeId, String stationId, BigDecimal latitude, BigDecimal longitude) {
}
