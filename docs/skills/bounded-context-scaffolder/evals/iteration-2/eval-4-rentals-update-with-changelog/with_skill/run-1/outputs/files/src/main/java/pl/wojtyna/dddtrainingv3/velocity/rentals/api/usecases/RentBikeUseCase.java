package pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.Bike;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BikeId;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BikeStationId;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BikeStationRepo;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.Borrower;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BorrowerId;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.RentalDuration;

/// Canvas inbound command "Rent Bike" from the Rider app.
@PrimaryPort
public class RentBikeUseCase {

    private final BikeStationRepo bikeStations;
    private final DomainEventPublisher eventPublisher;

    public RentBikeUseCase(BikeStationRepo bikeStations, DomainEventPublisher eventPublisher) {
        this.bikeStations = bikeStations;
        this.eventPublisher = eventPublisher;
    }

    public void rent(BikeStationId id, BikeId bike, BorrowerId borrowerId, RentalDuration duration) {
        // load aggregate
        var station = bikeStations.load(id).orElseThrow(() -> new BikeStationNotFoundException(id));
        // execute command
        var events = station.rent(new Bike(bike)).by(new Borrower(borrowerId)).forDuration(duration);
        // save aggregate
        bikeStations.save(station);
        // publish events
        eventPublisher.publish(events);
    }
}
