package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelists;

import org.jmolecules.ddd.annotation.ValueObject;

import java.util.Objects;

/// Identifies the rental session (the ride) being priced. Rides and rental sessions are synonyms in Pricing.
@ValueObject
public record RentalSessionId(String value) {

    public RentalSessionId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("RentalSessionId cannot be blank");
        }
    }
}
