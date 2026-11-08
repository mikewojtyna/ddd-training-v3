package pl.wojtyna.dddtrainingv3.velocity.settlement.api.queries;

import java.time.Instant;

/// Answer to *Ride Settlement*: `fee` in PLN as a decimal string, `status` one of `PENDING`, `SETTLED`, `UNPAID`;
/// `nextAttemptDueAt` only while a retry is pending, `lastFailureReason` (`DECLINED`, `NO_VALID_PAYMENT_METHOD`) after
/// a failed attempt.
public record RideSettlementView(String sessionId,
                                 String riderId,
                                 String fee,
                                 String status,
                                 int failedAttempts,
                                 Instant nextAttemptDueAt,
                                 String lastFailureReason) {
}
