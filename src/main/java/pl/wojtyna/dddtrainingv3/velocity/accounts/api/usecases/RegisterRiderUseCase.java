package pl.wojtyna.dddtrainingv3.velocity.accounts.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.AccountRepository;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.EmailAddress;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.RiderId;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.RiderRegistration;

import java.time.Clock;
import java.util.UUID;

/// Registers a person: creates their incomplete account and returns the new rider's id.
@PrimaryPort
public class RegisterRiderUseCase {

    private final RiderRegistration registration;
    private final AccountRepository accounts;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;

    public RegisterRiderUseCase(RiderRegistration registration, AccountRepository accounts,
                                DomainEventPublisher eventPublisher, Clock clock) {
        this.registration = registration;
        this.accounts = accounts;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    public String handle(RegisterRider command) {
        var riderId = new RiderId(UUID.randomUUID().toString());
        var newAccount = registration.register(new EmailAddress(command.email())).as(riderId).at(clock.instant());
        accounts.save(newAccount.created());
        eventPublisher.publish(newAccount.events());
        return riderId.value();
    }
}
