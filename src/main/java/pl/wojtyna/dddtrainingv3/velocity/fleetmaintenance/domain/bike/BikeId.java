package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike;

import org.jmolecules.ddd.annotation.ValueObject;

/// Identity of a bike, as the whole fleet knows it.
@ValueObject
public record BikeId(String value) {

    public BikeId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Bike id is required");
        }
    }
}
