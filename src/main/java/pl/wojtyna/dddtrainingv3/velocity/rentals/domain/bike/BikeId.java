package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike;

import org.jmolecules.ddd.annotation.ValueObject;

import java.util.Objects;

@ValueObject
public record BikeId(String value) {

    public BikeId {
        Objects.requireNonNull(value, "bike id");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Bike id cannot be blank");
        }
    }
}
