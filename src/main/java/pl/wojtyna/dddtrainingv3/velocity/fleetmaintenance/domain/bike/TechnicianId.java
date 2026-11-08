package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike;

import org.jmolecules.ddd.annotation.ValueObject;

/// A VeloCity employee who reserves, unlocks and repairs broken bikes.
@ValueObject
public record TechnicianId(String value) {

    public TechnicianId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Technician id is required");
        }
    }
}
