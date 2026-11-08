package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.ride;

import org.joda.money.Money;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvent;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.ChargeFailureReason;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.RiderId;

import java.time.Instant;

/// What happens to a ride while it is settled.
public sealed interface RideSettlementEvent extends DomainEvent {

    SessionId session();

    RiderId rider();

    /// The fee was charged, or it was PLN 0.00 and nothing was charged (`charged` = false, DE-37).
    record RideSettled(SessionId session, RiderId rider, Money fee, boolean charged, Instant settledAt)
        implements RideSettlementEvent {
    }

    /// Attempt 1 or 2 failed; the next one is due 5 minutes later (DE-35).
    record ChargeAttemptFailed(SessionId session, RiderId rider, Money fee, int attemptNumber,
                               ChargeFailureReason reason, Instant failedAt, Instant nextAttemptDueAt)
        implements RideSettlementEvent {
    }

    /// The 3rd attempt failed: the fee is unpaid and becomes the rider's outstanding debt (DE-35).
    record RideFeeUnpaid(SessionId session, RiderId rider, Money fee, ChargeFailureReason lastReason, Instant unpaidAt)
        implements RideSettlementEvent {
    }
}
