package pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.secondary;

import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement.SettlementEvent;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;

/// Translates Settlement's domain events into its published language (`api.events`).
@SecondaryAdapter
public class IntegrationEventsPublisher {

    private final ApplicationEventPublisher publisher;

    public IntegrationEventsPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @EventListener
    public void on(SettlementEvent.DebtRecorded event) {
        var amount = event.outstandingDebt().amount();
        publisher.publishEvent(new pl.wojtyna.dddtrainingv3.velocity.settlement.api.events.DebtRecorded(event.rideId().value(),
                                                              event.riderId().value(),
                                                              amount.getAmount(),
                                                              amount.getCurrencyUnit().getCode()));
    }
}
