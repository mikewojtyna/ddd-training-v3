package pl.wojtyna.dddtrainingv3.velocity.payments.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods.CardToken;
import pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods.PaymentMethodId;
import pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods.PaymentProcessor;
import pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods.RiderId;
import pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods.RiderPaymentMethods;
import pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods.RiderPaymentMethodsEvent.PaymentMethodVerified;
import pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods.RiderPaymentMethodsRepository;

import java.time.Clock;
import java.util.UUID;

/// Verifies a rider's card with the processor; a declined card is a result, not a rejection (plan D5).
@PrimaryPort
public class VerifyPaymentMethodUseCase {

    private final RiderPaymentMethodsRepository repository;
    private final PaymentProcessor processor;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;

    public VerifyPaymentMethodUseCase(RiderPaymentMethodsRepository repository, PaymentProcessor processor,
                                      DomainEventPublisher eventPublisher, Clock clock) {
        this.repository = repository;
        this.processor = processor;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    public PaymentMethodVerification handle(VerifyPaymentMethod command) {
        var riderId = new RiderId(command.riderId());
        var paymentMethodId = new PaymentMethodId(UUID.randomUUID().toString());
        var paymentMethods = repository.load(riderId).orElseGet(() -> RiderPaymentMethods.none(riderId));
        var events = paymentMethods.verify(new CardToken(command.cardToken()))
                                   .as(paymentMethodId)
                                   .at(clock.instant())
                                   .using(processor);
        repository.save(paymentMethods);
        eventPublisher.publish(events);
        var verified = events.hasOccurredEventOfType(PaymentMethodVerified.class);
        return new PaymentMethodVerification(paymentMethodId.value(), verified, verified ? null : "DECLINED");
    }
}
