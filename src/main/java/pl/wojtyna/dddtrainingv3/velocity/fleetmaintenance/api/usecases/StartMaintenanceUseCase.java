package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.BikeId;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.Bikes;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.TechnicianId;

/// Rentals reported a technician's unlock of a bike: maintenance on it starts. A bike Fleet Maintenance doesn't know
/// is ignored.
@PrimaryPort
public class StartMaintenanceUseCase {

    private final Bikes bikes;
    private final DomainEventPublisher eventPublisher;

    public StartMaintenanceUseCase(Bikes bikes, DomainEventPublisher eventPublisher) {
        this.bikes = bikes;
        this.eventPublisher = eventPublisher;
    }

    public void handle(StartMaintenance command) {
        var events = Retrying.onConflict(() -> bikes.load(new BikeId(command.bikeId())).map(bike -> {
            var happened = bike.startMaintenanceBy(new TechnicianId(command.technicianId())).at(command.startedAt());
            if (happened.stream().findAny().isPresent()) {
                bikes.save(bike);
            }
            return happened;
        }).orElseGet(DomainEvents::empty));
        eventPublisher.publish(events);
    }
}
