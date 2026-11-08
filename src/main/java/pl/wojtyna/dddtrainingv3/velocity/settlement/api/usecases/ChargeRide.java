package pl.wojtyna.dddtrainingv3.velocity.settlement.api.usecases;

import java.math.BigDecimal;
import java.time.Instant;

/// *Charge Ride*: a rental session ended; settle it. `endReason` in Rentals' terms (`STATION_RETURN`,
/// `FREE_FLOATING_RETURN`, `LOCK_FAILURE`, `MAX_RENTAL_TIME_EXCEEDED`); `stationId` or the position may be null.
public record ChargeRide(String sessionId,
                         String riderId,
                         Instant startedAt,
                         Instant endedAt,
                         String endReason,
                         String stationId,
                         BigDecimal latitude,
                         BigDecimal longitude) {
}
