package pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases;

/// A rider cancels their reservation of a bike.
public record CancelReservation(String bikeId, String riderId) {
}
