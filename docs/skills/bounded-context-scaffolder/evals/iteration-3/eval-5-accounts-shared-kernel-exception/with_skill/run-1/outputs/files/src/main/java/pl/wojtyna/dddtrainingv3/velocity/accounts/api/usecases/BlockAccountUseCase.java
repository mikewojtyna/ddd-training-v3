package pl.wojtyna.dddtrainingv3.velocity.accounts.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.AccountNotFound;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.AccountRepository;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BorrowerId;

/// Handles the back office's "Block Account" command.
///
/// DECISION D1 (2026-11-01): builds Rentals' internal `BorrowerId` (shared kernel). See the plan.
@PrimaryPort
public class BlockAccountUseCase {

    private final AccountRepository accounts;
    private final DomainEventPublisher eventPublisher;

    public BlockAccountUseCase(AccountRepository accounts, DomainEventPublisher eventPublisher) {
        this.accounts = accounts;
        this.eventPublisher = eventPublisher;
    }

    public void handle(BlockAccount command) {
        var id = new BorrowerId(command.borrowerId());
        var account = accounts.load(id).orElseThrow(() -> new AccountNotFound(id));
        var events = account.block();
        accounts.save(account);
        eventPublisher.publish(events);
    }
}
