package pl.wojtyna.dddtrainingv3.velocity.accounts.api.usecases;

import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.Account;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.AccountNotFound;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.AccountRepository;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.RiderId;

import java.util.function.Function;

/// The load → command → save → publish steps every Accounts use case on an existing account shares.
final class AccountCommands {

    private final AccountRepository accounts;
    private final DomainEventPublisher eventPublisher;

    AccountCommands(AccountRepository accounts, DomainEventPublisher eventPublisher) {
        this.accounts = accounts;
        this.eventPublisher = eventPublisher;
    }

    DomainEvents execute(String riderId, Function<Account, DomainEvents> command) {
        var rider = new RiderId(riderId);
        var account = accounts.load(rider).orElseThrow(() -> new AccountNotFound(rider));
        var events = command.apply(account);
        accounts.save(account);
        eventPublisher.publish(events);
        return events;
    }
}
