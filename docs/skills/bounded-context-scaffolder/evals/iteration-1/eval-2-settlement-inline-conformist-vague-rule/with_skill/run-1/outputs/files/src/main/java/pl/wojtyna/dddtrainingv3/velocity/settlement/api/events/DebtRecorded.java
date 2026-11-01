package pl.wojtyna.dddtrainingv3.velocity.settlement.api.events;

import org.jmolecules.ddd.annotation.ValueObject;
import pl.wojtyna.dddtrainingv3.common.integration.IntegrationEvent;

import java.math.BigDecimal;

/// Published language for the canvas outbound event **Debt Recorded** (to DebtRecovery): the Outstanding Debt left
/// for one ride after all Charge Attempts failed.
///
/// Nothing publishes it yet. It will be emitted once the retry-then-record-debt rule is confirmed as a strict
/// Given/When/Then example and modelled in a Settlement aggregate.
@ValueObject
public record DebtRecorded(String rideId, String riderId, BigDecimal outstandingDebt, String currency)
    implements IntegrationEvent {
}
