package pl.wojtyna.dddtrainingv3.velocity.customersupport.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.BikeId;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.IncidentId;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.IncidentNotFound;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.Incidents;

/// Fleet Maintenance fixed the incident's bike: the fleet issue is resolved with "bike fixed" (DE-55). Idempotent.
@PrimaryPort
public class ResolveFleetIssueUseCase {

    private final Incidents incidents;
    private final DomainEventPublisher eventPublisher;

    public ResolveFleetIssueUseCase(Incidents incidents, DomainEventPublisher eventPublisher) {
        this.incidents = incidents;
        this.eventPublisher = eventPublisher;
    }

    public void handle(ResolveFleetIssue command) {
        var id = new IncidentId(command.incidentId());
        var incident = incidents.load(id).orElseThrow(() -> new IncidentNotFound(id));
        var events = incident.resolveFleetIssueOf(new BikeId(command.bikeId())).at(command.resolvedAt());
        if (events.stream().findAny().isEmpty()) {
            return;
        }
        incidents.save(incident);
        eventPublisher.publish(events);
    }
}
