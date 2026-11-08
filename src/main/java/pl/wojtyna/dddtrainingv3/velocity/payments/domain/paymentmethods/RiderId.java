package pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods;

import org.jmolecules.ddd.annotation.ValueObject;

/// A rider, known to Payments only by the id Accounts gave them.
@ValueObject
public record RiderId(String value) {

    public RiderId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("RiderId cannot be blank");
        }
    }
}
