package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.Bike;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.BikeId;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.BikeRepository;

@PrimaryPort
public class ReportFaultUseCase {

    private final BikeRepository bikes;
    private final DomainEventPublisher eventPublisher;

    public ReportFaultUseCase(BikeRepository bikes, DomainEventPublisher eventPublisher) {
        this.bikes = bikes;
        this.eventPublisher = eventPublisher;
    }

    public void handle(ReportFault command) {
        var id = new BikeId(command.bikeId());
        var bike = bikes.load(id).orElseGet(() -> Bike.inCirculation(id));
        var events = bike.reportFault();
        bikes.save(bike);
        eventPublisher.publish(events);
    }
}
