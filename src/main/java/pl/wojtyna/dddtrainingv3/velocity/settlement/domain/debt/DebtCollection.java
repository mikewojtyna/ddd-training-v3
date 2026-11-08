package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.debt;

import org.jmolecules.ddd.annotation.ValueObject;
import org.joda.money.Money;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.Charge;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.RiderId;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.ride.SessionId;

import java.util.Set;

/// Charging the whole outstanding debt at once: the unpaid rides it covers and their sum.
@ValueObject
public record DebtCollection(RiderId rider, Set<SessionId> unpaidRides, Money amount) {

    public DebtCollection {
        unpaidRides = Set.copyOf(unpaidRides);
    }

    /// The charge to ask Payments for, under the given reference.
    public Charge chargeAs(String reference) {
        return new Charge(rider, amount, reference);
    }
}
