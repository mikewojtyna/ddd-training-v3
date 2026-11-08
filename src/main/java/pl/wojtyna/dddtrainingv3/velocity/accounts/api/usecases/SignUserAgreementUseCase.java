package pl.wojtyna.dddtrainingv3.velocity.accounts.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.AccountRepository;

import java.time.Clock;

@PrimaryPort
public class SignUserAgreementUseCase {

    private final AccountCommands commands;
    private final Clock clock;

    public SignUserAgreementUseCase(AccountRepository accounts, DomainEventPublisher eventPublisher, Clock clock) {
        this.commands = new AccountCommands(accounts, eventPublisher);
        this.clock = clock;
    }

    public void handle(SignUserAgreement command) {
        commands.execute(command.riderId(), account -> account.signUserAgreement(clock.instant()));
    }
}
