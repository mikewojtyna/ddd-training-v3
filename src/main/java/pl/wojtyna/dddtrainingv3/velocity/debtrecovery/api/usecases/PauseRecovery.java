package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.usecases;

import java.time.Instant;

/// *Pause Recovery*: a collections agent pauses a rider's running recovery. `reason` is one of `DISPUTE`,
/// `PAYMENT_PLAN`, `REGULATORY_HOLD`.
public record PauseRecovery(String riderId, String reason, String agentId, Instant now) {
}
