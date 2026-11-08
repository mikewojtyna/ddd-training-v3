package pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account;

import org.jmolecules.ddd.annotation.ValueObject;

/// The phone number the rider gives at the phone step (DE-98) and confirms with the SMS code.
@ValueObject
public record PhoneNumber(String value) {

    public PhoneNumber {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("PhoneNumber cannot be blank");
        }
    }
}
