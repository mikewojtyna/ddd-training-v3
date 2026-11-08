package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.queries;

import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.util.Optional;

/// Answers *Broken Bike*: Fleet Maintenance's view of the bike, with its whereabouts from Rentals.
@PrimaryPort
public class BrokenBikeQueryHandler {

    private final BrokenBikesReadModel readModel;
    private final BikeWhereabouts whereabouts;

    public BrokenBikeQueryHandler(BrokenBikesReadModel readModel, BikeWhereabouts whereabouts) {
        this.readModel = readModel;
        this.whereabouts = whereabouts;
    }

    public Optional<BrokenBikeView> handle(BrokenBikeQuery query) {
        return readModel.bike(query.bikeId())
                        .map(view -> whereabouts.of(view.bikeId()).map(view::at).orElse(view));
    }
}
