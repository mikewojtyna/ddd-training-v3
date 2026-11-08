package pl.wojtyna.dddtrainingv3.velocity.accounts.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.AccountRepository;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.PersonalData;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/// Records the rider's personal data. "The day" of the age limit is today's date in VeloCity's city, Warsaw
/// (plan `rider-provides-personal-data` D5).
@PrimaryPort
public class ProvidePersonalDataUseCase {

    private static final ZoneId VELOCITY_CITY = ZoneId.of("Europe/Warsaw");

    private final AccountCommands commands;
    private final Clock clock;

    public ProvidePersonalDataUseCase(AccountRepository accounts, DomainEventPublisher eventPublisher, Clock clock) {
        this.commands = new AccountCommands(accounts, eventPublisher);
        this.clock = clock;
    }

    public void handle(ProvidePersonalData command) {
        var data = new PersonalData(command.fullName(), command.dateOfBirth(), command.countryOfResidency());
        var now = clock.instant();
        var today = LocalDate.ofInstant(now, VELOCITY_CITY);
        commands.execute(command.riderId(), account -> account.providePersonalData(data).on(today).at(now));
    }
}
