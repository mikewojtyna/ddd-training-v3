package pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases;

/// Canvas inbound command "Return Bike" from the Rider app.
public record ReturnBike(String bikeStationId, String bikeId) {
}
