package pl.wojtyna.dddtrainingv3.velocity.accounts.infra.adapters.secondary.paymentsctxacl;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.annotations.PactDirectory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.web.client.RestClient;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.CardToken;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.CardVerification;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.PaymentMethodId;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.RiderId;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.ACCOUNTS;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.PACT_DIRECTORY;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.PAYMENTS;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.urlsOf;

/// Consumer side of `Payments [U,OHS,PL]->[D,CF] Accounts`: Accounts asks Payments to *Verify Payment Method*. A
/// declined pre-authorization is an answer (`verified=false`), not an error. The simulated processor declines tokens
/// starting with `tok_declined`.
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = PAYMENTS, pactVersion = PactSpecVersion.V4)
@PactDirectory(PACT_DIRECTORY)
@DisplayName("Contract: Accounts → Payments (verify payment method)")
class PaymentMethodVerifierPactTest {

    private static final String RIDER = "rider-pact-card-holder";

    @Pact(consumer = ACCOUNTS)
    V4Pact cardIsVerified(PactDslWithProvider builder) {
        return builder.given("the processor accepts the card")
                      .uponReceiving("a payment method verification of a valid card")
                      .path("/payments/payment-methods")
                      .method("POST")
                      .headers(Map.of("Content-Type", "application/json"))
                      .body(new PactDslJsonBody().stringValue("riderId", RIDER).stringValue("cardToken", "tok_visa_pact"))
                      .willRespondWith()
                      .status(200)
                      .body(new PactDslJsonBody().booleanValue("verified", true).stringType("paymentMethodId", "pm-1"))
                      .toPact(V4Pact.class);
    }

    @Pact(consumer = ACCOUNTS)
    V4Pact cardIsDeclined(PactDslWithProvider builder) {
        return builder.given("the processor declines the card")
                      .uponReceiving("a payment method verification of a declined card")
                      .path("/payments/payment-methods")
                      .method("POST")
                      .headers(Map.of("Content-Type", "application/json"))
                      .body(new PactDslJsonBody().stringValue("riderId", RIDER).stringValue("cardToken", "tok_declined_pact"))
                      .willRespondWith()
                      .status(200)
                      .body(new PactDslJsonBody().booleanValue("verified", false))
                      .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "cardIsVerified")
    void cardIsVerified(MockServer payments) {
        var verification = verifier(payments).verify(new RiderId(RIDER), new CardToken("tok_visa_pact"));

        assertThat(verification).isEqualTo(CardVerification.verifiedAs(new PaymentMethodId("pm-1")));
    }

    @Test
    @PactTestFor(pactMethod = "cardIsDeclined")
    void cardIsDeclined(MockServer payments) {
        var verification = verifier(payments).verify(new RiderId(RIDER), new CardToken("tok_declined_pact"));

        assertThat(verification).isEqualTo(CardVerification.declined());
    }

    private static RestPaymentMethodVerifier verifier(MockServer payments) {
        return new RestPaymentMethodVerifier(RestClient.builder().build(), urlsOf(payments.getUrl()));
    }
}
