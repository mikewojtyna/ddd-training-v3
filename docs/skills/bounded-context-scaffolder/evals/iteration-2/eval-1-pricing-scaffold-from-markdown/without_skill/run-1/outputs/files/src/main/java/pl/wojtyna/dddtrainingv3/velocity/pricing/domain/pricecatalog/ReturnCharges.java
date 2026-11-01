package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricecatalog;

import org.jmolecules.ddd.annotation.ValueObject;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;

import java.util.Objects;

/**
 * Drop-zone or unauthorized-zone charges reported by Rentals for a rental session.
 */
@ValueObject
public record ReturnCharges(Money amount) {

    public ReturnCharges {
        Objects.requireNonNull(amount, "Return charges amount is required");
        if (amount.isNegative()) {
            throw new IllegalArgumentException("Return charges cannot be negative");
        }
    }

    public static ReturnCharges none(CurrencyUnit currency) {
        return new ReturnCharges(Money.zero(currency));
    }
}
