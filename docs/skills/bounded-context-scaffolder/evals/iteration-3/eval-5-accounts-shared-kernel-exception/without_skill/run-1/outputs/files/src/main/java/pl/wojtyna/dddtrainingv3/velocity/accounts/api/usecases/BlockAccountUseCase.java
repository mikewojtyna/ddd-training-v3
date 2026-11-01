package pl.wojtyna.dddtrainingv3.velocity.accounts.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.AccountNotFoundException;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.AccountRepository;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BorrowerId;

/**
 * Block Account command, issued by the back office.
 */
@PrimaryPort
public class BlockAccountUseCase {

    private final AccountRepository accountRepository;
    private final DomainEventPublisher eventPublisher;

    public BlockAccountUseCase(AccountRepository accountRepository, DomainEventPublisher eventPublisher) {
        this.accountRepository = accountRepository;
        this.eventPublisher = eventPublisher;
    }

    public void block(BorrowerId accountId) {
        var account = accountRepository.load(accountId)
                                       .orElseThrow(() -> new AccountNotFoundException(
                                           "Account %s not found".formatted(accountId.value())));
        var events = account.block();
        accountRepository.save(account);
        eventPublisher.publish(events);
    }
}
