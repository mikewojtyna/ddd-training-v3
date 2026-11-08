package pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account;

import org.jmolecules.ddd.annotation.ValueObject;

/// The token of the card the rider adds; Accounts passes it on to Payments and never interprets it.
@ValueObject
public record CardToken(String value) {

    public CardToken {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("CardToken cannot be blank");
        }
    }
}
