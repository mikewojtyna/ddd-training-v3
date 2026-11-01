package pl.wojtyna.dddtrainingv3.velocity.settlement.api.events;

import org.jmolecules.ddd.annotation.ValueObject;
import pl.wojtyna.dddtrainingv3.common.integration.IntegrationEvent;

import java.math.BigDecimal;

/// Canvas outbound event "Debt Recorded" to DebtRecovery. Published language: primitives and strings only.
@ValueObject
public record DebtRecorded(String rideId, String riderId, BigDecimal amount, String currency) implements IntegrationEvent {
}
