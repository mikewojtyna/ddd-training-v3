package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.queries;

import java.time.Instant;

/// A rider's recovery process as a collections agent sees it. `status` is `RUNNING`, `PAUSED`, `SUCCEEDED` or
/// `EXHAUSTED`; `outstandingDebt` is a decimal string in PLN (Debt Recovery's last known figure);
/// `nextAttemptDueAt` only while running, `pauseReason` only while paused.
public record RecoveryProcessView(String riderId, String status, String outstandingDebt, int failedAttempts,
                                  Instant nextAttemptDueAt, String pauseReason) {
}
