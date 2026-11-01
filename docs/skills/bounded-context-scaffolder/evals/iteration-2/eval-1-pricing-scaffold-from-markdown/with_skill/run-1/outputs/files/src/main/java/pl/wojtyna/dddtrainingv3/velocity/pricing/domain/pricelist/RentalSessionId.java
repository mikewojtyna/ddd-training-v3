package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist;

import org.jmolecules.ddd.annotation.ValueObject;

import java.util.Objects;

/// Identifies the rental session (owned by Rentals) that a ride is priced for.
@ValueObject
public record RentalSessionId(String value) {

    public RentalSessionId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Rental session id cannot be blank");
        }
    }
}
