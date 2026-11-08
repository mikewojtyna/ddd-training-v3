package pl.wojtyna.dddtrainingv3.velocity.customersupport.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.IncidentId;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.IncidentNotFound;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.Incidents;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.AgentId;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.Resolution;

/// The assigned agent resolves an accepted, escalated or reopened incident.
@PrimaryPort
public class ResolveIncidentUseCase {

    private final Incidents incidents;
    private final DomainEventPublisher eventPublisher;

    public ResolveIncidentUseCase(Incidents incidents, DomainEventPublisher eventPublisher) {
        this.incidents = incidents;
        this.eventPublisher = eventPublisher;
    }

    public void handle(ResolveIncident command) {
        var id = new IncidentId(command.incidentId());
        var incident = incidents.load(id).orElseThrow(() -> new IncidentNotFound(id));
        var events = incident.resolveBy(new AgentId(command.agentId())).with(new Resolution(command.resolution())).at(command.resolvedAt());
        incidents.save(incident);
        eventPublisher.publish(events);
    }
}
