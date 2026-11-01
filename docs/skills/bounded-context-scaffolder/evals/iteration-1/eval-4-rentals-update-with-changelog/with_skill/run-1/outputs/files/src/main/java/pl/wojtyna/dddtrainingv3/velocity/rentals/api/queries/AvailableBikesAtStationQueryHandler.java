package pl.wojtyna.dddtrainingv3.velocity.rentals.api.queries;

import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.util.Optional;

/// Canvas inbound query "Available Bikes At Station" (Rider app).
@PrimaryPort
public class AvailableBikesAtStationQueryHandler {

    private final AvailableBikesReadModel readModel;

    public AvailableBikesAtStationQueryHandler(AvailableBikesReadModel readModel) {
        this.readModel = readModel;
    }

    public Optional<AvailableBikesAtStationView> handle(AvailableBikesAtStation query) {
        return readModel.availableAt(query.bikeStationId());
    }
}
