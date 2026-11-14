package pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.secondary.paymentsctxacl;

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
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.Charge;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.ChargeOutcome;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.RiderId;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.PACT_DIRECTORY;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.PAYMENTS;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.SETTLEMENT;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.urlsOf;
import static pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.ChargeFailureReason.DECLINED;
import static pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.ChargeFailureReason.NO_VALID_PAYMENT_METHOD;
import static pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.Pln.pln;

/// Consumer side of `Payments [U,OHS,PL]->[D,CF] Settlement`: Settlement asks Payments to *Charge* the rider's current
/// card. A failed charge is an answer (`status: FAILED` with `failureReason`), not an error.
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = PAYMENTS, pactVersion = PactSpecVersion.V4)
@PactDirectory(PACT_DIRECTORY)
@DisplayName("Contract: Settlement → Payments (charge)")
class PaymentsChargePactTest {

    private static final String RIDER = "rider-anna";
    private static final String RIDER_WITHOUT_CARD = "rider-payments-never-heard-of";
    private static final String REFERENCE = "ride-S-1";

    @Pact(consumer = SETTLEMENT)
    V4Pact chargeSucceeds(PactDslWithProvider builder) {
        return builder.given("a rider whose card works")
                      .uponReceiving("a charge to a card that works")
                      .path("/payments/charges")
                      .method("POST")
                      .headers(Map.of("Content-Type", "application/json"))
                      .body(chargeOf(RIDER, true))
                      .willRespondWith()
                      .status(200)
                      .body(new PactDslJsonBody().stringValue("status", "SUCCEEDED"))
                      .toPact(V4Pact.class);
    }

    @Pact(consumer = SETTLEMENT)
    V4Pact chargeIsDeclined(PactDslWithProvider builder) {
        return builder.given("a rider whose card has no funds")
                      .uponReceiving("a charge to a card without funds")
                      .path("/payments/charges")
                      .method("POST")
                      .headers(Map.of("Content-Type", "application/json"))
                      .body(chargeOf(RIDER, true))
                      .willRespondWith()
                      .status(200)
                      .body(new PactDslJsonBody().stringValue("status", "FAILED").stringValue("failureReason", "DECLINED"))
                      .toPact(V4Pact.class);
    }

    @Pact(consumer = SETTLEMENT)
    V4Pact riderHasNoValidPaymentMethod(PactDslWithProvider builder) {
        return builder.uponReceiving("a charge for a rider without a payment method")
                      .path("/payments/charges")
                      .method("POST")
                      .headers(Map.of("Content-Type", "application/json"))
                      .body(chargeOf(RIDER_WITHOUT_CARD, false))
                      .willRespondWith()
                      .status(200)
                      .body(new PactDslJsonBody().stringValue("status", "FAILED")
                                                 .stringValue("failureReason", "NO_VALID_PAYMENT_METHOD"))
                      .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "chargeSucceeds")
    void chargeSucceeds(MockServer payments) {
        assertThat(cards(payments).charge(charge(RIDER))).isEqualTo(ChargeOutcome.success());
    }

    @Test
    @PactTestFor(pactMethod = "chargeIsDeclined")
    void chargeIsDeclined(MockServer payments) {
        assertThat(cards(payments).charge(charge(RIDER))).isEqualTo(ChargeOutcome.failure(DECLINED));
    }

    @Test
    @PactTestFor(pactMethod = "riderHasNoValidPaymentMethod")
    void riderHasNoValidPaymentMethod(MockServer payments) {
        assertThat(cards(payments).charge(charge(RIDER_WITHOUT_CARD))).isEqualTo(ChargeOutcome.failure(NO_VALID_PAYMENT_METHOD));
    }

    /// The rider id comes from the provider state when Payments has to know the rider's card.
    private static PactDslJsonBody chargeOf(String rider, boolean riderFromProviderState) {
        var body = new PactDslJsonBody();
        if (riderFromProviderState) {
            body.valueFromProviderState("riderId", "${riderId}", rider);
        } else {
            body.stringValue("riderId", rider);
        }
        return body.decimalType("amount", new BigDecimal("3.00")).stringType("reference", REFERENCE);
    }

    private static Charge charge(String rider) {
        return new Charge(new RiderId(rider), pln("3.00"), REFERENCE);
    }

    private static PaymentsRiderCards cards(MockServer payments) {
        return new PaymentsRiderCards(RestClient.builder().build(), urlsOf(payments.getUrl()));
    }
}
