package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.queries;

import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.util.Optional;

/// Answers *Recovery Process*: empty when the rider never had a recovery process.
@PrimaryPort
public class RecoveryProcessQueryHandler {

    private final RecoveryProcessesReadModel readModel;

    public RecoveryProcessQueryHandler(RecoveryProcessesReadModel readModel) {
        this.readModel = readModel;
    }

    public Optional<RecoveryProcessView> handle(RecoveryProcessQuery query) {
        return readModel.recoveryOf(query.riderId());
    }
}
