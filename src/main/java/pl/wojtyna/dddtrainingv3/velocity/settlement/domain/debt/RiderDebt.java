package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.debt;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.joda.money.Money;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;
import pl.wojtyna.dddtrainingv3.common.domain.Version;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.ChargeOutcome;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.FluentLanguage.As;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.FluentLanguage.At;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.FluentLanguage.Of;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.Pln;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.RiderId;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.ride.SessionId;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/// A rider's outstanding debt: the sum of the fees of their unpaid rides. Guards: the debt is exactly the sum of the
/// unpaid rides, a collection charges all of it at once and clears only what it charged, and a rider without debt
/// has nothing to pay off.
@AggregateRoot
public class RiderDebt {

    private final RiderId rider;
    private final Version version; // the version this debt was created or loaded at
    private final Map<SessionId, Money> unpaidRides; // needed by: debt = sum of unpaid rides; a collection covers them

    private RiderDebt(RiderId rider, Version version, Map<SessionId, Money> unpaidRides) {
        this.rider = rider;
        this.version = version;
        this.unpaidRides = new LinkedHashMap<>(unpaidRides);
    }

    public static RiderDebt none(RiderId rider) {
        return new RiderDebt(rider, Version.initial(), Map.of());
    }

    /// `debt.addUnpaidFee(session).of(amount).at(now)`
    public Of<Money, At<Instant, DomainEvents>> addUnpaidFee(SessionId session) {
        return amount -> at -> {
            unpaidRides.put(session, amount);
            return DomainEvents.of(new RiderDebtEvent.RentalFeeUnpaid(rider, session, amount, outstandingDebt(), at));
        };
    }

    /// The whole outstanding debt, to be charged at once.
    public DebtCollection collection() {
        if (unpaidRides.isEmpty()) {
            throw new NoOutstandingDebtException("Rider " + rider.value() + " has no outstanding debt to pay off");
        }
        return new DebtCollection(rider, unpaidRides.keySet(), outstandingDebt());
    }

    /// `debt.record(collection).as(outcome).at(now)`
    public As<ChargeOutcome, At<Instant, DomainEvents>> record(DebtCollection collection) {
        return outcome -> at -> {
            if (!outcome.succeeded()) {
                return DomainEvents.empty();
            }
            unpaidRides.keySet().removeAll(collection.unpaidRides());
            if (!unpaidRides.isEmpty()) {
                return DomainEvents.empty();
            }
            return DomainEvents.of(new RiderDebtEvent.OutstandingDebtCleared(rider, collection.amount(), at));
        };
    }

    public Money outstandingDebt() {
        return unpaidRides.values().stream().reduce(Pln.zero(), Money::plus);
    }

    @Identity
    public RiderId id() {
        return rider;
    }

    public Version version() {
        return version;
    }

    // --- persistence support (memento), not used by domain logic ---

    public record Snapshot(RiderId rider, Version version, Map<SessionId, Money> unpaidRides) {

        public Snapshot {
            unpaidRides = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(unpaidRides));
        }
    }

    public Snapshot snapshot() {
        return new Snapshot(rider, version, unpaidRides);
    }

    public static RiderDebt restore(Snapshot snapshot) {
        return new RiderDebt(snapshot.rider(), snapshot.version(), snapshot.unpaidRides());
    }
}
