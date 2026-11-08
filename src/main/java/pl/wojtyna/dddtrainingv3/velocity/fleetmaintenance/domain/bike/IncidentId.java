package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike;

import org.jmolecules.ddd.annotation.ValueObject;

/// A fleet-issue incident in Customer Support, linked to the broken bike it is about (*incident link*).
@ValueObject
public record IncidentId(String value) {

    public IncidentId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Incident id is required");
        }
    }
}
