package pl.wojtyna.dddtrainingv3.velocity.customersupport.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.AgentId;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.IncidentId;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.IncidentNotFound;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.Incidents;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.RejectionReason;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.ReopeningWindow;

import java.time.ZoneId;

/// The assigned agent rejects an incident with a reason; the reopening window is counted in the city's time zone (plan D5).
@PrimaryPort
public class RejectIncidentUseCase {

    private final Incidents incidents;
    private final DomainEventPublisher eventPublisher;
    private final ZoneId cityZone;

    public RejectIncidentUseCase(Incidents incidents, DomainEventPublisher eventPublisher, ZoneId cityZone) {
        this.incidents = incidents;
        this.eventPublisher = eventPublisher;
        this.cityZone = cityZone;
    }

    public void handle(RejectIncident command) {
        var id = new IncidentId(command.incidentId());
        var incident = incidents.load(id).orElseThrow(() -> new IncidentNotFound(id));
        var events = incident.rejectBy(new AgentId(command.agentId()))
                             .because(new RejectionReason(command.reason()))
                             .within(ReopeningWindow.afterRejectionAt(command.rejectedAt(), cityZone));
        incidents.save(incident);
        eventPublisher.publish(events);
    }
}
