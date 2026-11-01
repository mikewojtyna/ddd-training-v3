package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.joda.money.Money;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;
import pl.wojtyna.dddtrainingv3.velocity.payments.api.usecases.ChargeResult;
import pl.wojtyna.dddtrainingv3.velocity.payments.api.usecases.ChargeRider;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement.FluentLanguage.At;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The process of collecting money for one priced ride. It ends either when a charge attempt succeeds or when the
 * last allowed attempt fails and the unpaid amount becomes outstanding debt. Either way, every ride ends up settled.
 * <p>
 * Settlement conforms to Payments, so it speaks Payments' {@link ChargeRider} and {@link ChargeResult} directly.
 */
@AggregateRoot
public class Settlement {

    private final SettlementId id;
    private final RideId rideId;
    private final RiderId riderId;
    private final Money amount;
    private final ChargeRetryPolicy retryPolicy;
    private final List<ChargeAttempt> attempts = new ArrayList<>();
    private Status status = Status.AWAITING_CHARGE;
    private Instant nextAttemptAt;

    private Settlement(RideId rideId, RiderId riderId, Money amount, Instant pricedAt,
                       ChargeRetryPolicy retryPolicy) {
        this.id = SettlementId.forRide(rideId);
        this.rideId = rideId;
        this.riderId = riderId;
        this.amount = amount;
        this.retryPolicy = retryPolicy;
        this.nextAttemptAt = pricedAt;
    }

    /**
     * Opens the settlement of a priced ride. The first charge attempt is due immediately.
     */
    public static Settlement open(RideId rideId, RiderId riderId, Money amount, Instant pricedAt,
                                  ChargeRetryPolicy retryPolicy) {
        Objects.requireNonNull(rideId, "rideId");
        Objects.requireNonNull(riderId, "riderId");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(pricedAt, "pricedAt");
        Objects.requireNonNull(retryPolicy, "retryPolicy");
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Only rides with a positive fee need settling");
        }
        return new Settlement(rideId, riderId, amount, pricedAt, retryPolicy);
    }

    public boolean isDueForChargeAttempt(Instant now) {
        return !isClosed() && !now.isBefore(nextAttemptAt);
    }

    /**
     * The command to send to Payments for the next charge attempt.
     */
    public ChargeRider nextChargeAttempt() {
        ensureOpen();
        return new ChargeRider(riderId.value(), id.value() + "/attempt-" + (attempts.size() + 1), amount);
    }

    /**
     * Records the outcome of a charge attempt: {@code settlement.record(result).at(attemptedAt)}.
     */
    public At<Instant, DomainEvents> record(ChargeResult result) {
        Objects.requireNonNull(result, "result");
        return attemptedAt -> {
            ensureOpen();
            if (attemptedAt.isBefore(nextAttemptAt)) {
                throw new ChargeAttemptNotDueException(
                    "Next charge attempt of " + id.value() + " is due at " + nextAttemptAt);
            }
            var attempt = new ChargeAttempt(attempts.size() + 1, attemptedAt, result);
            attempts.add(attempt);
            return switch (result) {
                case ChargeResult.Succeeded succeeded -> {
                    status = Status.CHARGED;
                    nextAttemptAt = null;
                    yield DomainEvents.of(new SettlementEvent.RiderCharged(id, rideId, riderId, amount,
                                                                           attempt.number(), attemptedAt));
                }
                case ChargeResult.Failed failed when retryPolicy.allowsAnotherAttemptAfter(attempts.size()) -> {
                    nextAttemptAt = retryPolicy.nextAttemptAfter(attemptedAt);
                    yield DomainEvents.of(new SettlementEvent.ChargeAttemptFailed(id, attempt.number(),
                                                                                  failed.reason(), nextAttemptAt));
                }
                case ChargeResult.Failed failed -> {
                    status = Status.DEBT_RECORDED;
                    nextAttemptAt = null;
                    yield DomainEvents.of(new SettlementEvent.DebtRecorded(id, rideId, riderId,
                                                                           new OutstandingDebt(amount),
                                                                           attempts.size(), attemptedAt));
                }
            };
        };
    }

    public SettlementId id() {
        return id;
    }

    public boolean isClosed() {
        return status != Status.AWAITING_CHARGE;
    }

    public Optional<Instant> nextAttemptAt() {
        return Optional.ofNullable(nextAttemptAt);
    }

    public List<ChargeAttempt> attempts() {
        return List.copyOf(attempts);
    }

    private void ensureOpen() {
        if (isClosed()) {
            throw new SettlementAlreadyClosedException("Settlement " + id.value() + " is already closed: " + status);
        }
    }

    private enum Status {
        AWAITING_CHARGE, CHARGED, DEBT_RECORDED
    }
}
