package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.*;

/// A technician reserves a broken bike: Fleet Maintenance rejects a bike that isn't broken (R2), Rentals holds it
/// (R1, R3) and says until when.
@PrimaryPort
public class ReserveBrokenBikeUseCase {

    private final Bikes bikes;
    private final ServiceReservations serviceReservations;
    private final DomainEventPublisher eventPublisher;

    public ReserveBrokenBikeUseCase(Bikes bikes, ServiceReservations serviceReservations, DomainEventPublisher eventPublisher) {
        this.bikes = bikes;
        this.serviceReservations = serviceReservations;
        this.eventPublisher = eventPublisher;
    }

    public TechnicianReservationReceipt handle(ReserveBrokenBike command) {
        var bike = bikes.loadOrUnknown(new BikeId(command.bikeId()));
        var events = bike.reserveFor(new TechnicianId(command.technicianId())).through(serviceReservations);
        bikes.save(bike);
        eventPublisher.publish(events);
        var reservation = bike.reservation().orElseThrow();
        return new TechnicianReservationReceipt(command.bikeId(), command.technicianId(), reservation.until());
    }
}
