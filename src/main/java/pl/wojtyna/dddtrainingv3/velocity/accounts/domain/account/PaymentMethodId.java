package pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account;

import org.jmolecules.ddd.annotation.ValueObject;

/// The reference Payments gives a verified card: all Accounts knows about the rider's card (plan D7 of
/// `rider-adds-payment-method`).
@ValueObject
public record PaymentMethodId(String value) {

    public PaymentMethodId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("PaymentMethodId cannot be blank");
        }
    }
}
