package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement;

import org.jmolecules.ddd.annotation.ValueObject;

import java.util.Objects;

/**
 * Exactly one settlement exists per ride, so the id is derived from the ride. This makes receiving the same
 * Ride Priced event twice harmless.
 */
@ValueObject
public record SettlementId(String value) {

    public SettlementId {
        Objects.requireNonNull(value, "value");
    }

    public static SettlementId forRide(RideId rideId) {
        return new SettlementId("settlement-" + rideId.value());
    }
}
