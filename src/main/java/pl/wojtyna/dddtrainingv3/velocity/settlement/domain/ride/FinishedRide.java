package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.ride;

import org.jmolecules.ddd.annotation.ValueObject;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.RiderId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

/// A finished rental session as Settlement needs it to ask for its fee. The position is known for free-floating
/// returns and lock failures, the station for station returns.
@ValueObject
public record FinishedRide(SessionId session, RiderId rider, Instant startedAt, Instant endedAt, EndReason endReason,
                           Optional<String> stationId, Optional<Position> position) {

    @ValueObject
    public record Position(BigDecimal latitude, BigDecimal longitude) {
    }
}
