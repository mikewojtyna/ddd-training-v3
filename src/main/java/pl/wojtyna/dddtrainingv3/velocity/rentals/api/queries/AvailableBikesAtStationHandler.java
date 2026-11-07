package pl.wojtyna.dddtrainingv3.velocity.rentals.api.queries;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BikeId;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BikeStationId;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BikeStationNotFoundException;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BikeStationRepo;

import java.time.Clock;
import java.time.Instant;

/// Canvas query *Available bikes at station*: reserved and withdrawn bikes aren't listed (rule R4).
@PrimaryPort
public class AvailableBikesAtStationHandler {

    private final BikeStationRepo stations;
    private final Clock clock;

    public AvailableBikesAtStationHandler(BikeStationRepo stations, Clock clock) {
        this.stations = stations;
        this.clock = clock;
    }

    public AvailableBikes handle(AvailableBikesAtStation query) {
        var id = new BikeStationId(query.stationId());
        var station = stations.load(id)
                              .orElseThrow(() -> new BikeStationNotFoundException("Unknown station " + id.value()));
        return new AvailableBikes(query.stationId(),
                                  station.availableBikesAt(Instant.now(clock)).stream().map(BikeId::value).toList());
    }
}
