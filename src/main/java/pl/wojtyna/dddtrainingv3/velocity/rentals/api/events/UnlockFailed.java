package pl.wojtyna.dddtrainingv3.velocity.rentals.api.events;

import org.jmolecules.ddd.annotation.ValueObject;
import pl.wojtyna.dddtrainingv3.common.integration.IntegrationEvent;

import java.time.Instant;

/// Published language: a bike's lock didn't open; the reservation was released. `holderType` is `RIDER` or `TECHNICIAN`.
/// See `docs/bounded-contexts/velocity/integration-contracts.md`.
@ValueObject
public record UnlockFailed(String bikeId, String holderType, String holderId, Instant failedAt) implements IntegrationEvent {
}
