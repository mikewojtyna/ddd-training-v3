package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.ride;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.joda.money.Money;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;
import pl.wojtyna.dddtrainingv3.common.domain.Version;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.Charge;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.ChargeFailureReason;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.ChargeOutcome;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.FluentLanguage.At;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.FluentLanguage.PricedAt;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.FluentLanguage.RiddenBy;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.RiderId;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/// The settlement of one ride. Guards: a ride is charged its fee at most until it is settled (DE-95), a free ride is
/// never charged (DE-37), and a failed charge is attempted again 5 minutes later, 3 attempts in all (DE-35).
@AggregateRoot
public class RideSettlement {

    private static final Duration RETRY_DELAY = Duration.ofMinutes(5); // DE-35
    private static final int MAX_ATTEMPTS = 3; // DE-35

    private final SessionId session;
    private final RiderId rider;
    private final Money fee;
    private final Version version; // the version this ride was created or loaded at
    private RideStatus status;
    private int failedAttempts;
    private Instant nextAttemptDueAt;
    private ChargeFailureReason lastFailureReason;

    private RideSettlement(SessionId session, RiderId rider, Money fee, RideStatus status) {
        this(session, rider, fee, Version.initial(), status, 0, null, null);
    }

    private RideSettlement(SessionId session, RiderId rider, Money fee, Version version, RideStatus status,
                           int failedAttempts, Instant nextAttemptDueAt, ChargeFailureReason lastFailureReason) {
        this.session = session;
        this.rider = rider;
        this.fee = fee;
        this.version = version;
        this.status = status;
        this.failedAttempts = failedAttempts;
        this.nextAttemptDueAt = nextAttemptDueAt;
        this.lastFailureReason = lastFailureReason;
    }

    /// A finished ride whose fee is known: `RideSettlement.of(session).riddenBy(rider).pricedAt(fee).at(now)`.
    public static RiddenBy<RiderId, PricedAt<Money, At<Instant, PricedRide>>> of(SessionId session) {
        return rider -> fee -> pricedAt -> {
            if (fee.isZero()) {
                return new PricedRide(new RideSettlement(session, rider, fee, RideStatus.SETTLED),
                                      DomainEvents.of(new RideSettlementEvent.RideSettled(session, rider, fee, false, pricedAt)));
            }
            return new PricedRide(new RideSettlement(session, rider, fee, RideStatus.PENDING), DomainEvents.empty());
        };
    }

    public record PricedRide(RideSettlement ride, DomainEvents events) {
    }

    /// The charge to make now, if one is due.
    public Optional<Charge> chargeDueAt(Instant now) {
        if (status != RideStatus.PENDING || (nextAttemptDueAt != null && now.isBefore(nextAttemptDueAt))) {
            return Optional.empty();
        }
        return Optional.of(new Charge(rider, fee, session.value()));
    }

    /// Records what Payments answered to the charge attempt made at the given moment.
    public At<Instant, DomainEvents> recordCharge(ChargeOutcome outcome) {
        return at -> {
            if (!outcome.succeeded()) {
                failedAttempts++;
                lastFailureReason = outcome.failureReason().orElseThrow();
                if (failedAttempts == MAX_ATTEMPTS) {
                    status = RideStatus.UNPAID;
                    nextAttemptDueAt = null;
                    return DomainEvents.of(new RideSettlementEvent.RideFeeUnpaid(session, rider, fee, lastFailureReason, at));
                }
                nextAttemptDueAt = at.plus(RETRY_DELAY);
                return DomainEvents.of(new RideSettlementEvent.ChargeAttemptFailed(session, rider, fee, failedAttempts,
                                                                                   lastFailureReason,
                                                                                   at, nextAttemptDueAt));
            }
            status = RideStatus.SETTLED;
            return DomainEvents.of(new RideSettlementEvent.RideSettled(session, rider, fee, true, at));
        };
    }

    @Identity
    public SessionId id() {
        return session;
    }

    public Version version() {
        return version;
    }

    // --- persistence support (memento), not used by domain logic ---

    public record Snapshot(SessionId session, RiderId rider, Money fee, Version version, RideStatus status,
                           int failedAttempts, Instant nextAttemptDueAt, ChargeFailureReason lastFailureReason) {

        public Snapshot withVersion(Version newVersion) {
            return new Snapshot(session, rider, fee, newVersion, status, failedAttempts, nextAttemptDueAt, lastFailureReason);
        }
    }

    public Snapshot snapshot() {
        return new Snapshot(session, rider, fee, version, status, failedAttempts, nextAttemptDueAt, lastFailureReason);
    }

    public static RideSettlement restore(Snapshot snapshot) {
        return new RideSettlement(snapshot.session(), snapshot.rider(), snapshot.fee(), snapshot.version(),
                                  snapshot.status(), snapshot.failedAttempts(), snapshot.nextAttemptDueAt(),
                                  snapshot.lastFailureReason());
    }
}
