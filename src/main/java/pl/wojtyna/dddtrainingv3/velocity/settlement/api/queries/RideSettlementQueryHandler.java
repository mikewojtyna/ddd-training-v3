package pl.wojtyna.dddtrainingv3.velocity.settlement.api.queries;

import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.util.Optional;

@PrimaryPort
public class RideSettlementQueryHandler {

    private final RideSettlementsReadModel readModel;

    public RideSettlementQueryHandler(RideSettlementsReadModel readModel) {
        this.readModel = readModel;
    }

    public Optional<RideSettlementView> handle(RideSettlementQuery query) {
        return readModel.rideSettlementOf(query.sessionId());
    }
}
