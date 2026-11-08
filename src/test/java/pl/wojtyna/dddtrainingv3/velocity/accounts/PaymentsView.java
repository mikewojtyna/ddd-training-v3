package pl.wojtyna.dddtrainingv3.velocity.accounts;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.VelocityServiceUrls;

import java.util.List;
import java.util.Map;

/// What Accounts' acceptance tests need from Payments, over Payments' REST resources: the rider's cards (to assert
/// the Accounts → Payments hand-off) and the processor's card-invalidation notification (to start the Payments →
/// Accounts hand-off).
class PaymentsView {

    record PaymentMethod(String paymentMethodId, String cardToken, String status) {
    }

    record PaymentMethods(String riderId, String currentPaymentMethodId, List<PaymentMethod> paymentMethods) {

        String currentCardToken() {
            return paymentMethods.stream()
                                 .filter(method -> method.paymentMethodId().equals(currentPaymentMethodId))
                                 .map(PaymentMethod::cardToken)
                                 .findFirst()
                                 .orElse(null);
        }
    }

    private final RestClient restClient;
    private final VelocityServiceUrls urls;

    PaymentsView(RestClient restClient, VelocityServiceUrls urls) {
        this.restClient = restClient;
        this.urls = urls;
    }

    PaymentMethods paymentMethodsOf(String riderId) {
        return restClient.get().uri(urls.url("/payments/riders/" + riderId + "/payment-methods")).retrieve().body(PaymentMethods.class);
    }

    void processorReportsCardInvalidated(String cardToken, String reason) {
        restClient.post()
                  .uri(urls.url("/payments/processor-notifications/card-invalidations"))
                  .contentType(MediaType.APPLICATION_JSON)
                  .body(Map.of("cardToken", cardToken, "reason", reason))
                  .retrieve()
                  .toBodilessEntity();
    }
}
