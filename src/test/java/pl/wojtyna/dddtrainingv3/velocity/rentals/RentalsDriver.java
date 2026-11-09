package pl.wojtyna.dddtrainingv3.velocity.rentals;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import pl.wojtyna.dddtrainingv3.common.integration.IntegrationEvent;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.VelocityServiceUrls;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/// Drives Rentals over its REST resources, as the rider app, the fleet manager, the bikes' telemetry and Fleet
/// Maintenance do; onboards riders over Accounts' REST resources; and records the integration events Rentals publishes
/// (`rentals.api.events`), as Settlement and Fleet Maintenance would receive them.
public class RentalsDriver implements ApplicationListener<ApplicationEvent> {

    public static final double EARTH_RADIUS_METRES = 6_371_008.8;

    public record Place(String stationId, double latitude, double longitude) {

        /// A point `metres` due north of this place.
        public Place northBy(double metres) {
            return new Place(null, latitude + Math.toDegrees(metres / EARTH_RADIUS_METRES), longitude);
        }
    }

    public static final Place CENTRAL = new Place("Central", 52.2300, 21.0110);
    public static final Place OLD_TOWN = new Place("Old Town", 52.2490, 21.0122);
    public static final Place RIVERSIDE = new Place("Riverside", 52.2400, 21.0300);

    public record Reply<T>(int status, T body, String error) {
    }

    public record Reservation(String bikeId, String holderType, String holderId, Instant reservedAt, Instant expiresAt) {
    }

    public record Unlock(String bikeId, String outcome, String sessionId) {
    }

    public record SessionEnd(String sessionId, String bikeId, String endReason, Instant endedAt) {
    }

    public record Bike(String bikeId, String availability, String hold, String holderType, String holderId,
                       BigDecimal latitude, BigDecimal longitude, String stationId) {
    }

    public record ActiveRental(String riderId, String bikeId, String kind, String sessionId, Instant since, Instant expiresAt) {
    }

    private record ErrorBody(String error, String message) {
    }

    private record Registered(String riderId) {
    }

    private final RestClient restClient;
    private final VelocityServiceUrls urls;
    private final ApplicationEventMulticaster multicaster;
    private final List<IntegrationEvent> published = new CopyOnWriteArrayList<>();

    public RentalsDriver(RestClient restClient, VelocityServiceUrls urls, ApplicationEventMulticaster multicaster) {
        this.restClient = restClient;
        this.urls = urls;
        this.multicaster = multicaster;
        multicaster.addApplicationListener(this);
    }

    public void stop() {
        multicaster.removeApplicationListener(this);
    }

    // --- Rentals ---

    public Reply<Void> addBike(String bikeId, String stationId) {
        return post("/rentals/bikes", Map.of("bikeId", bikeId, "stationId", stationId), Void.class);
    }

    public Reply<Bike> bike(String bikeId) {
        return get("/rentals/bikes/" + bikeId, Bike.class);
    }

    public Reply<Reservation> reserve(String bikeId, String riderId, Place standingAt) {
        return post("/rentals/bikes/" + bikeId + "/reservations",
                    Map.of("holderType", "RIDER", "holderId", riderId, "latitude", standingAt.latitude(), "longitude", standingAt.longitude()),
                    Reservation.class);
    }

    public Reply<Reservation> reserveForService(String bikeId, String technicianId) {
        return post("/rentals/bikes/" + bikeId + "/reservations", Map.of("holderType", "TECHNICIAN", "holderId", technicianId),
                    Reservation.class);
    }

    public Reply<Void> cancelReservation(String bikeId, String riderId) {
        return post("/rentals/bikes/" + bikeId + "/reservation-cancellation", Map.of("riderId", riderId), Void.class);
    }

    public Reply<Unlock> unlock(String bikeId, String riderId) {
        return post("/rentals/bikes/" + bikeId + "/unlock", Map.of("holderType", "RIDER", "holderId", riderId), Unlock.class);
    }

    public Reply<Unlock> unlockForService(String bikeId, String technicianId) {
        return post("/rentals/bikes/" + bikeId + "/unlock", Map.of("holderType", "TECHNICIAN", "holderId", technicianId), Unlock.class);
    }

    public Reply<SessionEnd> returnAtStation(String bikeId, String riderId, String stationId) {
        return post("/rentals/bikes/" + bikeId + "/station-return", Map.of("riderId", riderId, "stationId", stationId), SessionEnd.class);
    }

    public Reply<SessionEnd> returnAtStationReporting(String bikeId, String riderId, String stationId, String faultDescription) {
        return post("/rentals/bikes/" + bikeId + "/station-return",
                    Map.of("riderId", riderId, "stationId", stationId, "faultDescription", faultDescription), SessionEnd.class);
    }

    public Reply<SessionEnd> endRideHere(String bikeId, String riderId, double latitude, double longitude) {
        return post("/rentals/bikes/" + bikeId + "/free-floating-return",
                    Map.of("riderId", riderId, "latitude", latitude, "longitude", longitude), SessionEnd.class);
    }

    public Reply<Void> signalFault(String bikeId, String faultCode) {
        return post("/rentals/bikes/" + bikeId + "/fault-signals", Map.of("faultCode", faultCode), Void.class);
    }

    public Reply<Void> withdraw(String bikeId) {
        return post("/rentals/bikes/" + bikeId + "/withdrawal", Map.of(), Void.class);
    }

    public Reply<Void> makeAvailableAtStation(String bikeId, String stationId) {
        return post("/rentals/bikes/" + bikeId + "/availability", Map.of("stationId", stationId), Void.class);
    }

    public Reply<Void> makeAvailableAt(String bikeId, double latitude, double longitude) {
        return post("/rentals/bikes/" + bikeId + "/availability", Map.of("latitude", latitude, "longitude", longitude), Void.class);
    }

    public Reply<ActiveRental> activeRentalOf(String riderId) {
        return get("/rentals/riders/" + riderId + "/active-rental", ActiveRental.class);
    }

    // --- arrangement over REST ---

    /// A new bike at a station; returns its id.
    public String bikeAt(String bikeId, Place station) {
        var reply = addBike(bikeId, station.stationId());
        if (reply.status() != 201) {
            throw new IllegalStateException("Couldn't add " + bikeId + ": " + reply);
        }
        return bikeId;
    }

    /// A rider holding a reservation of `bikeId` (added at `station`), reserved at the clock's now.
    public void reserved(String bikeId, Place station, String riderId) {
        bikeAt(bikeId, station);
        expect(reserve(bikeId, riderId, station.northBy(10)), 201);
    }

    /// A rider riding `bikeId` (added at `station`), unlocked at the clock's now.
    public String riding(String bikeId, Place station, String riderId) {
        reserved(bikeId, station, riderId);
        return expect(unlock(bikeId, riderId), 200).body().sessionId();
    }

    private static <T> Reply<T> expect(Reply<T> reply, int status) {
        if (reply.status() != status) {
            throw new IllegalStateException("Expected " + status + " but got " + reply);
        }
        return reply;
    }

    // --- Accounts (onboarding over REST) ---

    public static String cardTokenOf(String riderSuffix) {
        return "tok_visa_4242_" + riderSuffix;
    }

    /// The payment processor reports the card of the rider onboarded with `riderSuffix` lost (Payments → Accounts).
    public int reportCardLost(String riderSuffix) {
        return post("/payments/processor-notifications/card-invalidations",
                    Map.of("cardToken", cardTokenOf(riderSuffix), "reason", "REPORTED_LOST"), Void.class).status();
    }

    public boolean isEligibleToRent(String riderId) {
        record Eligibility(String riderId, boolean eligible, String reason) {
        }
        return get("/accounts/riders/" + riderId + "/rental-eligibility", Eligibility.class).body().eligible();
    }

    public String activatedRider(String suffix) {
        var riderId = personalDataProvided(suffix);
        post("/accounts/riders/" + riderId + "/payment-methods", Map.of("cardToken", cardTokenOf(suffix)), Void.class);
        post("/accounts/riders/" + riderId + "/agreement-signature", null, Void.class);
        return riderId;
    }

    public String riderWithoutPaymentMethod(String suffix) {
        return personalDataProvided(suffix);
    }

    public String riderBlockedForDebt(String suffix) {
        var riderId = activatedRider(suffix);
        post("/accounts/riders/" + riderId + "/blocks", Map.of("reason", "OUTSTANDING_DEBT"), Void.class);
        return riderId;
    }

    private String personalDataProvided(String suffix) {
        var riderId = post("/accounts/riders", Map.of("email", "rider-" + suffix + "@example.com"), Registered.class).body().riderId();
        post("/accounts/riders/" + riderId + "/email-confirmation", null, Void.class);
        post("/accounts/riders/" + riderId + "/phone-verification", Map.of("phoneNumber", "+48 600 100 200"), Void.class);
        post("/accounts/riders/" + riderId + "/personal-data",
             Map.of("fullName", "Anna Nowak", "dateOfBirth", "1995-04-12", "countryOfResidency", "Poland"), Void.class);
        return riderId;
    }

    // --- recorded integration events ---

    public <E extends IntegrationEvent> List<E> published(Class<E> type, Predicate<E> matching) {
        return published.stream().filter(type::isInstance).map(type::cast).filter(matching).toList();
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof PayloadApplicationEvent<?> payloadEvent && payloadEvent.getPayload() instanceof IntegrationEvent integrationEvent
            && integrationEvent.getClass().getPackageName().startsWith("pl.wojtyna.dddtrainingv3.velocity.rentals.api.events")) {
            published.add(integrationEvent);
        }
    }

    // --- HTTP ---

    private <T> Reply<T> post(String path, Object body, Class<T> type) {
        var request = restClient.post().uri(urls.url(path)).contentType(MediaType.APPLICATION_JSON);
        if (body != null) {
            request = request.body(body);
        }
        return request.exchange((req, response) -> read(response.getStatusCode().value(), response, type));
    }

    private <T> Reply<T> get(String path, Class<T> type) {
        return restClient.get().uri(urls.url(path)).exchange((req, response) -> read(response.getStatusCode().value(), response, type));
    }

    private static <T> Reply<T> read(int status, RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse response, Class<T> type)
        throws java.io.IOException {
        if (status >= 200 && status < 300) {
            T body = type == Void.class ? null : response.bodyTo(type);
            return new Reply<>(status, body, null);
        }
        ErrorBody error;
        try {
            error = response.bodyTo(ErrorBody.class);
        } catch (RuntimeException noErrorBody) {
            error = null;
        }
        return new Reply<>(status, null, error == null ? null : error.error());
    }
}
