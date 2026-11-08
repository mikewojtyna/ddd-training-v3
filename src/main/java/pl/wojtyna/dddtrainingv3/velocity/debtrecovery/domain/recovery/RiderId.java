package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery;

import org.jmolecules.ddd.annotation.ValueObject;

/// A rider, known to Debt Recovery only by the id Accounts gave them. A rider has at most one open recovery process.
@ValueObject
public record RiderId(String value) {

    public RiderId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("RiderId cannot be blank");
        }
    }
}
