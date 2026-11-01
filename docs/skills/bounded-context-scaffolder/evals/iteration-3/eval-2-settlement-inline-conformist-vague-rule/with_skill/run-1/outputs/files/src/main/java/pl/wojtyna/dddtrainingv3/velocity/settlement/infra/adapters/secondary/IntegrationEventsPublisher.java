package pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.secondary;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import pl.wojtyna.dddtrainingv3.velocity.settlement.api.events.DebtRecorded;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement.SettlementEvent;

/// Translates Settlement's domain events into its published language.
@SecondaryAdapter
public class IntegrationEventsPublisher {

    private final ApplicationEventPublisher publisher;

    public IntegrationEventsPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    /// Canvas outbound event "Debt Recorded" to DebtRecovery (R1).
    @EventListener
    public void on(SettlementEvent.OutstandingDebtRecorded event) {
        var amount = event.debt().amount();
        publisher.publishEvent(new DebtRecorded(event.rideId().value(),
                                                event.riderId().value(),
                                                amount.getAmount(),
                                                amount.getCurrencyUnit().getCode()));
    }
}
