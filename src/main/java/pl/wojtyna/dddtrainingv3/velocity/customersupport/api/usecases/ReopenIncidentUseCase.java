package pl.wojtyna.dddtrainingv3.velocity.customersupport.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.IncidentId;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.IncidentNotFound;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.Incidents;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.RiderId;

/// The reporting rider reopens a rejected incident within its window (DE-54, DE-56).
@PrimaryPort
public class ReopenIncidentUseCase {

    private final Incidents incidents;
    private final DomainEventPublisher eventPublisher;

    public ReopenIncidentUseCase(Incidents incidents, DomainEventPublisher eventPublisher) {
        this.incidents = incidents;
        this.eventPublisher = eventPublisher;
    }

    public void handle(ReopenIncident command) {
        var id = new IncidentId(command.incidentId());
        var incident = incidents.load(id).orElseThrow(() -> new IncidentNotFound(id));
        var events = incident.reopenBy(new RiderId(command.riderId())).at(command.reopenedAt());
        incidents.save(incident);
        eventPublisher.publish(events);
    }
}
