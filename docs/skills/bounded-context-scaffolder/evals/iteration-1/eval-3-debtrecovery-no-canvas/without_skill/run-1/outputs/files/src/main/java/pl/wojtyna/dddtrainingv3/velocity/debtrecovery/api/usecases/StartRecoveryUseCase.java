package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recoveryprocess.RecoveryProcess;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recoveryprocess.RecoveryProcessId;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recoveryprocess.RecoveryProcessRepo;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recoveryprocess.RiderId;

/**
 * Triggered by Settlement's "Rental Fee Unpaid". Idempotent per rider: if the rider already has an open recovery
 * process, the new unpaid fee is pursued by that process (Settlement owns the amount, not Debt Recovery).
 */
@PrimaryPort
public class StartRecoveryUseCase {

    private final RecoveryProcessRepo repo;
    private final DomainEventPublisher eventPublisher;

    public StartRecoveryUseCase(RecoveryProcessRepo repo, DomainEventPublisher eventPublisher) {
        this.repo = repo;
        this.eventPublisher = eventPublisher;
    }

    public void startFor(RiderId rider) {
        if (repo.findOpenFor(rider).isPresent()) {
            return;
        }
        var started = RecoveryProcess.start(RecoveryProcessId.generate(), rider);
        repo.save(started.process());
        eventPublisher.publish(started.events());
    }
}
