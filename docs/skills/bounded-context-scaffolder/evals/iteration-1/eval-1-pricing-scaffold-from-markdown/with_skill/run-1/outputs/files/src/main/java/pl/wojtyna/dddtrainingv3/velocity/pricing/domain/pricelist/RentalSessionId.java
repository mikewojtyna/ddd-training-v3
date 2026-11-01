package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist;

import org.jmolecules.ddd.annotation.ValueObject;

import java.util.Objects;

/// Identifier of the rental session a ride belongs to, as carried by Rentals' "Rental Session Ended" event.
@ValueObject
public record RentalSessionId(String value) {

    public RentalSessionId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Rental session id cannot be blank");
        }
    }
}
