package pl.wojtyna.dddtrainingv3.velocity.payments.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods.ChargeId;
import pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods.ChargeReference;
import pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods.PaymentProcessor;
import pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods.RiderId;
import pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods.RiderPaymentMethods;
import pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods.RiderPaymentMethodsEvent.ChargeFailed;
import pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods.RiderPaymentMethodsRepository;

import java.math.RoundingMode;
import java.time.Clock;
import java.util.UUID;

/// Charges an amount to the rider's current card. Charging changes no payment method, so nothing is saved; a rider
/// Payments has never heard of has no current card, and the charge fails with `NO_VALID_PAYMENT_METHOD`.
@PrimaryPort
public class ChargeCurrentCardUseCase {

    private static final CurrencyUnit PLN = CurrencyUnit.of("PLN");

    private final RiderPaymentMethodsRepository repository;
    private final PaymentProcessor processor;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;

    public ChargeCurrentCardUseCase(RiderPaymentMethodsRepository repository, PaymentProcessor processor,
                                    DomainEventPublisher eventPublisher, Clock clock) {
        this.repository = repository;
        this.processor = processor;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    public ChargeResult handle(ChargeCurrentCard command) {
        if (command.amount() == null || command.amount().signum() <= 0) {
            throw new IllegalArgumentException("A charge needs a positive amount, got " + command.amount());
        }
        var riderId = new RiderId(command.riderId());
        var chargeId = new ChargeId(UUID.randomUUID().toString());
        var paymentMethods = repository.load(riderId).orElseGet(() -> RiderPaymentMethods.none(riderId));
        var events = paymentMethods.charge(Money.of(PLN, command.amount(), RoundingMode.UNNECESSARY))
                                   .forReference(new ChargeReference(command.reference()))
                                   .as(chargeId)
                                   .at(clock.instant())
                                   .using(processor);
        eventPublisher.publish(events);
        return events.stream()
                     .filter(ChargeFailed.class::isInstance)
                     .map(ChargeFailed.class::cast)
                     .findFirst()
                     .map(failed -> new ChargeResult(chargeId.value(), "FAILED", failed.reason().name()))
                     .orElseGet(() -> new ChargeResult(chargeId.value(), "SUCCEEDED", null));
    }
}
