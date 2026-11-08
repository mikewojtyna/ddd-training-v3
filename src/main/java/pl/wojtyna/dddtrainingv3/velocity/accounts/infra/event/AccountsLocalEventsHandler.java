package pl.wojtyna.dddtrainingv3.velocity.accounts.infra.event;

import org.springframework.context.event.EventListener;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.AccountEvent;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.NotifyRiderWhenBlockedPolicy;

/// Routes Accounts' own domain events to its policies (in-process, synchronous: the notification goes out in the
/// same request that placed the block).
public class AccountsLocalEventsHandler {

    private final NotifyRiderWhenBlockedPolicy notifyRiderWhenBlocked;

    public AccountsLocalEventsHandler(NotifyRiderWhenBlockedPolicy notifyRiderWhenBlocked) {
        this.notifyRiderWhenBlocked = notifyRiderWhenBlocked;
    }

    @EventListener
    public void on(AccountEvent.AccountBlocked event) {
        notifyRiderWhenBlocked.handle(event);
    }
}
