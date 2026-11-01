package pl.wojtyna.dddtrainingv3.velocity.accounts.infra.adapters.secondary;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import pl.wojtyna.dddtrainingv3.velocity.accounts.api.events.AccountBlocked;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.AccountEvent;

/// Translates Accounts' domain events into its published language (`api/events`).
@SecondaryAdapter
public class IntegrationEventsPublisher {

    private final ApplicationEventPublisher publisher;

    public IntegrationEventsPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    /// Canvas outbound event "Account Blocked" -> Rentals.
    @EventListener
    public void on(AccountEvent.AccountBlocked event) {
        publisher.publishEvent(new AccountBlocked(event.borrowerId().value()));
    }
}
