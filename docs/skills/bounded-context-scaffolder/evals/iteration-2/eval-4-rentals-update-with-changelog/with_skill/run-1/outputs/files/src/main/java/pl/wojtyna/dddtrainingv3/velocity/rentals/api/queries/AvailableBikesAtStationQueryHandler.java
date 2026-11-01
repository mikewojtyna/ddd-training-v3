package pl.wojtyna.dddtrainingv3.velocity.rentals.api.queries;

import org.jmolecules.architecture.hexagonal.PrimaryPort;

@PrimaryPort
public class AvailableBikesAtStationQueryHandler {

    private final AvailableBikesReadModel readModel;

    public AvailableBikesAtStationQueryHandler(AvailableBikesReadModel readModel) {
        this.readModel = readModel;
    }

    public AvailableBikesAtStation handle(AvailableBikesAtStationQuery query) {
        return readModel.availableAt(query.stationId());
    }
}
