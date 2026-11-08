package pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident;

import org.jmolecules.ddd.annotation.ValueObject;

/// Reference to a bike, as the rider named it; not checked against Rentals (plan D6).
@ValueObject
public record BikeId(String value) {

    public BikeId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Bike id is required");
        }
    }
}
