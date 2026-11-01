package pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.Bike;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BikeId;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BikeStationId;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BikeStationNotFoundException;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BikeStationRepo;

/**
 * Rider app returns a rented bike to a bike station.
 */
@PrimaryPort
public class ReturnBikeUseCase {

    private final BikeStationRepo bikeStationRepo;
    private final DomainEventPublisher eventPublisher;

    public ReturnBikeUseCase(BikeStationRepo bikeStationRepo, DomainEventPublisher eventPublisher) {
        this.bikeStationRepo = bikeStationRepo;
        this.eventPublisher = eventPublisher;
    }

    public void returnBike(BikeStationId stationId, BikeId bikeId) {
        var station = bikeStationRepo.load(stationId)
                                     .orElseThrow(() -> new BikeStationNotFoundException(
                                         "Bike station %s not found".formatted(stationId.value())));
        var events = station.returnBike(new Bike(bikeId));
        bikeStationRepo.save(station);
        eventPublisher.publish(events);
    }
}
