package pl.wojtyna.dddtrainingv3.velocity.accounts.infra.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import pl.wojtyna.dddtrainingv3.velocity.accounts.api.events.AccountBlocked;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.AccountEvent;

/**
 * Translates Accounts' internal domain events into its published (integration) events.
 */
public class AccountsIntegrationEventsPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public AccountsIntegrationEventsPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @EventListener
    public void handle(AccountEvent event) {
        switch (event) {
            case AccountEvent.AccountBlocked blocked ->
                applicationEventPublisher.publishEvent(new AccountBlocked(blocked.account().value()));
        }
    }
}
