package pl.wojtyna.dddtrainingv3.velocity.pricing.infra.adapters.secondary.events;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import pl.wojtyna.dddtrainingv3.velocity.pricing.api.events.RidePriced;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricecatalog.PricingEvent;

/**
 * Translates the internal Ride Priced domain event into Pricing's published language for Settlement.
 */
@SecondaryAdapter
public class RidePricedIntegrationEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public RidePricedIntegrationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @EventListener
    public void on(PricingEvent.RidePriced event) {
        applicationEventPublisher.publishEvent(new RidePriced(event.sessionId().value(),
                                                              event.riderId().value(),
                                                              event.appliedPriceList().value(),
                                                              event.totalFee().getAmount(),
                                                              event.totalFee().getCurrencyUnit().getCode()));
    }
}
