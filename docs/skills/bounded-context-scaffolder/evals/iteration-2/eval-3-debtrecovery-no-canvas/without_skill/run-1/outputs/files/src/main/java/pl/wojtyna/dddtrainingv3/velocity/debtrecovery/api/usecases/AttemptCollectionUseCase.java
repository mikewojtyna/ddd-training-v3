package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recoveryprocess.DebtCollector;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recoveryprocess.RecoveryProcessId;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recoveryprocess.RecoveryProcessRepo;

/**
 * One step of the recovery schedule: ask Settlement to collect the debt. What triggers this (schedule, reminders,
 * payment plan instalments) is not modeled yet.
 */
@PrimaryPort
public class AttemptCollectionUseCase {

    private final RecoveryProcessRepo repo;
    private final DebtCollector debtCollector;
    private final DomainEventPublisher eventPublisher;

    public AttemptCollectionUseCase(RecoveryProcessRepo repo, DebtCollector debtCollector,
                                    DomainEventPublisher eventPublisher) {
        this.repo = repo;
        this.debtCollector = debtCollector;
        this.eventPublisher = eventPublisher;
    }

    public void attempt(RecoveryProcessId id) {
        repo.load(id).ifPresent(process -> {
            var events = process.attemptCollection();
            debtCollector.requestCollection(process.rider());
            repo.save(process);
            eventPublisher.publish(events);
        });
    }
}
