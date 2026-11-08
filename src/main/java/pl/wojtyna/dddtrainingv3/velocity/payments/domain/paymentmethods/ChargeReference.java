package pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods;

import org.jmolecules.ddd.annotation.ValueObject;

/// What a charge is for, in the charging context's words (Settlement's ride).
@ValueObject
public record ChargeReference(String value) {

    public ChargeReference {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ChargeReference cannot be blank");
        }
    }
}
