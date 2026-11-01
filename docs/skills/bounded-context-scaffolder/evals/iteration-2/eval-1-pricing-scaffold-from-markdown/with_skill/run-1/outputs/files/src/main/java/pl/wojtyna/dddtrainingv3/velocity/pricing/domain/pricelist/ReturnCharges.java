package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist;

import org.jmolecules.ddd.annotation.ValueObject;
import org.joda.money.Money;

import java.util.Objects;

/// **Return Charges**: drop-zone or unauthorized-zone charges reported by Rentals.
@ValueObject
public record ReturnCharges(Money amount) {

    public ReturnCharges {
        Objects.requireNonNull(amount, "amount");
        if (amount.isNegative()) {
            throw new IllegalArgumentException("Return charges cannot be negative");
        }
    }

    public static ReturnCharges none() {
        return new ReturnCharges(Money.zero(PriceList.CURRENCY));
    }
}
