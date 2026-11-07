package pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases;

/// Canvas inbound command *Withdraw bike* (Fleet Maintenance). Fleet Maintenance decided the bike is out of
/// service; from now on Rentals doesn't offer it and it can't be reserved (rule R5).
public record WithdrawBike(String bikeId) {
}
