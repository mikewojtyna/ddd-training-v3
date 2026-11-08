package pl.wojtyna.dddtrainingv3.velocity.customersupport.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.IncidentId;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.IncidentNotFound;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.Incidents;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.AgentId;

/// A support agent picks up an open incident (DE-51).
@PrimaryPort
public class PickUpIncidentUseCase {

    private final Incidents incidents;
    private final DomainEventPublisher eventPublisher;

    public PickUpIncidentUseCase(Incidents incidents, DomainEventPublisher eventPublisher) {
        this.incidents = incidents;
        this.eventPublisher = eventPublisher;
    }

    public void handle(PickUpIncident command) {
        var id = new IncidentId(command.incidentId());
        var incident = incidents.load(id).orElseThrow(() -> new IncidentNotFound(id));
        var events = incident.pickUpBy(new AgentId(command.agentId())).at(command.pickedUpAt());
        incidents.save(incident);
        eventPublisher.publish(events);
    }
}
