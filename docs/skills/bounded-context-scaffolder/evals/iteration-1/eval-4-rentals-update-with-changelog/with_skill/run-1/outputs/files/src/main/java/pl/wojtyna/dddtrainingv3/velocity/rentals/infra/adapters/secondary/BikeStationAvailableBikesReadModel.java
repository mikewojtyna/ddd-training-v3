package pl.wojtyna.dddtrainingv3.velocity.rentals.infra.adapters.secondary;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.queries.AvailableBikesAtStationView;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.queries.AvailableBikesReadModel;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BikeStationId;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BikeStationRepo;

import java.util.Optional;
import java.util.stream.Collectors;

/// Read model for "Available Bikes At Station", answered straight from the `BikeStation` aggregate in the
/// repository. Replace with an event-fed projection if reads and writes need to scale separately.
@SecondaryAdapter
public class BikeStationAvailableBikesReadModel implements AvailableBikesReadModel {

    private final BikeStationRepo bikeStations;

    public BikeStationAvailableBikesReadModel(BikeStationRepo bikeStations) {
        this.bikeStations = bikeStations;
    }

    @Override
    public Optional<AvailableBikesAtStationView> availableAt(String bikeStationId) {
        return bikeStations.load(new BikeStationId(bikeStationId))
                           .map(station -> new AvailableBikesAtStationView(
                               bikeStationId,
                               station.availableBikes().stream()
                                      .map(bike -> bike.id().value())
                                      .collect(Collectors.toSet())));
    }
}
