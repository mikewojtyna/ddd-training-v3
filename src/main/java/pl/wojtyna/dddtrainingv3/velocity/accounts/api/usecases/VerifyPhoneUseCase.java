package pl.wojtyna.dddtrainingv3.velocity.accounts.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.AccountRepository;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.PhoneNumber;

import java.time.Clock;

@PrimaryPort
public class VerifyPhoneUseCase {

    private final AccountCommands commands;
    private final Clock clock;

    public VerifyPhoneUseCase(AccountRepository accounts, DomainEventPublisher eventPublisher, Clock clock) {
        this.commands = new AccountCommands(accounts, eventPublisher);
        this.clock = clock;
    }

    public void handle(VerifyPhone command) {
        var phone = new PhoneNumber(command.phoneNumber());
        commands.execute(command.riderId(), account -> account.verifyPhone(phone).at(clock.instant()));
    }
}
