package pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account;

import org.jmolecules.ddd.annotation.ValueObject;

import java.util.Optional;

/// Payments' answer to "is this card usable?": verified, with the reference Payments gives the card, or declined.
@ValueObject
public record CardVerification(boolean verified, Optional<PaymentMethodId> paymentMethod) {

    public CardVerification {
        if (verified && paymentMethod.isEmpty()) {
            throw new IllegalArgumentException("A verified card has a payment method reference");
        }
    }

    public static CardVerification verifiedAs(PaymentMethodId paymentMethod) {
        return new CardVerification(true, Optional.of(paymentMethod));
    }

    public static CardVerification declined() {
        return new CardVerification(false, Optional.empty());
    }
}
