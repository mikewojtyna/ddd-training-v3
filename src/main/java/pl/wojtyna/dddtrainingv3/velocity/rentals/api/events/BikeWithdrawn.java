package pl.wojtyna.dddtrainingv3.velocity.rentals.api.events;

import org.jmolecules.ddd.annotation.ValueObject;
import pl.wojtyna.dddtrainingv3.common.integration.IntegrationEvent;

import java.time.Instant;

/// Published language: a bike is no longer offered to riders. `reason` is one of `FAULT_REPORTED_AT_RETURN`, `LOCK_FAILED`, `FAULT_SIGNALLED`, `NOT_RETURNED`, `REQUESTED`.
/// See `docs/bounded-contexts/velocity/integration-contracts.md`.
@ValueObject
public record BikeWithdrawn(String bikeId, String reason, Instant withdrawnAt) implements IntegrationEvent {
}
