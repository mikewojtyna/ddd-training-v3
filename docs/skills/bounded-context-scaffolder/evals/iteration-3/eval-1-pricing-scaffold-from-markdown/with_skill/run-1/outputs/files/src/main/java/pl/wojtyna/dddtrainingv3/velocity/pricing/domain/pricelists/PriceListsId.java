package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelists;

import org.jmolecules.ddd.annotation.ValueObject;

import java.util.Objects;

/// Identity of the `PriceLists` aggregate. VeloCity has one timeline of price lists, `VELOCITY`.
@ValueObject
public record PriceListsId(String value) {

    public static final PriceListsId VELOCITY = new PriceListsId("velocity");

    public PriceListsId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("PriceListsId cannot be blank");
        }
    }
}
