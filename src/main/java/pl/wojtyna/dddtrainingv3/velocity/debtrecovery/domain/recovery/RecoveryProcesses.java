package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery;

import org.jmolecules.architecture.hexagonal.SecondaryPort;
import org.jmolecules.ddd.annotation.Repository;
import pl.wojtyna.dddtrainingv3.common.domain.OptimisticLockingException;

import java.util.List;
import java.util.Optional;

/// Riders' recovery processes, one per rider.
@Repository
@SecondaryPort
public interface RecoveryProcesses {

    Optional<RecoveryProcess> load(RiderId rider);

    /// Running recovery processes: the candidates for a due collection attempt (each process decides whether it is
    /// due, `RecoveryProcess#attemptDueAt`).
    List<RecoveryProcess> loadRunning();

    /// Stores the process.
    ///
    /// @throws OptimisticLockingException if the stored version differs from `process.version()`
    void save(RecoveryProcess process);
}
