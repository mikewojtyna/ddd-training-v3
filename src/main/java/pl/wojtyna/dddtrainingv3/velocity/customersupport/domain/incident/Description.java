package pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident;

import org.jmolecules.ddd.annotation.ValueObject;

/// The rider's own words about the incident.
@ValueObject
public record Description(String value) {

    public Description {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Description is required");
        }
    }
}
