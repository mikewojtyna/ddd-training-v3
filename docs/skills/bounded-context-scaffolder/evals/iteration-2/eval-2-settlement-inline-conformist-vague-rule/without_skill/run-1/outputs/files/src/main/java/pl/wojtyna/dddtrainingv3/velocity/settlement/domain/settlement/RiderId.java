package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement;

import org.jmolecules.ddd.annotation.ValueObject;

import java.util.Objects;

@ValueObject
public record RiderId(String value) {

    public RiderId {
        Objects.requireNonNull(value, "value");
    }
}
