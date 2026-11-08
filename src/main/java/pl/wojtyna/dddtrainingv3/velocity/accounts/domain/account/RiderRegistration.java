package pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account;

import org.jmolecules.ddd.annotation.Factory;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.FluentLanguage.As;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.FluentLanguage.At;

import java.time.Instant;

/// Registers a person: creates their incomplete account. One account per email address (DE-98), which needs to
/// know the other accounts, hence a factory with the `RegisteredEmails` port.
@Factory
public class RiderRegistration {

    private final RegisteredEmails registeredEmails;

    public RiderRegistration(RegisteredEmails registeredEmails) {
        this.registeredEmails = registeredEmails;
    }

    public As<RiderId, At<Instant, NewAccount>> register(EmailAddress email) {
        return riderId -> now -> {
            if (registeredEmails.isRegistered(email)) {
                throw new EmailAlreadyRegistered(email.value() + " already has an account");
            }
            return new NewAccount(Account.registered(riderId, email),
                                  DomainEvents.of(new AccountEvent.AccountRegistered(riderId, email, now)));
        };
    }

    public record NewAccount(Account created, DomainEvents events) {
    }
}
