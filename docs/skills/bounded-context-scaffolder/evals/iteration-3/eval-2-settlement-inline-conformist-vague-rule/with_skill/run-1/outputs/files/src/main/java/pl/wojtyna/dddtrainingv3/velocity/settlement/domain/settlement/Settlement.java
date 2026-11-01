package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement;

import org.joda.money.Money;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/// **Settlement**: the process of collecting money for one priced ride. One Settlement per ride, identified by the ride.
///
/// It holds the ride's Charge Attempts and decides, after each one, whether the ride is settled (R2) or, when the
/// last allowed attempt fails, records Outstanding Debt equal to the ride price (R1).
@AggregateRoot
public class Settlement {

    /// R1: "with 2 failed charge attempts, when the 3rd charge attempt fails, then an Outstanding Debt is recorded".
    static final int MAX_CHARGE_ATTEMPTS = 3;

    private final RideId rideId;
    private final RiderId riderId;
    private final Money price;
    private final List<ChargeAttempt> attempts = new ArrayList<>();
    private Status status = Status.OPEN;

    public Settlement(RideId rideId, RiderId riderId, Money price) {
        this.rideId = Objects.requireNonNull(rideId, "rideId");
        this.riderId = Objects.requireNonNull(riderId, "riderId");
        this.price = Objects.requireNonNull(price, "price");
        if (!price.isPositive()) {
            throw new IllegalArgumentException("A priced ride to settle must have a positive price");
        }
    }

    public DomainEvents chargeAttemptSucceeded() {
        record(ChargeAttempt.Outcome.SUCCEEDED);
        status = Status.SETTLED;
        return DomainEvents.of(new SettlementEvent.RideSettled(rideId, riderId));
    }

    public DomainEvents chargeAttemptFailed() {
        record(ChargeAttempt.Outcome.FAILED);
        if (failedAttempts() < MAX_CHARGE_ATTEMPTS) {
            return DomainEvents.empty();
        }
        status = Status.DEBT_RECORDED;
        return DomainEvents.of(new SettlementEvent.OutstandingDebtRecorded(rideId, riderId, new OutstandingDebt(price)));
    }

    @Identity
    public RideId id() {
        return rideId;
    }

    private void record(ChargeAttempt.Outcome outcome) {
        // What a finished Settlement does with another Charge Attempt is not a confirmed business rule yet
        // (awaiting GWT, see the plan). Until then it's guarded as a programming error, not modelled.
        if (status != Status.OPEN) {
            throw new IllegalStateException("Settlement for ride " + rideId.value() + " is already " + status);
        }
        attempts.add(new ChargeAttempt(attempts.size() + 1, outcome));
    }

    private long failedAttempts() {
        return attempts.stream().filter(ChargeAttempt::failed).count();
    }

    private enum Status {
        OPEN,
        SETTLED,
        DEBT_RECORDED
    }
}
