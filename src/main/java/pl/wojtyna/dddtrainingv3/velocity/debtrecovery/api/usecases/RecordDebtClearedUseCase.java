package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.RecoveryProcesses;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.RiderId;

/// *Record Debt Cleared*: an open or exhausted recovery succeeds; an already succeeded one is left as it is (DE-45).
/// A rider without a recovery process (debt paid before recovery ever started) changes nothing.
@PrimaryPort
public class RecordDebtClearedUseCase {

    private final RecoveryProcessChange change;

    public RecordDebtClearedUseCase(RecoveryProcesses processes, DomainEventPublisher eventPublisher) {
        this.change = new RecoveryProcessChange(processes, eventPublisher);
    }

    public void handle(RecordDebtCleared command) {
        var rider = new RiderId(command.riderId());
        if (change.load(rider).isEmpty()) {
            return;
        }
        change.apply(rider, () -> {
            throw new IllegalStateException("Recovery process of rider " + rider.value() + " disappeared");
        }, process -> process.debtCleared(command.clearedAt()));
    }
}
