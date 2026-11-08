package pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases;

/// The reservation's holder unlocks the bike.
public record UnlockBike(String bikeId, String holderType, String holderId) {
}
