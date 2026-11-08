package pl.wojtyna.dddtrainingv3.velocity.accounts.infra.adapters.secondary.paymentsctxacl;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.CardToken;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.CardVerification;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.PaymentMethodId;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.PaymentMethodVerifier;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.RiderId;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.VelocityServiceUrls;

/// Calls Payments' *Verify Payment Method*. `verified=false` (a declined pre-authorization) becomes a declined
/// `CardVerification`, not an exception (plan `rider-adds-payment-method` D5).
@SecondaryAdapter
public class RestPaymentMethodVerifier implements PaymentMethodVerifier {

    private record VerifyPaymentMethodRequest(String riderId, String cardToken) {
    }

    private record VerifyPaymentMethodResponse(String paymentMethodId, boolean verified, String declineReason) {
    }

    private final RestClient restClient;
    private final VelocityServiceUrls urls;

    public RestPaymentMethodVerifier(RestClient restClient, VelocityServiceUrls urls) {
        this.restClient = restClient;
        this.urls = urls;
    }

    @Override
    public CardVerification verify(RiderId rider, CardToken card) {
        var response = restClient.post()
                                 .uri(urls.url("/payments/payment-methods"))
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .body(new VerifyPaymentMethodRequest(rider.value(), card.value()))
                                 .retrieve()
                                 .body(VerifyPaymentMethodResponse.class);
        if (response == null || !response.verified()) {
            return CardVerification.declined();
        }
        return CardVerification.verifiedAs(new PaymentMethodId(response.paymentMethodId()));
    }
}
