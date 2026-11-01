package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelists;

import org.jmolecules.ddd.annotation.ValueObject;

import java.util.Objects;

/// Identifies the rider who pays the total fee.
@ValueObject
public record RiderId(String value) {

    public RiderId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("RiderId cannot be blank");
        }
    }
}
