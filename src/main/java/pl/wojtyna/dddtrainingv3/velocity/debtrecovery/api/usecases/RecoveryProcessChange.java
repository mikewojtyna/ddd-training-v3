package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.usecases;

import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;
import pl.wojtyna.dddtrainingv3.common.domain.OptimisticLockingException;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.RecoveryProcess;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.RecoveryProcesses;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.RiderId;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/// The use cases' shared flow on one rider's recovery process: load → command → save → publish. On a `Version`
/// conflict (e.g. Outstanding Debt Cleared arriving while an attempt is recorded, plan
/// `collections-attempts-debt-collection` D6) the process is reloaded and the command run again on the fresh state,
/// up to 3 times.
class RecoveryProcessChange {

    private static final int MAX_SAVES = 3;

    private final RecoveryProcesses processes;
    private final DomainEventPublisher eventPublisher;

    RecoveryProcessChange(RecoveryProcesses processes, DomainEventPublisher eventPublisher) {
        this.processes = processes;
        this.eventPublisher = eventPublisher;
    }

    /// Runs `command` on the rider's process; `whenMissing` supplies the process when the rider has none (or throws).
    void apply(RiderId rider, Supplier<RecoveryProcess> whenMissing, Function<RecoveryProcess, DomainEvents> command) {
        for (var save = 1; ; save++) {
            var process = processes.load(rider).orElseGet(whenMissing);
            var events = command.apply(process);
            try {
                processes.save(process);
                eventPublisher.publish(events);
                return;
            } catch (OptimisticLockingException concurrentChange) {
                if (save == MAX_SAVES) {
                    throw concurrentChange;
                }
            }
        }
    }

    Optional<RecoveryProcess> load(RiderId rider) {
        return processes.load(rider);
    }
}
