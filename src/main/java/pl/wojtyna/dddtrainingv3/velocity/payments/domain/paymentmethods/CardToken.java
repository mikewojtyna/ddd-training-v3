package pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods;

import org.jmolecules.ddd.annotation.ValueObject;

/// The processor's token for a card; VeloCity holds no card details.
@ValueObject
public record CardToken(String value) {

    public CardToken {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("CardToken cannot be blank");
        }
    }
}
