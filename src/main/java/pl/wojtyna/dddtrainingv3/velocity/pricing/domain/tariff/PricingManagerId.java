package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff;

import org.jmolecules.ddd.annotation.ValueObject;

/// The pricing manager who published a tariff. There are no identities yet, so a publication without a named
/// publisher is recorded as {@link #unknown()}.
@ValueObject
public record PricingManagerId(String value) {

    private static final String UNKNOWN = "unknown";

    public PricingManagerId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Pricing manager id cannot be blank");
        }
    }

    public static PricingManagerId unknown() {
        return new PricingManagerId(UNKNOWN);
    }

    public static PricingManagerId ofNullable(String value) {
        return value == null || value.isBlank() ? unknown() : new PricingManagerId(value);
    }
}
