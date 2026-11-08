package pl.wojtyna.dddtrainingv3.velocity.payments.infra.config;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.payments.api.queries.PaymentMethodsQueryHandler;
import pl.wojtyna.dddtrainingv3.velocity.payments.api.usecases.ChargeCurrentCardUseCase;
import pl.wojtyna.dddtrainingv3.velocity.payments.api.usecases.InvalidatePaymentMethodUseCase;
import pl.wojtyna.dddtrainingv3.velocity.payments.api.usecases.VerifyPaymentMethodUseCase;
import pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods.PaymentProcessor;
import pl.wojtyna.dddtrainingv3.velocity.payments.infra.adapters.secondary.InMemoryRiderPaymentMethodsRepository;
import pl.wojtyna.dddtrainingv3.velocity.payments.infra.adapters.secondary.IntegrationEventsPublisher;
import pl.wojtyna.dddtrainingv3.velocity.payments.infra.adapters.secondary.paymentprocessoracl.ProcessorPaymentProcessorAdapter;
import pl.wojtyna.dddtrainingv3.velocity.payments.infra.adapters.secondary.paymentprocessoracl.SimulatedPaymentProcessor;

import java.time.Clock;

/// Hand wiring of the Payments context. Domain classes carry no Spring annotations.
@Configuration
public class PaymentsContextConfig {

    @Bean
    public InMemoryRiderPaymentMethodsRepository paymentsRiderPaymentMethodsRepository() {
        return new InMemoryRiderPaymentMethodsRepository();
    }

    @Bean
    public SimulatedPaymentProcessor paymentsSimulatedPaymentProcessor() {
        return new SimulatedPaymentProcessor();
    }

    @Bean
    public PaymentProcessor paymentsPaymentProcessor(SimulatedPaymentProcessor paymentsSimulatedPaymentProcessor) {
        return new ProcessorPaymentProcessorAdapter(paymentsSimulatedPaymentProcessor);
    }

    @Bean
    public VerifyPaymentMethodUseCase paymentsVerifyPaymentMethodUseCase(InMemoryRiderPaymentMethodsRepository paymentsRiderPaymentMethodsRepository,
                                                                         PaymentProcessor paymentsPaymentProcessor,
                                                                         DomainEventPublisher eventPublisher,
                                                                         Clock clock) {
        return new VerifyPaymentMethodUseCase(paymentsRiderPaymentMethodsRepository, paymentsPaymentProcessor, eventPublisher, clock);
    }

    @Bean
    public ChargeCurrentCardUseCase paymentsChargeCurrentCardUseCase(InMemoryRiderPaymentMethodsRepository paymentsRiderPaymentMethodsRepository,
                                                                     PaymentProcessor paymentsPaymentProcessor,
                                                                     DomainEventPublisher eventPublisher,
                                                                     Clock clock) {
        return new ChargeCurrentCardUseCase(paymentsRiderPaymentMethodsRepository, paymentsPaymentProcessor, eventPublisher, clock);
    }

    @Bean
    public InvalidatePaymentMethodUseCase paymentsInvalidatePaymentMethodUseCase(InMemoryRiderPaymentMethodsRepository paymentsRiderPaymentMethodsRepository,
                                                                                 DomainEventPublisher eventPublisher,
                                                                                 Clock clock) {
        return new InvalidatePaymentMethodUseCase(paymentsRiderPaymentMethodsRepository, eventPublisher, clock);
    }

    @Bean
    public PaymentMethodsQueryHandler paymentsPaymentMethodsQueryHandler(InMemoryRiderPaymentMethodsRepository paymentsRiderPaymentMethodsRepository) {
        return new PaymentMethodsQueryHandler(paymentsRiderPaymentMethodsRepository);
    }

    @Bean
    public IntegrationEventsPublisher paymentsIntegrationEventsPublisher(ApplicationEventPublisher applicationEventPublisher) {
        return new IntegrationEventsPublisher(applicationEventPublisher);
    }
}
