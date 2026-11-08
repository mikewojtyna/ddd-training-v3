package pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.secondary;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.debt.RiderDebtEvent;

/// Publishes Settlement's published language (listener: Debt Recovery): `api.events.RentalFeeUnpaid` and
/// `api.events.OutstandingDebtCleared`. Ride Settled and Charge Attempt Failed have no listener outside Settlement.
@SecondaryAdapter
public class IntegrationEventsPublisher {

    private final ApplicationEventPublisher publisher;

    public IntegrationEventsPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @EventListener
    public void on(RiderDebtEvent.RentalFeeUnpaid event) {
        publisher.publishEvent(new pl.wojtyna.dddtrainingv3.velocity.settlement.api.events.RentalFeeUnpaid(
            event.rider().value(), event.session().value(), event.amount().getAmount(),
            event.outstandingDebt().getAmount(), event.occurredAt()));
    }

    @EventListener
    public void on(RiderDebtEvent.OutstandingDebtCleared event) {
        publisher.publishEvent(new pl.wojtyna.dddtrainingv3.velocity.settlement.api.events.OutstandingDebtCleared(
            event.rider().value(), event.clearedAt()));
    }
}
