package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recoveryprocess.RecoveryProcessRepo;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recoveryprocess.RiderId;

/**
 * Triggered by Settlement's "Outstanding Debt Cleared". Unblocking the account is not done here: Accounts reacts to
 * the same Settlement event.
 */
@PrimaryPort
public class CloseRecoveryOnDebtClearedUseCase {

    private final RecoveryProcessRepo repo;
    private final DomainEventPublisher eventPublisher;

    public CloseRecoveryOnDebtClearedUseCase(RecoveryProcessRepo repo, DomainEventPublisher eventPublisher) {
        this.repo = repo;
        this.eventPublisher = eventPublisher;
    }

    public void debtClearedFor(RiderId rider) {
        repo.findOpenFor(rider).ifPresent(process -> {
            var events = process.debtCleared();
            repo.save(process);
            eventPublisher.publish(events);
        });
    }
}
