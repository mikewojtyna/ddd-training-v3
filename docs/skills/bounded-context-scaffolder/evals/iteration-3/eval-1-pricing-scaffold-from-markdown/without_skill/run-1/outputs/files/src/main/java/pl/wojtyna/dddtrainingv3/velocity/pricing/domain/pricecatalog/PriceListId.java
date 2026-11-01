package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricecatalog;

import org.jmolecules.ddd.annotation.ValueObject;

import java.util.Objects;

@ValueObject
public record PriceListId(String value) {

    public PriceListId {
        Objects.requireNonNull(value, "Price list id is required");
    }
}
