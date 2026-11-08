package pl.wojtyna.dddtrainingv3.velocity.settlement.api.queries;

import org.jmolecules.architecture.hexagonal.PrimaryPort;

@PrimaryPort
public class RiderBalanceQueryHandler {

    private final RiderBalancesReadModel readModel;

    public RiderBalanceQueryHandler(RiderBalancesReadModel readModel) {
        this.readModel = readModel;
    }

    public RiderBalanceView handle(RiderBalanceQuery query) {
        return readModel.balanceOf(query.riderId()).orElseGet(() -> RiderBalanceView.nothingOwedBy(query.riderId()));
    }
}
