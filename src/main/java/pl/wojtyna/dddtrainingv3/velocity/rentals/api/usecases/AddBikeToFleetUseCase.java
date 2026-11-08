package pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeId;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeRepository;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.Fleet;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.Station.StationId;

import java.time.Clock;

@PrimaryPort
public class AddBikeToFleetUseCase {

    private final Fleet fleet;
    private final BikeRepository bikes;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;

    public AddBikeToFleetUseCase(Fleet fleet, BikeRepository bikes, DomainEventPublisher eventPublisher, Clock clock) {
        this.fleet = fleet;
        this.bikes = bikes;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    public void handle(AddBikeToFleet command) {
        var added = fleet.add(new BikeId(command.bikeId()), new StationId(command.stationId()), clock.instant());
        bikes.save(added.bike());
        eventPublisher.publish(added.events());
    }
}
