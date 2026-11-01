package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricecatalog;

import org.jmolecules.ddd.annotation.ValueObject;

import java.util.Objects;

@ValueObject
public record RentalSessionId(String value) {

    public RentalSessionId {
        Objects.requireNonNull(value, "Rental session id is required");
    }
}
