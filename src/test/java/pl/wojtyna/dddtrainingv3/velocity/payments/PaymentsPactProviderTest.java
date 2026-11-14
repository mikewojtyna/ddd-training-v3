package pl.wojtyna.dddtrainingv3.velocity.payments;

import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import org.junit.jupiter.api.DisplayName;
import pl.wojtyna.dddtrainingv3.velocity.common.contract.HttpProviderVerification;
import pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityContractTest;

import java.util.Map;

import static pl.wojtyna.dddtrainingv3.velocity.common.contract.ProviderStates.unique;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.PACT_DIRECTORY;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.PAYMENTS;

/// Provider side of Payments' Open Host Service: verifies the pacts of Accounts (*Verify Payment Method*) and
/// Settlement (*Charge*) against the running application and its simulated card processor.
@VelocityContractTest
@Provider(PAYMENTS)
@PactFolder(PACT_DIRECTORY)
@DisplayName("Contract: Payments honours its consumers' pacts")
class PaymentsPactProviderTest extends HttpProviderVerification {

    /// The simulated processor accepts every card token that doesn't start with `tok_declined` or `tok_nofunds`.
    @State("the processor accepts the card")
    void processorAcceptsTheCard() {
    }

    /// The simulated processor declines pre-authorizations of card tokens starting with `tok_declined`.
    @State("the processor declines the card")
    void processorDeclinesTheCard() {
    }

    @State("a rider whose card works")
    Map<String, Object> riderWhoseCardWorks() {
        var riderId = unique("rider");
        states.verifyCardInPayments(riderId, unique("tok_visa"));
        return Map.of("riderId", riderId);
    }

    /// Charges fail for card tokens starting with `tok_nofunds`.
    @State("a rider whose card has no funds")
    Map<String, Object> riderWhoseCardHasNoFunds() {
        var riderId = unique("rider");
        states.verifyCardInPayments(riderId, unique("tok_nofunds"));
        return Map.of("riderId", riderId);
    }
}
