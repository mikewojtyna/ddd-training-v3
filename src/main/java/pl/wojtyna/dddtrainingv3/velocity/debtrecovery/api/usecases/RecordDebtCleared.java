package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.usecases;

import java.time.Instant;

/// *Record Debt Cleared*: the rider's outstanding debt went down to PLN 0.00 (heard from Settlement's *Outstanding
/// Debt Cleared*).
public record RecordDebtCleared(String riderId, Instant clearedAt) {
}
