package pl.wojtyna.dddtrainingv3.velocity.rentals.infra.adapters.secondary;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.events.BikeReturned;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BikeRentalEvent;

/// Translates Rentals' domain events into its published language (`api.events`).
@SecondaryAdapter
public class IntegrationEventsPublisher {

    private final ApplicationEventPublisher publisher;

    public IntegrationEventsPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    /// Canvas outbound event "Bike Returned" → Pricing.
    @EventListener
    public void on(BikeRentalEvent.BikeReturned event) {
        publisher.publishEvent(new BikeReturned(event.stationId().value(),
                                                event.bikeId().value(),
                                                event.by().id().value()));
    }
}
