package pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident;

import org.jmolecules.ddd.annotation.ValueObject;

/// How an incident was resolved: the agent's words, the team's outcome, or one of the automatic resolutions.
@ValueObject
public record Resolution(String value) {

    /// DE-87: a rejection nobody contested within the reopening window.
    public static final Resolution REJECTION_NOT_CONTESTED = new Resolution("rejection not contested");

    /// DE-55: Fleet Maintenance fixed the incident's bike.
    public static final Resolution BIKE_FIXED = new Resolution("bike fixed");

    public Resolution {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Resolution is required");
        }
    }
}
