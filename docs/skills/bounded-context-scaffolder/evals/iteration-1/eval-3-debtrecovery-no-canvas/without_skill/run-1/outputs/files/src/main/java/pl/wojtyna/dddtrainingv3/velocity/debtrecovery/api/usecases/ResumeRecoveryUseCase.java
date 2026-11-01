package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recoveryprocess.RecoveryProcessId;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recoveryprocess.RecoveryProcessRepo;

@PrimaryPort
public class ResumeRecoveryUseCase {

    private final RecoveryProcessRepo repo;
    private final DomainEventPublisher eventPublisher;

    public ResumeRecoveryUseCase(RecoveryProcessRepo repo, DomainEventPublisher eventPublisher) {
        this.repo = repo;
        this.eventPublisher = eventPublisher;
    }

    public void resume(RecoveryProcessId id) {
        repo.load(id).ifPresent(process -> {
            var events = process.resume();
            repo.save(process);
            eventPublisher.publish(events);
        });
    }
}
