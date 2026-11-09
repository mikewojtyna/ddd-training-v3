package pl.wojtyna.dddtrainingv3.velocity.rentals.api.events;

import org.jmolecules.ddd.annotation.ValueObject;
import pl.wojtyna.dddtrainingv3.common.integration.IntegrationEvent;

import java.time.Instant;

/// Published language: a reservation ran out before its holder unlocked the bike. `holderType` is `RIDER` or `TECHNICIAN`.
/// See `docs/bounded-contexts/velocity/integration-contracts.md`.
@ValueObject
public record ReservationExpired(String bikeId, String holderType, String holderId, Instant expiredAt) implements IntegrationEvent {
}
