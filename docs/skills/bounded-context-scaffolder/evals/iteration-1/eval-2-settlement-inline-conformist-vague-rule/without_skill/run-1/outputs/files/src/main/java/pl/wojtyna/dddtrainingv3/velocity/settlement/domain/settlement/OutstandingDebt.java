package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement;

import org.jmolecules.ddd.annotation.ValueObject;
import org.joda.money.Money;

import java.util.Objects;

/**
 * The unpaid amount left after all charge attempts for a ride have failed.
 */
@ValueObject
public record OutstandingDebt(Money amount) {

    public OutstandingDebt {
        Objects.requireNonNull(amount, "amount");
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Outstanding debt must be positive");
        }
    }
}
