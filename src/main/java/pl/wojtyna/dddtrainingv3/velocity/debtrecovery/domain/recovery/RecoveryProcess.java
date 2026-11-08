package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.joda.money.Money;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;
import pl.wojtyna.dddtrainingv3.common.domain.Version;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.FluentLanguage.As;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.FluentLanguage.At;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.FluentLanguage.By;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/// The rider's recovery process: the pursuit of one rider's whole outstanding debt. Identified by the rider, because
/// a rider has at most one open recovery process; after success or exhaustion a new unpaid fee starts a new process
/// in the same place (DE-88), as no rule reads an ended process's history.
///
/// Guards: attempts are due at once on start, then 72 hours after each failed attempt, and only while running; the
/// 5th failed attempt exhausts it; a collected or cleared debt makes it succeed (also after exhaustion, once); only a
/// running recovery can be paused and only a paused one resumed (due at once, count kept); a further unpaid fee joins
/// an open recovery.
@AggregateRoot
public class RecoveryProcess {

    private static final Duration BETWEEN_ATTEMPTS = Duration.ofHours(72); // DE-40, DE-90: to the minute
    private static final int MAX_FAILED_ATTEMPTS = 5; // DE-41

    private final RiderId rider;
    private final Version version; // the version this process was created or loaded at
    private RecoveryStatus status; // needed by: open joins / ended restarts; due only while running; pause / resume
    private int failedAttempts; // needed by: exhausted after 5; count kept across pause and join
    private Instant nextAttemptDueAt; // needed by: due at once, then 72 hours after a failed attempt
    private Money outstandingDebt; // needed by: an attempt asks for the whole outstanding debt
    private PauseReason pauseReason; // needed by: a paused recovery stays paused for its reason when a fee joins
    private Instant startedAt; // needed by: a clearance from before the recovery started doesn't end it

    private RecoveryProcess(RiderId rider, Version version, RecoveryStatus status, int failedAttempts,
                            Instant nextAttemptDueAt, Money outstandingDebt, PauseReason pauseReason,
                            Instant startedAt) {
        this.rider = rider;
        this.version = version;
        this.status = status;
        this.failedAttempts = failedAttempts;
        this.nextAttemptDueAt = nextAttemptDueAt;
        this.outstandingDebt = outstandingDebt;
        this.pauseReason = pauseReason;
        this.startedAt = startedAt;
    }

    /// A rider without a recovery process.
    public static RecoveryProcess none(RiderId rider) {
        return new RecoveryProcess(rider, Version.initial(), RecoveryStatus.NONE, 0, null, null, null, null);
    }

    /// `process.unpaidFee(outstandingDebt).at(unpaidAt)`
    public At<Instant, DomainEvents> unpaidFee(Money outstandingDebt) {
        return unpaidAt -> {
            this.outstandingDebt = outstandingDebt;
            if (status.isOpen()) {
                return DomainEvents.empty();
            }
            status = RecoveryStatus.RUNNING;
            startedAt = unpaidAt;
            failedAttempts = 0;
            nextAttemptDueAt = unpaidAt;
            this.outstandingDebt = outstandingDebt;
            return DomainEvents.of(new RecoveryEvent.RecoveryStarted(rider, outstandingDebt, unpaidAt, unpaidAt));
        };
    }

    public Optional<CollectionAttempt> attemptDueAt(Instant now) {
        if (status != RecoveryStatus.RUNNING || now.isBefore(nextAttemptDueAt)) {
            return Optional.empty();
        }
        return Optional.of(new CollectionAttempt(rider, failedAttempts + 1, outstandingDebt, now));
    }

    /// `process.record(attempt).as(outcome)`
    public As<CollectionOutcome, DomainEvents> record(CollectionAttempt attempt) {
        return outcome -> {
            if (status != RecoveryStatus.RUNNING) { // paused or ended while the attempt was in flight (plan D6)
                return outcome.debtCleared()
                       ? DomainEvents.of(attempted(attempt, true)).append(debtCleared(attempt.attemptedAt()))
                       : DomainEvents.empty();
            }
            if (outcome.debtCleared()) {
                status = RecoveryStatus.SUCCEEDED;
                nextAttemptDueAt = null;
                return DomainEvents.of(attempted(attempt, true),
                                       new RecoveryEvent.RecoverySucceeded(rider, attempt.attemptedAt(), failedAttempts));
            }
            failedAttempts++;
            if (failedAttempts == MAX_FAILED_ATTEMPTS) {
                status = RecoveryStatus.EXHAUSTED;
                nextAttemptDueAt = null;
                return DomainEvents.of(attempted(attempt, false),
                                       new RecoveryEvent.RecoveryExhausted(rider, attempt.attemptedAt(), outstandingDebt));
            }
            nextAttemptDueAt = attempt.attemptedAt().plus(BETWEEN_ATTEMPTS);
            return DomainEvents.of(attempted(attempt, false));
        };
    }

    private RecoveryEvent.CollectionAttempted attempted(CollectionAttempt attempt, boolean collected) {
        return new RecoveryEvent.CollectionAttempted(rider, attempt.attemptNumber(), attempt.amountAsked(), collected,
                                                     attempt.attemptedAt(), nextAttemptDueAt);
    }

    /// The rider's outstanding debt was cleared: an open or exhausted recovery succeeded (DE-45); an already
    /// succeeded one stays as it is.
    public DomainEvents debtCleared(Instant clearedAt) {
        if (status == RecoveryStatus.NONE || status == RecoveryStatus.SUCCEEDED || clearedAt.isBefore(startedAt)) {
            return DomainEvents.empty();
        }
        status = RecoveryStatus.SUCCEEDED;
        nextAttemptDueAt = null;
        pauseReason = null;
        return DomainEvents.of(new RecoveryEvent.RecoverySucceeded(rider, clearedAt, failedAttempts));
    }

    /// `process.pause(reason).by(agent).at(now)`
    public By<AgentId, At<Instant, DomainEvents>> pause(PauseReason reason) {
        return agent -> now -> {
            if (status != RecoveryStatus.RUNNING) {
                throw new RecoveryNotRunningException("Only a running recovery can be paused; rider " + rider.value()
                                                      + "'s recovery is " + status);
            }
            status = RecoveryStatus.PAUSED;
            pauseReason = reason;
            nextAttemptDueAt = null;
            return DomainEvents.of(new RecoveryEvent.RecoveryPaused(rider, reason, agent, now, failedAttempts));
        };
    }

    /// `process.resume().by(agent).at(now)`
    public By<AgentId, At<Instant, DomainEvents>> resume() {
        return agent -> now -> {
            if (status != RecoveryStatus.PAUSED) {
                throw new RecoveryNotPausedException("Only a paused recovery can be resumed; rider " + rider.value()
                                                     + "'s recovery is " + status);
            }
            status = RecoveryStatus.RUNNING;
            pauseReason = null;
            nextAttemptDueAt = now;
            return DomainEvents.of(new RecoveryEvent.RecoveryResumed(rider, agent, now, failedAttempts, nextAttemptDueAt));
        };
    }

    @Identity
    public RiderId rider() {
        return rider;
    }

    public RecoveryStatus status() {
        return status;
    }

    public int failedAttempts() {
        return failedAttempts;
    }

    public Optional<Instant> nextAttemptDueAt() {
        return Optional.ofNullable(nextAttemptDueAt);
    }

    public Optional<PauseReason> pauseReason() {
        return Optional.ofNullable(pauseReason);
    }

    public Money outstandingDebt() {
        return outstandingDebt;
    }

    public Version version() {
        return version;
    }

    // --- persistence support (memento), not used by domain logic ---

    public record Snapshot(RiderId rider, Version version, RecoveryStatus status, int failedAttempts,
                           Instant nextAttemptDueAt, Money outstandingDebt, PauseReason pauseReason,
                           Instant startedAt) {
    }

    public Snapshot snapshot() {
        return new Snapshot(rider, version, status, failedAttempts, nextAttemptDueAt, outstandingDebt, pauseReason,
                            startedAt);
    }

    public static RecoveryProcess restore(Snapshot snapshot) {
        return new RecoveryProcess(snapshot.rider(), snapshot.version(), snapshot.status(), snapshot.failedAttempts(),
                                   snapshot.nextAttemptDueAt(), snapshot.outstandingDebt(), snapshot.pauseReason(),
                                   snapshot.startedAt());
    }
}
