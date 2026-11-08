package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike;

import org.jmolecules.ddd.annotation.ValueObject;

/// What is wrong with the bike, in the reporter's words (or the bike's fault code).
@ValueObject
public record FaultDescription(String value) {

    public FaultDescription {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Fault description is required");
        }
    }
}
