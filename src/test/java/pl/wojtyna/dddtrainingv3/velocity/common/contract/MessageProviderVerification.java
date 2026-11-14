package pl.wojtyna.dddtrainingv3.velocity.common.contract;

import au.com.dius.pact.provider.junit5.MessageTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import pl.wojtyna.dddtrainingv3.common.integration.IntegrationEvent;

import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.EVENT_JSON;

/// Base of an integration-event provider verification: every `@PactVerifyProvider` method of a subclass makes the real
/// scenario happen over the contexts' REST resources, waits for the event the provider context really published on the
/// in-process bus and hands it to Pact as JSON, the way a message broker would carry it.
public abstract class MessageProviderVerification {

    @Autowired
    protected ProviderStates states;

    @Autowired
    protected CapturedIntegrationEvents events;

    @BeforeEach
    void targetTheMessages(PactVerificationContext context) {
        context.setTarget(new MessageTestTarget());
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verify(PactVerificationContext context) {
        context.verifyInteraction();
    }

    protected static String asMessage(IntegrationEvent event) {
        return EVENT_JSON.writeValueAsString(event);
    }
}
