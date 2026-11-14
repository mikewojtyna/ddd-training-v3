package pl.wojtyna.dddtrainingv3.velocity.payments;

import au.com.dius.pact.provider.PactVerifyProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import org.junit.jupiter.api.DisplayName;
import pl.wojtyna.dddtrainingv3.velocity.common.contract.MessageProviderVerification;
import pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityContractTest;
import pl.wojtyna.dddtrainingv3.velocity.payments.api.events.PaymentMethodInvalidated;

import static pl.wojtyna.dddtrainingv3.velocity.common.contract.ProviderStates.unique;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.PACT_DIRECTORY;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.PAYMENTS_EVENTS;

/// Provider side of Payments' integration events: verifies Accounts' pact for `PaymentMethodInvalidated` with the
/// event Payments really publishes when the processor reports a rider's card as expired.
@VelocityContractTest
@Provider(PAYMENTS_EVENTS)
@PactFolder(PACT_DIRECTORY)
@DisplayName("Contract: Payments publishes the events its consumers expect")
class PaymentsEventsPactProviderTest extends MessageProviderVerification {

    @PactVerifyProvider("a rider's current card was invalidated by the processor")
    String cardInvalidated() {
        var card = unique("tok_visa");
        var riderId = states.onboardedRider(card);
        states.invalidateCard(card, "EXPIRED");
        return asMessage(events.awaitFirst(PaymentMethodInvalidated.class, event -> event.riderId().equals(riderId)));
    }
}
