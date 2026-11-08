package pl.wojtyna.dddtrainingv3.velocity.accounts.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.AccountRepository;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.BlockReason;

import java.time.Clock;

/// Places a block. The block notification follows from the published Account Blocked
/// (`NotifyRiderWhenBlockedPolicy`); a reason already in place publishes nothing (DE-94).
@PrimaryPort
public class BlockAccountUseCase {

    private final AccountCommands commands;
    private final Clock clock;

    public BlockAccountUseCase(AccountRepository accounts, DomainEventPublisher eventPublisher, Clock clock) {
        this.commands = new AccountCommands(accounts, eventPublisher);
        this.clock = clock;
    }

    public void handle(BlockAccount command) {
        var reason = BlockReason.valueOf(command.reason());
        commands.execute(command.riderId(), account -> account.placeBlock(reason).at(clock.instant()));
    }
}
