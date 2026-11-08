package pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account;

import pl.wojtyna.dddtrainingv3.common.domain.DomainPolicy;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.AccountEvent.AccountBlocked;

/// Whenever a block reason is placed on an account, the rider gets a block notification for it (DE-7). A reason
/// already in place produces no Account Blocked, so no second notification either (DE-94).
public class NotifyRiderWhenBlockedPolicy implements DomainPolicy {

    private final RiderNotifications notifications;

    public NotifyRiderWhenBlockedPolicy(RiderNotifications notifications) {
        this.notifications = notifications;
    }

    public void handle(AccountBlocked accountBlocked) {
        notifications.sendBlockNotification(accountBlocked.riderId(), accountBlocked.reason());
    }
}
