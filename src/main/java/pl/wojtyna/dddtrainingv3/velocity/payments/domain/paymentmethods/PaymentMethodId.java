package pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods;

import org.jmolecules.ddd.annotation.ValueObject;

/// A rider's payment method (a card), as VeloCity refers to it.
@ValueObject
public record PaymentMethodId(String value) {

    public PaymentMethodId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("PaymentMethodId cannot be blank");
        }
    }
}
