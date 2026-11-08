package pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases;

/// A client (Fleet Maintenance) asks Rentals to withdraw a bike (reason `REQUESTED`).
public record WithdrawBike(String bikeId) {
}
