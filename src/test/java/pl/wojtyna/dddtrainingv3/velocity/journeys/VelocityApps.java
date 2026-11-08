package pl.wojtyna.dddtrainingv3.velocity.journeys;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.VelocityServiceUrls;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/// The apps of VeloCity's people (rider app, fleet manager's console, technician app, support agent's desk), talking
/// to the contexts only over their public REST resources, the way the real apps would.
///
/// Every answer is a `Reply` with the HTTP status and the JSON body as a map, so journeys read like the apps' view of
/// the system and never touch a context's classes.
class VelocityApps {

    static final Place CENTRAL = new Place("Central", 52.2300, 21.0110);
    static final Place RIVERSIDE = new Place("Riverside", 52.2400, 21.0300);
    /// Inside the Old Town Square drop zone (centre 52.2297/21.0122, radius 150 m).
    static final Place OLD_TOWN_SQUARE_DROP_ZONE = new Place(null, 52.2298, 21.0123);

    record Place(String stationId, double latitude, double longitude) {

        /// A point roughly ten metres north of this place: "standing near" a bike parked here (DE-12: within 50 m).
        Place tenMetresNorth() {
            return new Place(null, latitude + 0.00009, longitude);
        }
    }

    record Reply(int status, Map<String, Object> body) {

        String get(String field) {
            var value = body == null ? null : body.get(field);
            return value == null ? null : value.toString();
        }

        @SuppressWarnings("unchecked")
        List<Object> list(String field) {
            return (List<Object>) body.get(field);
        }
    }

    private final RestClient restClient;
    private final VelocityServiceUrls urls;

    VelocityApps(RestClient restClient, VelocityServiceUrls urls) {
        this.restClient = restClient;
        this.urls = urls;
    }

    static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    // --- rider app: onboarding (Accounts) ---

    Reply register(String email) {
        return post("/accounts/riders", Map.of("email", email));
    }

    Reply confirmEmail(String riderId) {
        return post("/accounts/riders/" + riderId + "/email-confirmation", null);
    }

    Reply verifyPhone(String riderId, String phoneNumber) {
        return post("/accounts/riders/" + riderId + "/phone-verification", Map.of("phoneNumber", phoneNumber));
    }

    Reply providePersonalData(String riderId, String fullName, String dateOfBirth, String countryOfResidency) {
        return post("/accounts/riders/" + riderId + "/personal-data",
                    Map.of("fullName", fullName, "dateOfBirth", dateOfBirth, "countryOfResidency", countryOfResidency));
    }

    Reply addCard(String riderId, String cardToken) {
        return post("/accounts/riders/" + riderId + "/payment-methods", Map.of("cardToken", cardToken));
    }

    Reply signUserAgreement(String riderId) {
        return post("/accounts/riders/" + riderId + "/agreement-signature", null);
    }

    Reply account(String riderId) {
        return get("/accounts/riders/" + riderId);
    }

    Reply rentalEligibility(String riderId) {
        return get("/accounts/riders/" + riderId + "/rental-eligibility");
    }

    /// A person who went through the whole onboarding in the app with the given card; returns the rider id.
    String onboardedRider(String cardToken) {
        var riderId = expect(register(unique("rider") + "@example.com"), 201).get("riderId");
        expect(confirmEmail(riderId), 200);
        expect(verifyPhone(riderId, "+48 600 100 200"), 200);
        expect(providePersonalData(riderId, "Anna Nowak", "1995-04-12", "Poland"), 200);
        expect(addCard(riderId, cardToken), 200);
        expect(signUserAgreement(riderId), 200);
        return riderId;
    }

    // --- rider app: bikes (Rentals) ---

    Reply reserve(String bikeId, String riderId, Place standingAt) {
        return post("/rentals/bikes/" + bikeId + "/reservations",
                    Map.of("holderType", "RIDER", "holderId", riderId,
                           "latitude", standingAt.latitude(), "longitude", standingAt.longitude()));
    }

    Reply unlock(String bikeId, String riderId) {
        return post("/rentals/bikes/" + bikeId + "/unlock", Map.of("holderType", "RIDER", "holderId", riderId));
    }

    Reply returnAtStation(String bikeId, String riderId, String stationId) {
        return post("/rentals/bikes/" + bikeId + "/station-return", Map.of("riderId", riderId, "stationId", stationId));
    }

    Reply endRideHere(String bikeId, String riderId, Place place) {
        return post("/rentals/bikes/" + bikeId + "/free-floating-return",
                    Map.of("riderId", riderId, "latitude", place.latitude(), "longitude", place.longitude()));
    }

    Reply bike(String bikeId) {
        return get("/rentals/bikes/" + bikeId);
    }

    // --- rider app: money (Settlement) and support (Customer Support) ---

    Reply payOffDebt(String riderId) {
        return post("/settlement/riders/" + riderId + "/debt-payment", null);
    }

    Reply balance(String riderId) {
        return get("/settlement/riders/" + riderId + "/balance");
    }

    Reply rideSettlement(String sessionId) {
        return get("/settlement/rides/" + sessionId);
    }

    Reply reportIncident(String riderId, String category, String description, String bikeId) {
        var body = new HashMap<String, Object>(Map.of("riderId", riderId, "category", category, "description", description));
        if (bikeId != null) {
            body.put("bikeId", bikeId);
        }
        return post("/customer-support/incidents", body);
    }

    Reply incident(String incidentId) {
        return get("/customer-support/incidents/" + incidentId);
    }

    // --- support agent's desk (Customer Support), collections desk (Debt Recovery) ---

    Reply pickUpIncident(String incidentId, String agentId) {
        return post("/customer-support/incidents/" + incidentId + "/assignment", Map.of("agentId", agentId));
    }

    Reply acceptIncident(String incidentId, String agentId) {
        return post("/customer-support/incidents/" + incidentId + "/acceptance", Map.of("agentId", agentId));
    }

    Reply recovery(String riderId) {
        return get("/debt-recovery/riders/" + riderId + "/recovery");
    }

    // --- fleet manager's console (Rentals) ---

    Reply addBikeToFleet(String bikeId, String stationId) {
        return post("/rentals/bikes", Map.of("bikeId", bikeId, "stationId", stationId));
    }

    // --- technician app (Fleet Maintenance) ---

    Reply brokenBike(String bikeId) {
        return get("/fleet-maintenance/bikes/" + bikeId);
    }

    Reply reserveBrokenBike(String bikeId, String technicianId) {
        return post("/fleet-maintenance/bikes/" + bikeId + "/reservation", Map.of("technicianId", technicianId));
    }

    Reply unlockBrokenBike(String bikeId, String technicianId) {
        return post("/fleet-maintenance/bikes/" + bikeId + "/unlock", Map.of("technicianId", technicianId));
    }

    Reply returnToCirculationAtStation(String bikeId, String technicianId, String stationId) {
        return post("/fleet-maintenance/bikes/" + bikeId + "/return-to-circulation",
                    Map.of("technicianId", technicianId, "stationId", stationId));
    }

    // --- HTTP ---

    static Reply expect(Reply reply, int status) {
        if (reply.status() != status) {
            throw new AssertionError("Expected HTTP " + status + " but got " + reply);
        }
        return reply;
    }

    private Reply post(String path, Object body) {
        var request = restClient.post().uri(urls.url(path)).contentType(MediaType.APPLICATION_JSON);
        if (body != null) {
            request = request.body(body);
        }
        return request.exchange((req, response) -> read(response));
    }

    private Reply get(String path) {
        return restClient.get().uri(urls.url(path)).exchange((req, response) -> read(response));
    }

    @SuppressWarnings("unchecked")
    private static Reply read(RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse response) throws java.io.IOException {
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
