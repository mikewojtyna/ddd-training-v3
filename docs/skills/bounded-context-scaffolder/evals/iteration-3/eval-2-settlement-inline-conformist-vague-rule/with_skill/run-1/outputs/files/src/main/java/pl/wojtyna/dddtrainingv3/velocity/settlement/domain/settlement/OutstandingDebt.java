package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement;

import org.joda.money.Money;
import org.jmolecules.ddd.annotation.ValueObject;

import java.util.Objects;

/// **Outstanding Debt**: the unpaid amount left after all attempts fail.
@ValueObject
public record OutstandingDebt(Money amount) {

    public OutstandingDebt {
        Objects.requireNonNull(amount, "amount");
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Outstanding Debt must be positive");
        }
    }
}
