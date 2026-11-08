package pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases;

/// The riding rider returns the bike at a station, optionally reporting a fault.
public record ReturnBikeAtStation(String bikeId, String riderId, String stationId, String faultDescription) {
}
