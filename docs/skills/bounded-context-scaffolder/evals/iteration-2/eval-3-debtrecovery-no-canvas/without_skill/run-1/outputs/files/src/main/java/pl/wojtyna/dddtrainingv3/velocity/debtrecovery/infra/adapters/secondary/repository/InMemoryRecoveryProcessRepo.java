package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.infra.adapters.secondary.repository;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recoveryprocess.RecoveryProcess;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recoveryprocess.RecoveryProcessId;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recoveryprocess.RecoveryProcessRepo;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recoveryprocess.RiderId;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@SecondaryAdapter
public class InMemoryRecoveryProcessRepo implements RecoveryProcessRepo {

    private final Map<RecoveryProcessId, RecoveryProcess> processes = new ConcurrentHashMap<>();

    @Override
    public Optional<RecoveryProcess> load(RecoveryProcessId id) {
        return Optional.ofNullable(processes.get(id));
    }

    @Override
    public Optional<RecoveryProcess> findOpenFor(RiderId rider) {
        return processes.values()
                        .stream()
                        .filter(process -> process.rider().equals(rider))
                        .filter(RecoveryProcess::isOpen)
                        .findFirst();
    }

    @Override
    public void save(RecoveryProcess process) {
        processes.put(process.id(), process);
    }
}
