package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.AgentId;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.PauseReason;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.RecoveryNotRunningException;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.RecoveryProcesses;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.RiderId;

import java.util.Arrays;

/// *Pause Recovery*: only a running recovery can be paused, for one of the three reasons (DE-42, DE-89). A rider
/// without a recovery process is rejected with the same rule (plan D6).
@PrimaryPort
public class PauseRecoveryUseCase {

    private final RecoveryProcessChange change;

    public PauseRecoveryUseCase(RecoveryProcesses processes, DomainEventPublisher eventPublisher) {
        this.change = new RecoveryProcessChange(processes, eventPublisher);
    }

    public void handle(PauseRecovery command) {
        var rider = new RiderId(command.riderId());
        var reason = reasonOf(command.reason());
        var agent = new AgentId(command.agentId());
        change.apply(rider, () -> {
            throw new RecoveryNotRunningException("Rider " + rider.value() + " has no recovery process to pause");
        }, process -> process.pause(reason).by(agent).at(command.now()));
    }

    private static PauseReason reasonOf(String reason) {
        return Arrays.stream(PauseReason.values())
                     .filter(known -> known.name().equals(reason))
                     .findFirst()
                     .orElseThrow(() -> new UnknownPauseReason(
                         "A recovery is paused only for DISPUTE, PAYMENT_PLAN or REGULATORY_HOLD, not '" + reason + "'"));
    }
}
