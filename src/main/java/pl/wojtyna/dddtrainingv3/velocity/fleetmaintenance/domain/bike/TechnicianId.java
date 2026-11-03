package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike;

import org.jmolecules.ddd.annotation.ValueObject;

import java.util.Objects;

/// A VeloCity employee who reserves and unlocks bikes in order to repair them.
@ValueObject
public record TechnicianId(String value) {

    public TechnicianId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Technician id cannot be blank");
        }
    }
}
