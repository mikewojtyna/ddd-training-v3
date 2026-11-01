package pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.Bike;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BikeId;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BikeStationId;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BikeStationRepo;

/// Canvas inbound command "Return Bike" from the Rider app.
@PrimaryPort
public class ReturnBikeUseCase {

    private final BikeStationRepo bikeStations;
    private final DomainEventPublisher eventPublisher;

    public ReturnBikeUseCase(BikeStationRepo bikeStations, DomainEventPublisher eventPublisher) {
        this.bikeStations = bikeStations;
        this.eventPublisher = eventPublisher;
    }

    public void returnBike(BikeStationId id, BikeId bike) {
        // load aggregate
        var station = bikeStations.load(id).orElseThrow(() -> new BikeStationNotFoundException(id));
        // execute command
        var events = station.returnBike(new Bike(bike));
        // save aggregate
        bikeStations.save(station);
        // publish events
        eventPublisher.publish(events);
    }
}
