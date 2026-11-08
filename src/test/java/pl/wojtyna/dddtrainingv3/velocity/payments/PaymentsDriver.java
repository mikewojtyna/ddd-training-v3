package pl.wojtyna.dddtrainingv3.velocity.payments;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.VelocityServiceUrls;
import pl.wojtyna.dddtrainingv3.velocity.payments.api.events.PaymentMethodInvalidated;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/// Drives Payments over its REST resources, as Accounts, Settlement and the processor do, and records the
/// `PaymentMethodInvalidated` integration events Payments publishes, as a downstream listener would see them.
class PaymentsDriver implements ApplicationListener<ApplicationEvent> {

    record Verification(String paymentMethodId, boolean verified, String declineReason) {
    }

    record Charge(String chargeId, String status, String failureReason) {
    }

    record PaymentMethod(String paymentMethodId, String cardToken, String status) {
    }

    record PaymentMethods(String riderId, String currentPaymentMethodId, List<PaymentMethod> paymentMethods) {
    }

    private final RestClient restClient;
    private final VelocityServiceUrls urls;
    private final ApplicationEventMulticaster multicaster;
    private final List<PaymentMethodInvalidated> invalidations = new CopyOnWriteArrayList<>();

    PaymentsDriver(RestClient restClient, VelocityServiceUrls urls, ApplicationEventMulticaster multicaster) {
        this.restClient = restClient;
        this.urls = urls;
        this.multicaster = multicaster;
        multicaster.addApplicationListener(this);
    }

    void stop() {
        multicaster.removeApplicationListener(this);
    }

    Verification verify(String riderId, String cardToken) {
        return post("/payments/payment-methods", Map.of("riderId", riderId, "cardToken", cardToken), Verification.class);
    }

    Charge charge(String riderId, String amount, String reference) {
        return post("/payments/charges", Map.of("riderId", riderId, "amount", amount, "reference", reference), Charge.class);
    }

    void reportCardInvalidated(String cardToken, String reason) {
        restClient.post()
                  .uri(urls.url("/payments/processor-notifications/card-invalidations"))
                  .contentType(MediaType.APPLICATION_JSON)
                  .body(Map.of("cardToken", cardToken, "reason", reason))
                  .retrieve()
                  .toBodilessEntity();
    }

    PaymentMethods paymentMethodsOf(String riderId) {
        return restClient.get().uri(urls.url("/payments/riders/" + riderId + "/payment-methods")).retrieve().body(PaymentMethods.class);
    }

    List<PaymentMethodInvalidated> invalidationsOf(String riderId) {
        return invalidations.stream().filter(event -> event.riderId().equals(riderId)).toList();
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof PayloadApplicationEvent<?> payloadEvent && payloadEvent.getPayload() instanceof PaymentMethodInvalidated invalidated) {
            invalidations.add(invalidated);
        }
    }

    private <T> T post(String path, Object body, Class<T> responseType) {
        return restClient.post()
                         .uri(urls.url(path))
                         .contentType(MediaType.APPLICATION_JSON)
                         .body(body)
                         .retrieve()
                         .body(responseType);
    }
}
