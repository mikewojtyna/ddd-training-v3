package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.BikeId;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.Bikes;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.TechnicianId;

/// Rentals reported that a technician's reservation ended without an unlock (expired, or the lock didn't open): the
/// bike stays broken and waits for any technician.
@PrimaryPort
public class ReleaseTechnicianReservationUseCase {

    private final Bikes bikes;
    private final DomainEventPublisher eventPublisher;

    public ReleaseTechnicianReservationUseCase(Bikes bikes, DomainEventPublisher eventPublisher) {
        this.bikes = bikes;
        this.eventPublisher = eventPublisher;
    }

    public void handle(ReleaseTechnicianReservation command) {
        var events = Retrying.onConflict(() -> bikes.load(new BikeId(command.bikeId())).map(bike -> {
            var happened = bike.releaseReservationOf(new TechnicianId(command.technicianId())).at(command.releasedAt());
            if (happened.stream().findAny().isPresent()) {
                bikes.save(bike);
            }
            return happened;
        }).orElseGet(DomainEvents::empty));
        eventPublisher.publish(events);
    }
}
