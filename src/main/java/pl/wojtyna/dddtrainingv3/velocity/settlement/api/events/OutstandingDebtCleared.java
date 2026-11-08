package pl.wojtyna.dddtrainingv3.velocity.settlement.api.events;

import org.jmolecules.ddd.annotation.ValueObject;
import pl.wojtyna.dddtrainingv3.common.integration.IntegrationEvent;

import java.time.Instant;

/// Published language: the rider's outstanding debt went down to PLN 0.00.
/// See `docs/bounded-contexts/velocity/integration-contracts.md`.
@ValueObject
public record OutstandingDebtCleared(String riderId, Instant occurredAt) implements IntegrationEvent {
}
