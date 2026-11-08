package pl.wojtyna.dddtrainingv3.velocity.payments.api.events;

import org.jmolecules.ddd.annotation.ValueObject;
import pl.wojtyna.dddtrainingv3.common.integration.IntegrationEvent;

import java.time.Instant;

/// Published language: the processor reported the rider's current card as no longer usable (expired, cancelled, reported lost).
/// See `docs/bounded-contexts/velocity/integration-contracts.md`.
@ValueObject
public record PaymentMethodInvalidated(String riderId, String paymentMethodId, String reason, Instant occurredAt) implements IntegrationEvent {
}
