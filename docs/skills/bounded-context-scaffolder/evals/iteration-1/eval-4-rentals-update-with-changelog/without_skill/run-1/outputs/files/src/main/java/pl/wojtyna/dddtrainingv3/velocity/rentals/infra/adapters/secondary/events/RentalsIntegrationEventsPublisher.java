package pl.wojtyna.dddtrainingv3.velocity.rentals.infra.adapters.secondary.events;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.events.BikeReturned;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BikeRentalEvent;

/**
 * Translates internal Rentals domain events into the published language of Rentals ({@code api.events}).
 */
@SecondaryAdapter
public class RentalsIntegrationEventsPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public RentalsIntegrationEventsPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @EventListener
    public void on(BikeRentalEvent.BikeReturned bikeReturned) {
        applicationEventPublisher.publishEvent(new BikeReturned(bikeReturned.station().value(),
                                                                bikeReturned.bike().id().value(),
                                                                bikeReturned.by().id().value()));
    }
}
