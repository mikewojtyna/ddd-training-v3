package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement;

import org.joda.money.Money;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvent;

import java.time.Instant;

public sealed interface SettlementEvent extends DomainEvent {

    SettlementId settlementId();

    /**
     * A charge attempt succeeded; the ride is paid and the settlement is closed.
     */
    record RiderCharged(SettlementId settlementId, RideId rideId, RiderId riderId, Money amount,
                        int attemptNumber, Instant chargedAt) implements SettlementEvent {
    }

    /**
     * A charge attempt failed, and the retry policy allows another one at {@code nextAttemptAt}.
     */
    record ChargeAttemptFailed(SettlementId settlementId, int attemptNumber, String reason,
                               Instant nextAttemptAt) implements SettlementEvent {
    }

    /**
     * The last allowed charge attempt failed; the unpaid amount is now outstanding debt and the settlement is closed.
     */
    record DebtRecorded(SettlementId settlementId, RideId rideId, RiderId riderId, OutstandingDebt debt,
                        int attemptsMade, Instant recordedAt) implements SettlementEvent {
    }
}
