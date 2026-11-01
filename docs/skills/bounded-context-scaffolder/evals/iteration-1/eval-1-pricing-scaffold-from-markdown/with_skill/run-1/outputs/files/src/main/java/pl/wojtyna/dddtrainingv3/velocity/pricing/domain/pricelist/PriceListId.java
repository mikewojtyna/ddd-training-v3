package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist;

import org.jmolecules.ddd.annotation.ValueObject;

import java.util.Objects;

@ValueObject
public record PriceListId(String value) {

    public PriceListId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Price list id cannot be blank");
        }
    }
}
