package pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident;

import org.jmolecules.ddd.annotation.ValueObject;

/// The rider who reported an incident.
@ValueObject
public record RiderId(String value) {

    public RiderId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Rider id is required");
        }
    }
}
