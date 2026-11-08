package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.usecases;

/// The answer to *Unlock Broken Bike*: `outcome` is `UNLOCKED` or `UNLOCK_FAILED`.
public record UnlockResult(String bikeId, String outcome) {
}
