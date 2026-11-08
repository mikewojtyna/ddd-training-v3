package pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods;

import org.jmolecules.ddd.annotation.ValueObject;

/// One charge to a rider's card.
@ValueObject
public record ChargeId(String value) {

    public ChargeId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ChargeId cannot be blank");
        }
    }
}
