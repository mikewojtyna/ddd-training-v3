package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist;

import org.jmolecules.ddd.annotation.ValueObject;
import org.joda.money.Money;

import java.util.Objects;

/// **Total Fee**: sum of billing period fees plus return charges.
@ValueObject
public record TotalFee(Money amount) {

    public TotalFee {
        Objects.requireNonNull(amount, "amount");
        if (amount.isNegative()) {
            throw new IllegalArgumentException("Total fee cannot be negative");
        }
    }
}
