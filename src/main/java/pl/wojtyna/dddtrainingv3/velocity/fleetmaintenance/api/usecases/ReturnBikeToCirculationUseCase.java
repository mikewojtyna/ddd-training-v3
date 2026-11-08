package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.*;

/// A technician returns a bike under maintenance to circulation. Order (plan
/// technician-returns-fixed-bike-to-circulation D5): the rule (only after maintenance started) → Rentals makes the
/// bike available → Bike Fixed is recorded → each linked fleet-issue incident is resolved in Customer Support. A
/// failed incident resolution is logged and doesn't undo the fix.
@PrimaryPort
public class ReturnBikeToCirculationUseCase {

    private static final Logger log = LoggerFactory.getLogger(ReturnBikeToCirculationUseCase.class);

    private final Bikes bikes;
    private final CirculationReturns circulationReturns;
    private final FleetIssueResolutions fleetIssueResolutions;
    private final DomainEventPublisher eventPublisher;

    public ReturnBikeToCirculationUseCase(Bikes bikes, CirculationReturns circulationReturns,
                                          FleetIssueResolutions fleetIssueResolutions, DomainEventPublisher eventPublisher) {
        this.bikes = bikes;
        this.circulationReturns = circulationReturns;
        this.fleetIssueResolutions = fleetIssueResolutions;
        this.eventPublisher = eventPublisher;
    }

    public void handle(ReturnBikeToCirculation command) {
        var bike = bikes.loadOrUnknown(new BikeId(command.bikeId()));
        var place = command.stationId() != null && !command.stationId().isBlank()
                    ? Place.station(command.stationId())
                    : Place.position(command.latitude(), command.longitude());
        var circulationReturn = new CirculationReturn(new TechnicianId(command.technicianId()), place, command.returnedAt());
        var events = bike.returnToCirculation(circulationReturn).through(circulationReturns);
        bikes.save(bike);
        eventPublisher.publish(events);
        events.stream()
              .filter(BikeEvent.BikeFixed.class::isInstance)
              .map(BikeEvent.BikeFixed.class::cast)
              .flatMap(fixed -> fixed.incidents().stream())
              .forEach(incident -> resolve(incident, bike.id()));
    }

    private void resolve(IncidentId incident, BikeId bike) {
        try {
            fleetIssueResolutions.resolve(incident, bike);
        } catch (RuntimeException failure) {
            log.error("Bike {} is fixed but incident {} wasn't resolved", bike.value(), incident.value(), failure);
        }
    }
}
