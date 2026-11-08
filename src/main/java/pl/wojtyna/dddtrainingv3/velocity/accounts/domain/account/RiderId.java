package pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account;

import org.jmolecules.ddd.annotation.ValueObject;

/// A rider: a person with an account at VeloCity. Other contexts know the rider only by this id.
@ValueObject
public record RiderId(String value) {

    public RiderId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("RiderId cannot be blank");
        }
    }
}
