package pl.wojtyna.dddtrainingv3.velocity.accounts;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.AccountEvent;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.VelocityServiceUrls;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/// Drives Accounts over its REST resources, as the rider app, Rentals and Settlement do, and records Accounts'
/// domain events (published on the server's threads, so `@RecordApplicationEvents` wouldn't see them).
class AccountsDriver implements ApplicationListener<ApplicationEvent> {

    record Registration(int status, String riderId) {
    }

    record CardAddition(int status, Boolean verified, String paymentMethodId) {
    }

    record Status(String riderId, String onboardingStep, boolean activated, List<String> blocks) {
    }

    record Eligibility(String riderId, boolean eligible, String reason) {
    }

    private record RegisteredBody(String riderId) {
    }

    private record CardAdditionBody(Boolean verified, String paymentMethodId) {
    }

    private final RestClient restClient;
    private final VelocityServiceUrls urls;
    private final ApplicationEventMulticaster multicaster;
    private final List<AccountEvent> events = new CopyOnWriteArrayList<>();

    AccountsDriver(RestClient restClient, VelocityServiceUrls urls, ApplicationEventMulticaster multicaster) {
        this.restClient = restClient;
        this.urls = urls;
        this.multicaster = multicaster;
        multicaster.addApplicationListener(this);
    }

    void stop() {
        multicaster.removeApplicationListener(this);
    }

    // --- commands and queries, one per REST resource ---

    Registration register(String email) {
        return restClient.post().uri(urls.url("/accounts/riders")).contentType(MediaType.APPLICATION_JSON)
                         .body(Map.of("email", email))
                         .exchange((request, response) -> {
                             var status = response.getStatusCode().value();
                             return new Registration(status, status == 201 ? response.bodyTo(RegisteredBody.class).riderId() : null);
                         });
    }

    int confirmEmail(String riderId) {
        return post("/accounts/riders/" + riderId + "/email-confirmation", null);
    }

    int verifyPhone(String riderId, String phoneNumber) {
        return post("/accounts/riders/" + riderId + "/phone-verification", Map.of("phoneNumber", phoneNumber));
    }

    int providePersonalData(String riderId, String fullName, String dateOfBirth, String countryOfResidency) {
        return post("/accounts/riders/" + riderId + "/personal-data",
                    Map.of("fullName", fullName, "dateOfBirth", dateOfBirth, "countryOfResidency", countryOfResidency));
    }

    CardAddition addPaymentMethod(String riderId, String cardToken) {
        return restClient.post().uri(urls.url("/accounts/riders/" + riderId + "/payment-methods"))
                         .contentType(MediaType.APPLICATION_JSON)
                         .body(Map.of("cardToken", cardToken))
                         .exchange((request, response) -> {
                             var status = response.getStatusCode().value();
                             if (status != 200) {
                                 return new CardAddition(status, null, null);
                             }
                             var body = response.bodyTo(CardAdditionBody.class);
                             return new CardAddition(status, body.verified(), body.paymentMethodId());
                         });
    }

    int signUserAgreement(String riderId) {
        return post("/accounts/riders/" + riderId + "/agreement-signature", null);
    }

    int blockForOutstandingDebt(String riderId) {
        return post("/accounts/riders/" + riderId + "/blocks", Map.of("reason", "OUTSTANDING_DEBT"));
    }

    int unblockOutstandingDebt(String riderId) {
        return restClient.delete().uri(urls.url("/accounts/riders/" + riderId + "/blocks/OUTSTANDING_DEBT"))
                         .exchange((request, response) -> response.getStatusCode().value());
    }

    Status statusOf(String riderId) {
        return restClient.get().uri(urls.url("/accounts/riders/" + riderId)).retrieve().body(Status.class);
    }

    int statusCodeOf(String riderId) {
        return restClient.get().uri(urls.url("/accounts/riders/" + riderId))
                         .exchange((request, response) -> response.getStatusCode().value());
    }

    Eligibility rentalEligibilityOf(String riderId) {
        return restClient.get().uri(urls.url("/accounts/riders/" + riderId + "/rental-eligibility")).retrieve().body(Eligibility.class);
    }

    // --- arrangement: a rider after each onboarding step, over REST ---

    String registered(String email) {
        return register(email).riderId();
    }

    String phoneVerified(String email) {
        var riderId = registered(email);
        confirmEmail(riderId);
        verifyPhone(riderId, "+48 600 100 200");
        return riderId;
    }

    String personalDataProvided(String email) {
        var riderId = phoneVerified(email);
        providePersonalData(riderId, "Anna Nowak", "1995-04-12", "Poland");
        return riderId;
    }

    String cardVerified(String email, String cardToken) {
        var riderId = personalDataProvided(email);
        addPaymentMethod(riderId, cardToken);
        return riderId;
    }

    String activated(String email, String cardToken) {
        var riderId = cardVerified(email, cardToken);
        signUserAgreement(riderId);
        return riderId;
    }

    // --- recorded domain events ---

    <E extends AccountEvent> List<E> eventsOf(String riderId, Class<E> type) {
        return events.stream()
                     .filter(type::isInstance)
                     .map(type::cast)
                     .filter(event -> event.riderId().value().equals(riderId))
                     .toList();
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof PayloadApplicationEvent<?> payloadEvent && payloadEvent.getPayload() instanceof AccountEvent accountEvent) {
            events.add(accountEvent);
        }
    }

    private int post(String path, Object body) {
        var request = restClient.post().uri(urls.url(path)).contentType(MediaType.APPLICATION_JSON);
        if (body != null) {
            request = request.body(body);
        }
        return request.exchange((req, response) -> response.getStatusCode().value());
    }
}
