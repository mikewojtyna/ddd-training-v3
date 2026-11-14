package pl.wojtyna.dddtrainingv3.velocity.accounts.infra.adapters.primary.paymentsctxacl;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.consumer.junit5.ProviderType;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.V4Interaction;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.annotations.PactDirectory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import pl.wojtyna.dddtrainingv3.velocity.accounts.api.usecases.BlockAccount;
import pl.wojtyna.dddtrainingv3.velocity.accounts.api.usecases.BlockAccountUseCase;
import pl.wojtyna.dddtrainingv3.velocity.payments.api.events.PaymentMethodInvalidated;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.ACCOUNTS;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.EVENT_JSON;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.PACT_DIRECTORY;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.PAYMENTS_EVENTS;

/// Consumer side of Payments' integration event `PaymentMethodInvalidated` (CML `Payments [U,OHS,PL]->[D,CF] Accounts`):
/// whenever the processor reports the rider's current card as unusable, Accounts blocks the account for an *invalid
/// card*. Accounts needs the rider and the payment method (logged when the rider has no account).
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = PAYMENTS_EVENTS, providerType = ProviderType.ASYNCH, pactVersion = PactSpecVersion.V4)
@PactDirectory(PACT_DIRECTORY)
@DisplayName("Contract: Payments → Accounts (PaymentMethodInvalidated)")
class PaymentMethodInvalidatedPactTest {

    @Pact(consumer = ACCOUNTS)
    V4Pact cardInvalidated(MessagePactBuilder builder) {
        return builder.expectsToReceive("a rider's current card was invalidated by the processor")
                      .withContent(new PactDslJsonBody().stringType("riderId", "rider-anna").stringType("paymentMethodId", "pm-1"))
                      .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "cardInvalidated")
    void blocksTheAccountForAnInvalidCard(V4Interaction.AsynchronousMessage message) throws Exception {
        var blockAccount = mock(BlockAccountUseCase.class);

        new PaymentMethodInvalidatedListener(blockAccount).on(EVENT_JSON.readValue(message.contentsAsBytes(), PaymentMethodInvalidated.class));

        verify(blockAccount).handle(new BlockAccount("rider-anna", "INVALID_CARD"));
    }
}
