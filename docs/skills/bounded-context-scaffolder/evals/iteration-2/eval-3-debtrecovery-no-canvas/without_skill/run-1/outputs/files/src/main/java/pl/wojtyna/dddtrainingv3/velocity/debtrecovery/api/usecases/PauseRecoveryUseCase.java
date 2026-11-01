package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recoveryprocess.PauseReason;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recoveryprocess.RecoveryProcessId;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recoveryprocess.RecoveryProcessRepo;

@PrimaryPort
public class PauseRecoveryUseCase {

    private final RecoveryProcessRepo repo;
    private final DomainEventPublisher eventPublisher;

    public PauseRecoveryUseCase(RecoveryProcessRepo repo, DomainEventPublisher eventPublisher) {
        this.repo = repo;
        this.eventPublisher = eventPublisher;
    }

    public void pause(RecoveryProcessId id, PauseReason reason) {
        repo.load(id).ifPresent(process -> {
            var events = process.pause().because(reason);
            repo.save(process);
            eventPublisher.publish(events);
        });
    }
}
