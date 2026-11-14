package pl.wojtyna.dddtrainingv3.velocity.common.contract;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;

/// Base of a REST provider verification: replays every interaction of the provider's pacts against the running
/// application. Subclasses add `@VelocityContractTest`, `@Provider`, `@PactFolder` and one `@State` method per provider
/// state, which returns the ids it created for the pact's `fromProviderState` expressions.
public abstract class HttpProviderVerification {

    @LocalServerPort
    private int port;

    @Autowired
    protected ProviderStates states;

    @BeforeEach
    void targetTheRunningApplication(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", port));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verify(PactVerificationContext context) {
        context.verifyInteraction();
    }
}
