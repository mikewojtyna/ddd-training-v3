package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.queries;

import org.jmolecules.architecture.hexagonal.SecondaryPort;

import java.util.Optional;

/// Read model of riders' recovery processes.
@SecondaryPort
public interface RecoveryProcessesReadModel {

    Optional<RecoveryProcessView> recoveryOf(String riderId);
}
