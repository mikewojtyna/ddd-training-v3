package pl.wojtyna.dddtrainingv3.velocity.common.contract;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import pl.wojtyna.dddtrainingv3.velocity.common.MutableClock;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.VelocityServiceUrls;
import pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.primary.FailedChargeRetryJob;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.awaitility.Awaitility.await;

/// Builds provider states for the Pact verifications the way VeloCity's people would: over the contexts' public REST
/// resources (rider app, fleet manager, technician app, support desk), never through a context's classes. Only time
/// (`MutableClock`) and Settlement's charge-retry job, which the journeys drive too, are touched directly.
///
/// Every state uses fresh identifiers, because all provider tests share one Spring context.
public class ProviderStates {

    /// Rentals' reference station `Central` (52.2300/21.0110).
    public static final String CENTRAL = "Central";
    /// About ten metres north of `Central`: "standing near" a bike parked there (DE-12: within 50 m).
    public static final double NEAR_CENTRAL_LATITUDE = 52.23009;
    public static final double NEAR_CENTRAL_LONGITUDE = 21.0110;
    /// Inside the Old Town Square drop zone (centre 52.2297/21.0122, radius 150 m).
    public static final double OLD_TOWN_SQUARE_LATITUDE = 52.2298;
    public static final double OLD_TOWN_SQUARE_LONGITUDE = 21.0123;

    public record Reply(int status, Map<String, Object> body) {

        public String get(String field) {
            var value = body == null ? null : body.get(field);
            return value == null ? null : value.toString();
        }
    }

    private final RestClient restClient;
    private final VelocityServiceUrls urls;
    private final MutableClock clock;
    private final FailedChargeRetryJob failedChargeRetryJob;

    public ProviderStates(RestClient restClient, VelocityServiceUrls urls, MutableClock clock, FailedChargeRetryJob failedChargeRetryJob) {
        this.restClient = restClient;
        this.urls = urls;
        this.clock = clock;
        this.failedChargeRetryJob = failedChargeRetryJob;
    }

    public static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public MutableClock clock() {
        return clock;
    }

    // --- Accounts ---

    /// A person who registered in the rider app and stopped there; returns the rider id.
    public String registeredRider() {
        return expect(post("/accounts/riders", Map.of("email", unique("rider") + "@example.com")), 201).get("riderId");
    }

    /// A person who went through the whole onboarding with the given card; returns the rider id.
    public String onboardedRider(String cardToken) {
        var riderId = registeredRider();
        expect(post("/accounts/riders/" + riderId + "/email-confirmation", null), 200);
        expect(post("/accounts/riders/" + riderId + "/phone-verification", Map.of("phoneNumber", "+48 600 100 200")), 200);
        expect(post("/accounts/riders/" + riderId + "/personal-data",
                    Map.of("fullName", "Anna Nowak", "dateOfBirth", "1995-04-12", "countryOfResidency", "Poland")), 200);
        addCard(riderId, cardToken);
        expect(post("/accounts/riders/" + riderId + "/agreement-signature", null), 200);
        return riderId;
    }

    public String onboardedRider() {
        return onboardedRider(unique("tok_visa"));
    }

    public void addCard(String riderId, String cardToken) {
        expect(post("/accounts/riders/" + riderId + "/payment-methods", Map.of("cardToken", cardToken)), 200);
    }

    public void blockForOutstandingDebt(String riderId) {
        expect(post("/accounts/riders/" + riderId + "/blocks", Map.of("reason", "OUTSTANDING_DEBT")), 200);
    }

    // --- Payments ---

    /// The card processor reports a card as no longer usable.
    public void invalidateCard(String cardToken, String reason) {
        expect(post("/payments/processor-notifications/card-invalidations", Map.of("cardToken", cardToken, "reason", reason)), 200);
    }

    /// Payments verifies a card for a rider it may not have heard of yet (Payments keeps its own riders' cards).
    public void verifyCardInPayments(String riderId, String cardToken) {
        expect(post("/payments/payment-methods", Map.of("riderId", riderId, "cardToken", cardToken)), 200);
    }

    // --- Rentals ---

    /// The fleet manager adds a new bike at a station; returns its id.
    public String bikeAt(String stationId) {
        var bikeId = unique("bike");
        expect(post("/rentals/bikes", Map.of("bikeId", bikeId, "stationId", stationId)), 201);
        return bikeId;
    }

    public void withdraw(String bikeId) {
        expect(post("/rentals/bikes/" + bikeId + "/withdrawal", Map.of()), 200);
    }

    public void signalFault(String bikeId, String faultCode) {
        expect(post("/rentals/bikes/" + bikeId + "/fault-signals", Map.of("faultCode", faultCode)), 202);
    }

    public void reserveForTechnician(String bikeId, String technicianId) {
        expect(post("/rentals/bikes/" + bikeId + "/reservations", Map.of("holderType", "TECHNICIAN", "holderId", technicianId)), 201);
    }

    public Reply unlockForTechnician(String bikeId, String technicianId) {
        return expect(post("/rentals/bikes/" + bikeId + "/unlock", Map.of("holderType", "TECHNICIAN", "holderId", technicianId)), 200);
    }

    /// A withdrawn bike whose reservation `technicianId` holds.
    public String withdrawnBikeReservedBy(String technicianId) {
        var bikeId = bikeAt(CENTRAL);
        withdraw(bikeId);
        reserveForTechnician(bikeId, technicianId);
        return bikeId;
    }

    /// A rider rides a bike from `Central` for `minutes` (reserve, unlock, ride); returns the session id.
    public String rideFromCentral(String riderId, String bikeId, int minutes) {
        expect(post("/rentals/bikes/" + bikeId + "/reservations",
                    Map.of("holderType", "RIDER", "holderId", riderId,
                           "latitude", NEAR_CENTRAL_LATITUDE, "longitude", NEAR_CENTRAL_LONGITUDE)), 201);
        var sessionId = expect(post("/rentals/bikes/" + bikeId + "/unlock", Map.of("holderType", "RIDER", "holderId", riderId)), 200)
            .get("sessionId");
        clock.advanceBy(Duration.ofMinutes(minutes));
        return sessionId;
    }

    public void returnAtStation(String bikeId, String riderId, String stationId) {
        expect(post("/rentals/bikes/" + bikeId + "/station-return", Map.of("riderId", riderId, "stationId", stationId)), 200);
    }

    public void endRideHere(String bikeId, String riderId, double latitude, double longitude) {
        expect(post("/rentals/bikes/" + bikeId + "/free-floating-return",
                    Map.of("riderId", riderId, "latitude", latitude, "longitude", longitude)), 200);
    }

    // --- Settlement ---

    /// A rider whose card has no funds rode 30 minutes (PLN 3.00); the charge failed right away and on both retries,
    /// so the fee became outstanding debt (DE-35). Returns `riderId` and `sessionId`.
    public Map<String, Object> riderWithUnpaidRide() {
        clock.setTo("2027-06-01T08:00:00Z");
        var riderId = onboardedRider(unique("tok_nofunds"));
        var bikeId = bikeAt(CENTRAL);
        var sessionId = rideFromCentral(riderId, bikeId, 30);
        returnAtStation(bikeId, riderId, CENTRAL);
        awaitRide(sessionId, "PENDING", 1);
        clock.advanceBy(Duration.ofMinutes(5));
        failedChargeRetryJob.retryFailedCharges();
        awaitRide(sessionId, "PENDING", 2);
        clock.advanceBy(Duration.ofMinutes(5));
        failedChargeRetryJob.retryFailedCharges();
        awaitRide(sessionId, "UNPAID", 3);
        var state = new HashMap<String, Object>();
        state.put("riderId", riderId);
        state.put("sessionId", sessionId);
        return state;
    }

    public void payOffDebt(String riderId) {
        expect(post("/settlement/riders/" + riderId + "/debt-payment", null), 200);
    }

    private void awaitRide(String sessionId, String status, int failedAttempts) {
        await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofMillis(50)).until(() -> {
            var ride = get("/settlement/rides/" + sessionId);
            return ride.status() == 200 && status.equals(ride.get("status"))
                   && String.valueOf(failedAttempts).equals(ride.get("failedAttempts"));
        });
    }

    // --- Customer Support ---

    /// A rider reports an incident in the app; returns the incident id.
    public String incident(String category, String bikeId) {
        var body = new HashMap<String, Object>(Map.of("riderId", unique("rider"), "category", category,
                                                      "description", "The rear brake doesn't work"));
        if (bikeId != null) {
            body.put("bikeId", bikeId);
        }
        return expect(post("/customer-support/incidents", body), 201).get("incidentId");
    }

    // --- HTTP ---

    public static Reply expect(Reply reply, int status) {
        if (reply.status() != status) {
            throw new IllegalStateException("Provider state set-up expected HTTP " + status + " but got " + reply);
        }
        return reply;
    }

    public Reply post(String path, Object body) {
        var request = restClient.post().uri(urls.url(path)).contentType(MediaType.APPLICATION_JSON);
        if (body != null) {
            request = request.body(body);
        }
        return request.exchange((req, response) -> read(response));
    }

    public Reply get(String path) {
        return restClient.get().uri(urls.url(path)).exchange((req, response) -> read(response));
    }

    @SuppressWarnings("unchecked")
    private static Reply read(RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse response) throws IOException {
        var status = response.getStatusCode().value();
        Map<String, Object> body;
        try {
            body = response.bodyTo(Map.class);
        } catch (RuntimeException noJsonObject) {
            body = null;
        }
        return new Reply(status, body);
    }
}
