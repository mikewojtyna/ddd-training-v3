package pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident;

import org.jmolecules.ddd.annotation.ValueObject;

/// Identity of an incident.
@ValueObject
public record IncidentId(String value) {

    public IncidentId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Incident id is required");
        }
    }
}
