package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff;

import org.jmolecules.ddd.annotation.ValueObject;

/// Identifier of a tariff, chosen by the pricing manager (e.g. `T-2`).
@ValueObject
public record TariffId(String value) {

    public TariffId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Tariff id cannot be blank");
        }
    }
}
