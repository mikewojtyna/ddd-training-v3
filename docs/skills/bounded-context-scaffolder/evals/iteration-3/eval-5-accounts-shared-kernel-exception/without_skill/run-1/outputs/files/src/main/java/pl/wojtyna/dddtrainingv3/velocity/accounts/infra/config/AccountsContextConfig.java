package pl.wojtyna.dddtrainingv3.velocity.accounts.infra.config;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.accounts.api.usecases.BlockAccountUseCase;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.AccountRepository;
import pl.wojtyna.dddtrainingv3.velocity.accounts.infra.adapters.secondary.repository.InMemoryAccountRepository;
import pl.wojtyna.dddtrainingv3.velocity.accounts.infra.event.AccountsIntegrationEventsPublisher;

@Configuration
public class AccountsContextConfig {

    // Bean names carry a "velocityAccounts" prefix because CrowdSorcery already defines an "accountRepository" bean.
    @Bean
    public AccountRepository velocityAccountsAccountRepository() {
        return new InMemoryAccountRepository();
    }

    @Bean
    public BlockAccountUseCase velocityAccountsBlockAccountUseCase(AccountRepository velocityAccountsAccountRepository,
                                                                   DomainEventPublisher eventPublisher) {
        return new BlockAccountUseCase(velocityAccountsAccountRepository, eventPublisher);
    }

    @Bean
    public AccountsIntegrationEventsPublisher velocityAccountsIntegrationEventsPublisher(
        ApplicationEventPublisher applicationEventPublisher) {
        return new AccountsIntegrationEventsPublisher(applicationEventPublisher);
    }
}
