package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement;

import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.RideId;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.RiderId;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.joda.money.Money;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/// The process of collecting money for one priced ride. One Settlement per priced ride is the consistency boundary
/// for its Charge Attempts and its Outstanding Debt.
///
/// When the 2nd and 3rd Charge Attempt are made ("shortly after the ride") is not modelled yet, see the plan.
@AggregateRoot
public class Settlement {

    /// "Given ... 2 failed charge attempts, when the 3rd charge attempt fails, then an Outstanding Debt is recorded".
    static final int MAX_CHARGE_ATTEMPTS = 3;

    private final RideId rideId;
    private final RiderId riderId;
    private final Money price;
    private final List<ChargeAttempt> chargeAttempts = new ArrayList<>();
    private Status status = Status.COLLECTING;

    private Settlement(RideId rideId, RiderId riderId, Money price) {
        this.rideId = Objects.requireNonNull(rideId, "rideId");
        this.riderId = Objects.requireNonNull(riderId, "riderId");
        this.price = Objects.requireNonNull(price, "price");
    }

    public static Settlement forPricedRide(RideId rideId, RiderId riderId, Money price) {
        return new Settlement(rideId, riderId, price);
    }

    public DomainEvents chargeAttemptFailed() {
        ensureStillCollecting();
        chargeAttempts.add(new ChargeAttempt(nextAttemptNumber(), ChargeAttempt.Outcome.FAILED));
        if (chargeAttempts.size() >= MAX_CHARGE_ATTEMPTS) {
            status = Status.DEBT_RECORDED;
            return DomainEvents.of(new SettlementEvent.DebtRecorded(rideId, riderId, new OutstandingDebt(price)));
        }
        return DomainEvents.of(new SettlementEvent.ChargeAttemptFailed(rideId, chargeAttempts.size()));
    }

    public DomainEvents chargeAttemptSucceeded() {
        ensureStillCollecting();
        chargeAttempts.add(new ChargeAttempt(nextAttemptNumber(), ChargeAttempt.Outcome.SUCCEEDED));
        status = Status.SETTLED;
        return DomainEvents.of(new SettlementEvent.RideSettled(rideId));
    }

    @Identity
    public RideId id() {
        return rideId;
    }

    private int nextAttemptNumber() {
        return chargeAttempts.size() + 1;
    }

    private void ensureStillCollecting() {
        if (status != Status.COLLECTING) {
            // Not a confirmed business rule yet (plan, Q6): what an outcome reported after the ride is settled or the
            // debt is recorded means. Rejected as an illegal state until the rule is agreed.
            throw new IllegalStateException("Settlement of ride " + rideId.value() + " is already " + status);
        }
    }

    private enum Status {
        COLLECTING, SETTLED, DEBT_RECORDED
    }

    /// One try to take money from the rider's payment method. Never changes once made.
    private record ChargeAttempt(int number, Outcome outcome) {

        private enum Outcome {
            FAILED, SUCCEEDED
        }
    }
}
