package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.RecoveryProcess;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.RecoveryProcesses;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.RiderId;

import static pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.Pln.pln;

/// *Start Debt Recovery*: an unpaid fee starts a recovery process with its first attempt due at once, or joins the
/// rider's open one (DE-40, DE-44, DE-88). Two unpaid fees of one rider arriving together are serialized by the
/// `Version` check: the second reloads and joins.
@PrimaryPort
public class StartDebtRecoveryUseCase {

    private final RecoveryProcessChange change;

    public StartDebtRecoveryUseCase(RecoveryProcesses processes, DomainEventPublisher eventPublisher) {
        this.change = new RecoveryProcessChange(processes, eventPublisher);
    }

    public void handle(StartDebtRecovery command) {
        var rider = new RiderId(command.riderId());
        change.apply(rider, () -> RecoveryProcess.none(rider),
                     process -> process.unpaidFee(pln(command.outstandingDebt())).at(command.unpaidAt()));
    }
}
