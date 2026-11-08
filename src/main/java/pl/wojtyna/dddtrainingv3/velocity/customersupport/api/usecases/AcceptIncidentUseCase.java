package pl.wojtyna.dddtrainingv3.velocity.customersupport.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.IncidentId;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.IncidentNotFound;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.Incidents;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.AgentId;

/// The assigned agent accepts an incident.
@PrimaryPort
public class AcceptIncidentUseCase {

    private final Incidents incidents;
    private final DomainEventPublisher eventPublisher;

    public AcceptIncidentUseCase(Incidents incidents, DomainEventPublisher eventPublisher) {
        this.incidents = incidents;
        this.eventPublisher = eventPublisher;
    }

    public void handle(AcceptIncident command) {
        var id = new IncidentId(command.incidentId());
        var incident = incidents.load(id).orElseThrow(() -> new IncidentNotFound(id));
        var events = incident.acceptBy(new AgentId(command.agentId())).at(command.acceptedAt());
        incidents.save(incident);
        eventPublisher.publish(events);
    }
}
