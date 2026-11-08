package pl.wojtyna.dddtrainingv3.velocity.rentals.api.events;

import org.jmolecules.ddd.annotation.ValueObject;
import pl.wojtyna.dddtrainingv3.common.integration.IntegrationEvent;

import java.math.BigDecimal;
import java.time.Instant;

/// Published language: a rider's rental session ended. `endReason` is one of `STATION_RETURN`, `FREE_FLOATING_RETURN`, `LOCK_FAILURE`, `MAX_RENTAL_TIME_EXCEEDED`; `stationId` is set for station returns, the position for free-floating returns and lock failures, neither when the maximum rental time was exceeded.
/// See `docs/bounded-contexts/velocity/integration-contracts.md`.
@ValueObject
public record RentalSessionEnded(String sessionId, String bikeId, String riderId, Instant startedAt, Instant endedAt, String endReason, String stationId, BigDecimal latitude, BigDecimal longitude) implements IntegrationEvent {
}
