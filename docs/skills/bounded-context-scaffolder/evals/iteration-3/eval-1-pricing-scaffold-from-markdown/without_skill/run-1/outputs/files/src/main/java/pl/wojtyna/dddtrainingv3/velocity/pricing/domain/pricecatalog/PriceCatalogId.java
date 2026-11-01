package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricecatalog;

import org.jmolecules.ddd.annotation.ValueObject;

import java.util.Objects;

@ValueObject
public record PriceCatalogId(String value) {

    public static final PriceCatalogId VELOCITY = new PriceCatalogId("velocity");

    public PriceCatalogId {
        Objects.requireNonNull(value, "Price catalog id is required");
    }
}
