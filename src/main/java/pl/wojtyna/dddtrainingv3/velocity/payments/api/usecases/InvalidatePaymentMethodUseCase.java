package pl.wojtyna.dddtrainingv3.velocity.payments.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods.CardToken;
import pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods.InvalidationReason;
import pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods.RiderPaymentMethodsRepository;

import java.time.Clock;

/// Records the processor's report that a card is no longer usable. An unknown card changes nothing.
@PrimaryPort
public class InvalidatePaymentMethodUseCase {

    private final RiderPaymentMethodsRepository repository;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;

    public InvalidatePaymentMethodUseCase(RiderPaymentMethodsRepository repository, DomainEventPublisher eventPublisher,
                                          Clock clock) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    public void handle(InvalidatePaymentMethod command) {
        var card = new CardToken(command.cardToken());
        var reason = InvalidationReason.valueOf(command.reason());
        var now = clock.instant();
        for (var paymentMethods : repository.loadHolding(card)) {
            var events = paymentMethods.invalidate(card).because(reason).at(now);
            repository.save(paymentMethods);
            eventPublisher.publish(events);
        }
    }
}
