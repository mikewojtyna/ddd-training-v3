package pl.wojtyna.dddtrainingv3.velocity.rentals.infra.adapters.secondary;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BikeRentalEvent;

/// Translates Rentals domain events into the published integration events in `api.events`.
@SecondaryAdapter
public class IntegrationEventsPublisher {

    private final ApplicationEventPublisher publisher;

    public IntegrationEventsPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @EventListener
    public void on(BikeRentalEvent.BikeReturned event) {
        publisher.publishEvent(new pl.wojtyna.dddtrainingv3.velocity.rentals.api.events.BikeReturned(
            event.to().value(), event.bike().value()));
    }
}
