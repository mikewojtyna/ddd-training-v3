package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.Bike;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.BikeId;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.BikeRepository;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.TechnicianId;

@PrimaryPort
public class ReturnBikeToCirculationUseCase {

    private final BikeRepository bikes;
    private final DomainEventPublisher eventPublisher;

    public ReturnBikeToCirculationUseCase(BikeRepository bikes, DomainEventPublisher eventPublisher) {
        this.bikes = bikes;
        this.eventPublisher = eventPublisher;
    }

    public void handle(ReturnBikeToCirculation command) {
        var id = new BikeId(command.bikeId());
        var bike = bikes.load(id).orElseGet(() -> Bike.inCirculation(id));
        var events = bike.returnToCirculation(new TechnicianId(command.technicianId()));
        bikes.save(bike);
        eventPublisher.publish(events);
    }
}
