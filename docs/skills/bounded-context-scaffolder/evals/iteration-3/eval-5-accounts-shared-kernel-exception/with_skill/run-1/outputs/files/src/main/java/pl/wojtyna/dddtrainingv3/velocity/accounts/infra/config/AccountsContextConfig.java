package pl.wojtyna.dddtrainingv3.velocity.accounts.infra.config;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.accounts.api.usecases.BlockAccountUseCase;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.AccountRepository;
import pl.wojtyna.dddtrainingv3.velocity.accounts.infra.adapters.secondary.InMemoryAccountRepository;
import pl.wojtyna.dddtrainingv3.velocity.accounts.infra.adapters.secondary.IntegrationEventsPublisher;

@Configuration
public class AccountsContextConfig {

    @Bean
    public AccountRepository accountsAccountRepository() {
        return new InMemoryAccountRepository();
    }

    @Bean
    public BlockAccountUseCase accountsBlockAccountUseCase(AccountRepository accountsAccountRepository,
                                                           DomainEventPublisher eventPublisher) {
        return new BlockAccountUseCase(accountsAccountRepository, eventPublisher);
    }

    @Bean
    public IntegrationEventsPublisher accountsIntegrationEventsPublisher(ApplicationEventPublisher applicationEventPublisher) {
        return new IntegrationEventsPublisher(applicationEventPublisher);
    }
}
