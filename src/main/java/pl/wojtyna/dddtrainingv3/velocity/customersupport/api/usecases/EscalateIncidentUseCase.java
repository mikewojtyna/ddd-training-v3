package pl.wojtyna.dddtrainingv3.velocity.customersupport.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.IncidentId;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.IncidentNotFound;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.Incidents;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.AgentId;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.Team;

/// The assigned agent escalates an accepted incident to a team (DE-53).
@PrimaryPort
public class EscalateIncidentUseCase {

    private final Incidents incidents;
    private final DomainEventPublisher eventPublisher;

    public EscalateIncidentUseCase(Incidents incidents, DomainEventPublisher eventPublisher) {
        this.incidents = incidents;
        this.eventPublisher = eventPublisher;
    }

    public void handle(EscalateIncident command) {
        var id = new IncidentId(command.incidentId());
        var incident = incidents.load(id).orElseThrow(() -> new IncidentNotFound(id));
        var events = incident.escalateBy(new AgentId(command.agentId())).to(Team.valueOf(command.team())).at(command.escalatedAt());
        incidents.save(incident);
        eventPublisher.publish(events);
    }
}
