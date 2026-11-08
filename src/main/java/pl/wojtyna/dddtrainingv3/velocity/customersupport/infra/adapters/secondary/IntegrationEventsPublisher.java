package pl.wojtyna.dddtrainingv3.velocity.customersupport.infra.adapters.secondary;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.IncidentEvent;

/// Publishes Customer Support's published language: maps domain events to `api.events` records.
@SecondaryAdapter
public class IntegrationEventsPublisher {

    private final ApplicationEventPublisher publisher;

    public IntegrationEventsPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @EventListener
    public void on(IncidentEvent.FleetIssueReported event) {
        publisher.publishEvent(new pl.wojtyna.dddtrainingv3.velocity.customersupport.api.events.FleetIssueReported(
            event.incidentId().value(), event.bikeId().value(), event.description().value(), event.reportedAt()));
    }
}
