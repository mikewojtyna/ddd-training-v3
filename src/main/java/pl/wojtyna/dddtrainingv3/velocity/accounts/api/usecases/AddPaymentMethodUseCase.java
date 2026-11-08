package pl.wojtyna.dddtrainingv3.velocity.accounts.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.AccountEvent;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.AccountRepository;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.CardToken;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.PaymentMethodVerifier;

import java.time.Clock;

/// Adds the rider's card: the account checks the onboarding order, then asks Payments to verify the card (only when
/// the order allows it), completes the payment step and lifts an *invalid card* block on a verified card.
@PrimaryPort
public class AddPaymentMethodUseCase {

    private final AccountCommands commands;
    private final PaymentMethodVerifier verifier;
    private final Clock clock;

    public AddPaymentMethodUseCase(AccountRepository accounts, PaymentMethodVerifier verifier,
                                   DomainEventPublisher eventPublisher, Clock clock) {
        this.commands = new AccountCommands(accounts, eventPublisher);
        this.verifier = verifier;
        this.clock = clock;
    }

    public PaymentMethodAddition handle(AddPaymentMethod command) {
        var card = new CardToken(command.cardToken());
        var events = commands.execute(command.riderId(),
                                      account -> account.addPaymentMethod(card).at(clock.instant()).using(verifier));
        return events.stream()
                     .filter(AccountEvent.PaymentMethodAdded.class::isInstance)
                     .map(AccountEvent.PaymentMethodAdded.class::cast)
                     .findFirst()
                     .map(added -> new PaymentMethodAddition(true, added.paymentMethodId().value()))
                     .orElse(new PaymentMethodAddition(false, null));
    }
}
