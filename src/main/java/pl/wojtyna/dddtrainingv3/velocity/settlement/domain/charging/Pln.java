package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;

/// Settlement moves money only in PLN.
public final class Pln {

    public static final CurrencyUnit PLN = CurrencyUnit.of("PLN");

    private Pln() {
    }

    public static Money pln(String amount) {
        return pln(new BigDecimal(amount));
    }

    public static Money pln(BigDecimal amount) {
        return Money.of(PLN, amount, RoundingMode.UNNECESSARY);
    }

    public static Money zero() {
        return Money.zero(PLN);
    }
}
