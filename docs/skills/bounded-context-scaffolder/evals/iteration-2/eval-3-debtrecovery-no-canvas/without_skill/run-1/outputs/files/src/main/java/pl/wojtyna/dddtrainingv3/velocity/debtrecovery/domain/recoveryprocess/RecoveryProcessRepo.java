package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recoveryprocess;

import org.jmolecules.architecture.hexagonal.SecondaryPort;

import java.util.Optional;

@SecondaryPort
public interface RecoveryProcessRepo {

    Optional<RecoveryProcess> load(RecoveryProcessId id);

    /**
     * A rider has at most one open (active or paused) recovery process.
     */
    Optional<RecoveryProcess> findOpenFor(RiderId rider);

    void save(RecoveryProcess process);
}
