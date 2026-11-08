package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.AgentId;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.RecoveryNotPausedException;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.RecoveryProcesses;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.RiderId;

/// *Resume Recovery*: only a paused recovery can be resumed; its next attempt is due at once and its failed attempts
/// are kept (DE-42, DE-89). The attempt itself is made by the next *Attempt Due Collections* run (plan D5). A rider
/// without a recovery process is rejected with the same rule.
@PrimaryPort
public class ResumeRecoveryUseCase {

    private final RecoveryProcessChange change;

    public ResumeRecoveryUseCase(RecoveryProcesses processes, DomainEventPublisher eventPublisher) {
        this.change = new RecoveryProcessChange(processes, eventPublisher);
    }

    public void handle(ResumeRecovery command) {
        var rider = new RiderId(command.riderId());
        var agent = new AgentId(command.agentId());
        change.apply(rider, () -> {
            throw new RecoveryNotPausedException("Rider " + rider.value() + " has no recovery process to resume");
        }, process -> process.resume().by(agent).at(command.now()));
    }
}
