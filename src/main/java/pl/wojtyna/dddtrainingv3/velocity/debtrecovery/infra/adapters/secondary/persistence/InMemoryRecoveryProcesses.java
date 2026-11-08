package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.infra.adapters.secondary.persistence;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import pl.wojtyna.dddtrainingv3.common.domain.OptimisticLockingException;
import pl.wojtyna.dddtrainingv3.common.domain.Version;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.queries.RecoveryProcessView;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.queries.RecoveryProcessesReadModel;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.RecoveryProcess;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.RecoveryProcesses;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.RecoveryStatus;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.RiderId;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/// Riders' recovery processes in memory, with optimistic locking on `Version`. Also serves the Recovery Process query.
@SecondaryAdapter
public class InMemoryRecoveryProcesses implements RecoveryProcesses, RecoveryProcessesReadModel {

    private final Map<RiderId, RecoveryProcess.Snapshot> processes = new ConcurrentHashMap<>();

    @Override
    public Optional<RecoveryProcess> load(RiderId rider) {
        return Optional.ofNullable(processes.get(rider)).map(RecoveryProcess::restore);
    }

    @Override
    public List<RecoveryProcess> loadRunning() {
        return processes.values().stream()
                        .filter(snapshot -> snapshot.status() == RecoveryStatus.RUNNING)
                        .map(RecoveryProcess::restore)
                        .toList();
    }

    @Override
    public void save(RecoveryProcess process) {
        var snapshot = process.snapshot();
        processes.compute(snapshot.rider(), (rider, stored) -> {
            var storedVersion = stored == null ? Version.initial() : stored.version();
            if (!storedVersion.equals(snapshot.version())) {
                throw new OptimisticLockingException("Recovery process of rider " + rider.value() + " was modified concurrently");
            }
            return new RecoveryProcess.Snapshot(rider, snapshot.version().next(), snapshot.status(),
                                                snapshot.failedAttempts(), snapshot.nextAttemptDueAt(),
                                                snapshot.outstandingDebt(), snapshot.pauseReason(), snapshot.startedAt());
        });
    }

    @Override
    public Optional<RecoveryProcessView> recoveryOf(String riderId) {
        return Optional.ofNullable(processes.get(new RiderId(riderId)))
                       .map(snapshot -> new RecoveryProcessView(
                           riderId,
                           snapshot.status().name(),
                           snapshot.outstandingDebt() == null ? null : snapshot.outstandingDebt().getAmount().toPlainString(),
                           snapshot.failedAttempts(),
                           snapshot.status() == RecoveryStatus.RUNNING ? snapshot.nextAttemptDueAt() : null,
                           snapshot.status() == RecoveryStatus.PAUSED && snapshot.pauseReason() != null
                           ? snapshot.pauseReason().name() : null));
    }
}
