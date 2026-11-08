package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.queries;

import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.util.List;

/// Answers *Broken Bikes*: every broken bike or bike under maintenance, with its whereabouts from Rentals.
@PrimaryPort
public class BrokenBikesQueryHandler {

    private final BrokenBikesReadModel readModel;
    private final BikeWhereabouts whereabouts;

    public BrokenBikesQueryHandler(BrokenBikesReadModel readModel, BikeWhereabouts whereabouts) {
        this.readModel = readModel;
        this.whereabouts = whereabouts;
    }

    public List<BrokenBikeView> handle(BrokenBikesQuery query) {
        return readModel.brokenBikes().stream()
                        .map(view -> whereabouts.of(view.bikeId()).map(view::at).orElse(view))
                        .toList();
    }
}
