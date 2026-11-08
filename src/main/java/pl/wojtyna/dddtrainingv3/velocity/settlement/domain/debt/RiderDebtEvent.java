package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.debt;

import org.joda.money.Money;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvent;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.RiderId;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.ride.SessionId;

import java.time.Instant;

/// What happens to a rider's outstanding debt.
public sealed interface RiderDebtEvent extends DomainEvent {

    RiderId rider();

    /// A ride's fee stayed unpaid after all charge attempts and was added to the rider's outstanding debt.
    record RentalFeeUnpaid(RiderId rider, SessionId session, Money amount, Money outstandingDebt, Instant occurredAt)
        implements RiderDebtEvent {
    }

    /// The rider's outstanding debt went down to PLN 0.00.
    record OutstandingDebtCleared(RiderId rider, Money amountPaid, Instant clearedAt) implements RiderDebtEvent {
    }
}
