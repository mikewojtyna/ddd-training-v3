package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery;

import org.joda.money.Money;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvent;

import java.time.Instant;

/// What happens to a rider's recovery process. Nobody outside Debt Recovery listens yet, so there are no integration
/// events (canvas Outbound).
public sealed interface RecoveryEvent extends DomainEvent {

    RiderId rider();

    /// A recovery process started for the rider's whole outstanding debt; the first attempt is due at once.
    record RecoveryStarted(RiderId rider, Money outstandingDebt, Instant startedAt, Instant firstAttemptDueAt)
        implements RecoveryEvent {
    }

    /// A due collection attempt was made. `nextAttemptDueAt` is `null` when it collected or exhausted the recovery.
    record CollectionAttempted(RiderId rider, int attemptNumber, Money amountAsked, boolean collected,
                               Instant attemptedAt, Instant nextAttemptDueAt) implements RecoveryEvent {
    }

    /// The rider's outstanding debt was cleared while the recovery was open or exhausted.
    record RecoverySucceeded(RiderId rider, Instant succeededAt, int failedAttempts) implements RecoveryEvent {
    }

    /// The 5th collection attempt failed; the debt stays outstanding.
    record RecoveryExhausted(RiderId rider, Instant exhaustedAt, Money outstandingDebt) implements RecoveryEvent {
    }

    /// A collections agent paused a running recovery.
    record RecoveryPaused(RiderId rider, PauseReason reason, AgentId pausedBy, Instant pausedAt, int failedAttempts)
        implements RecoveryEvent {
    }

    /// A collections agent resumed a paused recovery; the next attempt is due at once.
    record RecoveryResumed(RiderId rider, AgentId resumedBy, Instant resumedAt, int failedAttempts,
                           Instant nextAttemptDueAt) implements RecoveryEvent {
    }
}
