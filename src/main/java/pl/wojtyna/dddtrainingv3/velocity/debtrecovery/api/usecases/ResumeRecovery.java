package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.usecases;

import java.time.Instant;

/// *Resume Recovery*: a collections agent resumes a rider's paused recovery.
public record ResumeRecovery(String riderId, String agentId, Instant now) {
}
