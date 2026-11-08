package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging;

import org.jmolecules.ddd.annotation.ValueObject;
import org.joda.money.Money;

/// A charge Settlement wants made: an amount taken from the rider's card, for a reference (a ride's session id, or a
/// debt collection).
@ValueObject
public record Charge(RiderId rider, Money amount, String reference) {

    public Charge {
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("A charge needs a positive amount, got " + amount);
        }
    }
}
