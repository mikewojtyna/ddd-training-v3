package pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.Bike;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BikeId;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BikeStationNotFoundException;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BikeStationRepo;

/// Canvas command *Withdraw bike*, issued by Fleet Maintenance (rule R5).
@PrimaryPort
public class WithdrawBikeUseCase {

    private final BikeStationRepo stations;
    private final DomainEventPublisher eventPublisher;

    public WithdrawBikeUseCase(BikeStationRepo stations, DomainEventPublisher eventPublisher) {
        this.stations = stations;
        this.eventPublisher = eventPublisher;
    }

    public void handle(WithdrawBike command) {
        var bike = new BikeId(command.bikeId());
        var station = stations.loadContaining(bike)
                              .orElseThrow(() -> new BikeStationNotFoundException("No station holds bike " + bike.value()));
        var events = station.withdraw(new Bike(bike));
        stations.save(station);
        eventPublisher.publish(events);
    }
}
