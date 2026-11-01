package pl.wojtyna.dddtrainingv3.velocity.settlement.api.events;

import org.jmolecules.ddd.annotation.ValueObject;
import pl.wojtyna.dddtrainingv3.common.integration.IntegrationEvent;

import java.math.BigDecimal;

/// Published language: canvas outbound event "Debt Recorded" to DebtRecovery.
/// Carries primitives and strings only, so Settlement's domain types don't leak to consumers.
@ValueObject
public record DebtRecorded(String rideId, String riderId, BigDecimal amount, String currency)
    implements IntegrationEvent {
}
