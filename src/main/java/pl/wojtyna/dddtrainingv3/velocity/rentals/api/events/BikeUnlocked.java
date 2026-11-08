package pl.wojtyna.dddtrainingv3.velocity.rentals.api.events;

import org.jmolecules.ddd.annotation.ValueObject;
import pl.wojtyna.dddtrainingv3.common.integration.IntegrationEvent;

import java.time.Instant;

/// Published language: a bike's lock opened for its reservation holder. `holderType` is `RIDER` or `TECHNICIAN`.
/// See `docs/bounded-contexts/velocity/integration-contracts.md`.
@ValueObject
public record BikeUnlocked(String bikeId, String holderType, String holderId, Instant unlockedAt) implements IntegrationEvent {
}
