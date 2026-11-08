package pl.wojtyna.dddtrainingv3.velocity.accounts.infra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.accounts.api.queries.AccountStatusQueryHandler;
import pl.wojtyna.dddtrainingv3.velocity.accounts.api.queries.RentalEligibilityQueryHandler;
import pl.wojtyna.dddtrainingv3.velocity.accounts.api.usecases.AddPaymentMethodUseCase;
import pl.wojtyna.dddtrainingv3.velocity.accounts.api.usecases.BlockAccountUseCase;
import pl.wojtyna.dddtrainingv3.velocity.accounts.api.usecases.ConfirmEmailUseCase;
import pl.wojtyna.dddtrainingv3.velocity.accounts.api.usecases.ProvidePersonalDataUseCase;
import pl.wojtyna.dddtrainingv3.velocity.accounts.api.usecases.RegisterRiderUseCase;
import pl.wojtyna.dddtrainingv3.velocity.accounts.api.usecases.SignUserAgreementUseCase;
import pl.wojtyna.dddtrainingv3.velocity.accounts.api.usecases.UnblockAccountUseCase;
import pl.wojtyna.dddtrainingv3.velocity.accounts.api.usecases.VerifyPhoneUseCase;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.NotifyRiderWhenBlockedPolicy;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.PaymentMethodVerifier;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.RiderRegistration;
import pl.wojtyna.dddtrainingv3.velocity.accounts.infra.adapters.primary.paymentsctxacl.PaymentMethodInvalidatedListener;
import pl.wojtyna.dddtrainingv3.velocity.accounts.infra.adapters.secondary.InMemoryAccountRepository;
import pl.wojtyna.dddtrainingv3.velocity.accounts.infra.adapters.secondary.messagingprovideracl.RecordingRiderNotifications;
import pl.wojtyna.dddtrainingv3.velocity.accounts.infra.adapters.secondary.paymentsctxacl.RestPaymentMethodVerifier;
import pl.wojtyna.dddtrainingv3.velocity.accounts.infra.event.AccountsLocalEventsHandler;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.VelocityServiceUrls;

import java.time.Clock;

/// Hand wiring of the Accounts context. Domain classes carry no Spring annotations.
@Configuration
public class AccountsContextConfig {

    @Bean
    public InMemoryAccountRepository accountsAccountRepository() {
        return new InMemoryAccountRepository();
    }

    @Bean
    public PaymentMethodVerifier accountsPaymentMethodVerifier(RestClient velocityRestClient, VelocityServiceUrls velocityServiceUrls) {
        return new RestPaymentMethodVerifier(velocityRestClient, velocityServiceUrls);
    }

    @Bean
    public RecordingRiderNotifications accountsRiderNotifications(Clock clock) {
        return new RecordingRiderNotifications(clock);
    }

    @Bean
    public AccountsLocalEventsHandler accountsLocalEventsHandler(RecordingRiderNotifications accountsRiderNotifications) {
        return new AccountsLocalEventsHandler(new NotifyRiderWhenBlockedPolicy(accountsRiderNotifications));
    }

    @Bean
    public RegisterRiderUseCase accountsRegisterRiderUseCase(InMemoryAccountRepository accountsAccountRepository,
                                                             DomainEventPublisher eventPublisher, Clock clock) {
        return new RegisterRiderUseCase(new RiderRegistration(accountsAccountRepository), accountsAccountRepository,
                                        eventPublisher, clock);
    }

    @Bean
    public ConfirmEmailUseCase accountsConfirmEmailUseCase(InMemoryAccountRepository accountsAccountRepository,
                                                           DomainEventPublisher eventPublisher, Clock clock) {
        return new ConfirmEmailUseCase(accountsAccountRepository, eventPublisher, clock);
    }

    @Bean
    public VerifyPhoneUseCase accountsVerifyPhoneUseCase(InMemoryAccountRepository accountsAccountRepository,
                                                         DomainEventPublisher eventPublisher, Clock clock) {
        return new VerifyPhoneUseCase(accountsAccountRepository, eventPublisher, clock);
    }

    @Bean
    public ProvidePersonalDataUseCase accountsProvidePersonalDataUseCase(InMemoryAccountRepository accountsAccountRepository,
                                                                         DomainEventPublisher eventPublisher, Clock clock) {
        return new ProvidePersonalDataUseCase(accountsAccountRepository, eventPublisher, clock);
    }

    @Bean
    public AddPaymentMethodUseCase accountsAddPaymentMethodUseCase(InMemoryAccountRepository accountsAccountRepository,
                                                                   PaymentMethodVerifier accountsPaymentMethodVerifier,
                                                                   DomainEventPublisher eventPublisher, Clock clock) {
        return new AddPaymentMethodUseCase(accountsAccountRepository, accountsPaymentMethodVerifier, eventPublisher, clock);
    }

    @Bean
    public SignUserAgreementUseCase accountsSignUserAgreementUseCase(InMemoryAccountRepository accountsAccountRepository,
                                                                     DomainEventPublisher eventPublisher, Clock clock) {
        return new SignUserAgreementUseCase(accountsAccountRepository, eventPublisher, clock);
    }

    @Bean
    public BlockAccountUseCase accountsBlockAccountUseCase(InMemoryAccountRepository accountsAccountRepository,
                                                           DomainEventPublisher eventPublisher, Clock clock) {
        return new BlockAccountUseCase(accountsAccountRepository, eventPublisher, clock);
    }

    @Bean
    public UnblockAccountUseCase accountsUnblockAccountUseCase(InMemoryAccountRepository accountsAccountRepository,
                                                               DomainEventPublisher eventPublisher, Clock clock) {
        return new UnblockAccountUseCase(accountsAccountRepository, eventPublisher, clock);
    }

    @Bean
    public AccountStatusQueryHandler accountsAccountStatusQueryHandler(InMemoryAccountRepository accountsAccountRepository) {
        return new AccountStatusQueryHandler(accountsAccountRepository);
    }

    @Bean
    public RentalEligibilityQueryHandler accountsRentalEligibilityQueryHandler(InMemoryAccountRepository accountsAccountRepository) {
        return new RentalEligibilityQueryHandler(accountsAccountRepository);
    }

    @Bean
    public PaymentMethodInvalidatedListener accountsPaymentMethodInvalidatedListener(BlockAccountUseCase accountsBlockAccountUseCase) {
        return new PaymentMethodInvalidatedListener(accountsBlockAccountUseCase);
    }
}
