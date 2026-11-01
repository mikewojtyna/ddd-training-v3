package pl.wojtyna.dddtrainingv3.velocity.rentals.api.queries;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BikeStation;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BikeStationId;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BikeStationNotFoundException;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BikeStationRepo;

import java.util.List;

/**
 * Rider app asks which bikes are available at a bike station.
 */
@PrimaryPort
public class AvailableBikesAtStationQuery {

    private final BikeStationRepo bikeStationRepo;

    public AvailableBikesAtStationQuery(BikeStationRepo bikeStationRepo) {
        this.bikeStationRepo = bikeStationRepo;
    }

    public AvailableBikesAtStation availableBikesAt(BikeStationId stationId) {
        var station = bikeStationRepo.load(stationId)
                                     .orElseThrow(() -> new BikeStationNotFoundException(
                                         "Bike station %s not found".formatted(stationId.value())));
        return AvailableBikesAtStation.from(station);
    }

    public record AvailableBikesAtStation(String stationId, List<String> bikeIds) {

        static AvailableBikesAtStation from(BikeStation station) {
            return new AvailableBikesAtStation(station.id().value(),
                                               station.availableBikes()
                                                      .stream()
                                                      .map(bike -> bike.id().value())
                                                      .toList());
        }
    }
}
