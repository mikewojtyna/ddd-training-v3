package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement;

import pl.wojtyna.dddtrainingv3.common.domain.DomainEvent;

public sealed interface SettlementEvent extends DomainEvent {

    /// The ride is paid: a Charge Attempt succeeded (R2).
    record RideSettled(RideId rideId, RiderId riderId) implements SettlementEvent {
    }

    /// All Charge Attempts failed and the unpaid amount is recorded (R1). Published as `api.events.DebtRecorded`.
    record OutstandingDebtRecorded(RideId rideId, RiderId riderId, OutstandingDebt debt) implements SettlementEvent {
    }
}
