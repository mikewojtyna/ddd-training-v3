package pl.wojtyna.dddtrainingv3.velocity.pricing.infra.adapters.secondary;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import pl.wojtyna.dddtrainingv3.velocity.pricing.api.events.RidePriced;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist.PriceListEvent;

/// Translates Pricing's domain events into its published language (`api.events`).
@SecondaryAdapter
public class IntegrationEventsPublisher {

    private final ApplicationEventPublisher publisher;

    public IntegrationEventsPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @EventListener
    public void on(PriceListEvent.RidePriced event) {
        var totalFee = event.totalFee().amount();
        publisher.publishEvent(new RidePriced(event.rentalSessionId().value(),
                                              event.riderId().value(),
                                              totalFee.getAmount(),
                                              totalFee.getCurrencyUnit().getCode(),
                                              event.priceListId().value()));
    }
}
