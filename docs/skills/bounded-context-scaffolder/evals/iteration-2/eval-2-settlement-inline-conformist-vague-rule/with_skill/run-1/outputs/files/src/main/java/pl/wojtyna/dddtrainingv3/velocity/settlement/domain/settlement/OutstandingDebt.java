package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement;

import org.jmolecules.ddd.annotation.ValueObject;
import org.joda.money.Money;

import java.util.Objects;

/// The unpaid amount left after all Charge Attempts fail.
@ValueObject
public record OutstandingDebt(Money amount) {

    public OutstandingDebt {
        Objects.requireNonNull(amount, "amount");
        if (amount.isNegative()) {
            throw new IllegalArgumentException("Outstanding debt cannot be negative");
        }
    }
}
