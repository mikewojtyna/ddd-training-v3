package pl.wojtyna.dddtrainingv3.velocity.payments.infra.adapters.secondary;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods.RiderPaymentMethodsEvent;

/// Publishes Payments' published language: only the current card's invalidation leaves Payments (DE-73), as
/// `api.events.PaymentMethodInvalidated`. The other domain events have no listener outside Payments yet.
@SecondaryAdapter
public class IntegrationEventsPublisher {

    private final ApplicationEventPublisher publisher;

    public IntegrationEventsPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @EventListener
    public void on(RiderPaymentMethodsEvent.PaymentMethodInvalidated event) {
        publisher.publishEvent(new pl.wojtyna.dddtrainingv3.velocity.payments.api.events.PaymentMethodInvalidated(
            event.riderId().value(), event.paymentMethodId().value(), event.reason().name(), event.invalidatedAt()));
    }
}
