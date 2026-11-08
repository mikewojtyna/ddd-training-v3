package pl.wojtyna.dddtrainingv3.velocity.settlement.api.events;

import org.jmolecules.ddd.annotation.ValueObject;
import pl.wojtyna.dddtrainingv3.common.integration.IntegrationEvent;

import java.math.BigDecimal;
import java.time.Instant;

/// Published language: all charge attempts for a ride failed; its fee became outstanding debt (PLN).
/// See `docs/bounded-contexts/velocity/integration-contracts.md`.
@ValueObject
public record RentalFeeUnpaid(String riderId, String sessionId, BigDecimal amount, BigDecimal outstandingDebt, Instant occurredAt) implements IntegrationEvent {
}
