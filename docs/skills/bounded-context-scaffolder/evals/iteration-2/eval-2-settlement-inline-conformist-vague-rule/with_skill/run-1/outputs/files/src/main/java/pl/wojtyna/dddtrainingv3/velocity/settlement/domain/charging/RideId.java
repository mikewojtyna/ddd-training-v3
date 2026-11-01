package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging;

import org.jmolecules.ddd.annotation.ValueObject;

import java.util.Objects;

@ValueObject
public record RideId(String value) {

    public RideId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Ride id cannot be blank");
        }
    }
}
