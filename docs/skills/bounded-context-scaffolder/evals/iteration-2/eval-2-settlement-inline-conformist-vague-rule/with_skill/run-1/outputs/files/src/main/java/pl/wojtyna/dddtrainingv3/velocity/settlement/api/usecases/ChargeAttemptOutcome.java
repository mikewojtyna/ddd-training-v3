package pl.wojtyna.dddtrainingv3.velocity.settlement.api.usecases;

/// The outcome of one Charge Attempt for a ride. Whether it comes from a synchronous Charge Rider result or from a
/// Payments event is still open (plan, Q5); both variants end up here.
public record ChargeAttemptOutcome(String rideId, boolean succeeded) {
}
