package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recoveryprocess;

import pl.wojtyna.dddtrainingv3.common.domain.DomainEvent;

public sealed interface RecoveryProcessEvent extends DomainEvent {

    RecoveryProcessId processId();

    record RecoveryStarted(RecoveryProcessId processId, RiderId rider) implements RecoveryProcessEvent {
    }

    record CollectionAttempted(RecoveryProcessId processId, RiderId rider) implements RecoveryProcessEvent {
    }

    record RecoveryPaused(RecoveryProcessId processId, PauseReason reason) implements RecoveryProcessEvent {
    }

    record RecoveryResumed(RecoveryProcessId processId) implements RecoveryProcessEvent {
    }

    record RecoverySucceeded(RecoveryProcessId processId, RiderId rider) implements RecoveryProcessEvent {
    }

    record RecoveryExhausted(RecoveryProcessId processId, RiderId rider) implements RecoveryProcessEvent {
    }
}
