package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement;

import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.RideId;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.RiderId;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvent;

public sealed interface SettlementEvent extends DomainEvent {

    record ChargeAttemptFailed(RideId rideId, int attemptNumber) implements SettlementEvent {
    }

    record RideSettled(RideId rideId) implements SettlementEvent {
    }

    record DebtRecorded(RideId rideId, RiderId riderId, OutstandingDebt outstandingDebt) implements SettlementEvent {
    }
}
