package pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases;

/// `outcome` is `UNLOCKED` or `UNLOCK_FAILED`; `sessionId` is set when a rider's session started.
public record UnlockOutcome(String bikeId, String outcome, String sessionId) {
}
