package pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases;

/// A fleet manager puts a new bike into service at a station.
public record AddBikeToFleet(String bikeId, String stationId) {
}
