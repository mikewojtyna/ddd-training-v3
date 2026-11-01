package pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.Bike;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BikeId;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BikeStationId;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BikeStationNotFoundException;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BikeStationRepo;

/// Canvas inbound command "Return Bike" (Rider app). Publishes `BikeReturned`, which reaches Pricing as an
/// integration event.
@PrimaryPort
public class ReturnBikeUseCase {

    private final BikeStationRepo bikeStations;
    private final DomainEventPublisher eventPublisher;

    public ReturnBikeUseCase(BikeStationRepo bikeStations, DomainEventPublisher eventPublisher) {
        this.bikeStations = bikeStations;
        this.eventPublisher = eventPublisher;
    }

    public void handle(ReturnBike command) {
        var stationId = new BikeStationId(command.bikeStationId());
        var station = bikeStations.load(stationId).orElseThrow(() -> new BikeStationNotFoundException(stationId));
        var events = station.returnBike(new Bike(new BikeId(command.bikeId())));
        bikeStations.save(station);
        eventPublisher.publish(events);
    }
}
