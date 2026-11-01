package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recoveryprocess;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recoveryprocess.FluentLanguage.Because;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recoveryprocess.RecoveryProcessEvent.CollectionAttempted;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recoveryprocess.RecoveryProcessEvent.RecoveryExhausted;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recoveryprocess.RecoveryProcessEvent.RecoveryPaused;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recoveryprocess.RecoveryProcessEvent.RecoveryResumed;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recoveryprocess.RecoveryProcessEvent.RecoveryStarted;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recoveryprocess.RecoveryProcessEvent.RecoverySucceeded;

import java.util.Objects;
import java.util.Optional;

/**
 * The active pursuit of one rider's outstanding debt. The debt itself (amount, rider balance) is owned by Settlement;
 * this aggregate owns only the lifecycle of the pursuit: active, paused, succeeded or exhausted. It does not block or
 * unblock the rider's account, that is Accounts' decision.
 */
@AggregateRoot
public class RecoveryProcess {

    @Identity
    private final RecoveryProcessId id;
    private final RiderId rider;
    private RecoveryStatus status;
    private PauseReason pauseReason;

    private RecoveryProcess(RecoveryProcessId id, RiderId rider) {
        this.id = Objects.requireNonNull(id);
        this.rider = Objects.requireNonNull(rider);
        this.status = RecoveryStatus.ACTIVE;
    }

    /**
     * Starts recovery for a rider who owes money. The caller saves the returned process and publishes its events.
     */
    public static Started start(RecoveryProcessId id, RiderId rider) {
        var process = new RecoveryProcess(id, rider);
        return new Started(process, DomainEvents.of(new RecoveryStarted(id, rider)));
    }

    public record Started(RecoveryProcess process, DomainEvents events) {
    }

    /**
     * Asks for the debt to be collected. Only an active process pursues the debt; a paused one waits.
     */
    public DomainEvents attemptCollection() {
        ensureNotClosed();
        if (status == RecoveryStatus.PAUSED) {
            throw new RecoveryProcessPausedException("Recovery is paused, collection is not attempted");
        }
        return DomainEvents.of(new CollectionAttempted(id, rider));
    }

    public Because<PauseReason, DomainEvents> pause() {
        return reason -> {
            Objects.requireNonNull(reason);
            ensureNotClosed();
            if (status == RecoveryStatus.PAUSED) {
                throw new RecoveryProcessPausedException("Recovery is already paused");
            }
            status = RecoveryStatus.PAUSED;
            pauseReason = reason;
            return DomainEvents.of(new RecoveryPaused(id, reason));
        };
    }

    public DomainEvents resume() {
        ensureNotClosed();
        if (status != RecoveryStatus.PAUSED) {
            throw new RecoveryProcessNotPausedException("Only a paused recovery can be resumed");
        }
        status = RecoveryStatus.ACTIVE;
        pauseReason = null;
        return DomainEvents.of(new RecoveryResumed(id));
    }

    /**
     * Settlement reported the rider's outstanding debt as cleared. Recovery succeeds even if it was paused: the money
     * is back, so there is nothing left to pursue.
     */
    public DomainEvents debtCleared() {
        ensureNotClosed();
        status = RecoveryStatus.SUCCEEDED;
        pauseReason = null;
        return DomainEvents.of(new RecoverySucceeded(id, rider));
    }

    /**
     * All recovery actions were tried and rejected. The rules that decide when this happens (schedule, which actions)
     * are not modeled yet.
     */
    public DomainEvents exhaust() {
        ensureNotClosed();
        status = RecoveryStatus.EXHAUSTED;
        pauseReason = null;
        return DomainEvents.of(new RecoveryExhausted(id, rider));
    }

    public RecoveryProcessId id() {
        return id;
    }

    public RiderId rider() {
        return rider;
    }

    public RecoveryStatus status() {
        return status;
    }

    public Optional<PauseReason> pauseReason() {
        return Optional.ofNullable(pauseReason);
    }

    public boolean isOpen() {
        return !status.isClosed();
    }

    private void ensureNotClosed() {
        if (status.isClosed()) {
            throw new RecoveryProcessClosedException("Recovery process is already " + status);
        }
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof RecoveryProcess other && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
