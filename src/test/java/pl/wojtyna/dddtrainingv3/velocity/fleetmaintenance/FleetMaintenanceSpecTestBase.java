package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import pl.wojtyna.dddtrainingv3.velocity.common.MutableClock;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.VelocityServiceUrls;
import pl.wojtyna.dddtrainingv3.velocity.rentals.RentalsDriver;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases.EndOverdueRentalSessions;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases.EndOverdueRentalSessionsUseCase;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases.ReleaseExpiredReservations;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases.ReleaseExpiredReservationsUseCase;
import pl.wojtyna.dddtrainingv3.velocity.rentals.infra.adapters.secondary.bikelocksacl.SimulatedBikeLocks;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/// What every Fleet Maintenance acceptance test needs: Rentals' driver (bikes, riders, reservations, rides), the
/// technician app (Fleet Maintenance's REST resources), the rider app's incident reports (Customer Support's REST
/// resources), the clock, the simulated locks and ids of its own (the Spring context is shared and cached).
abstract class FleetMaintenanceSpecTestBase {

    static final Duration HAND_OFF = Duration.ofSeconds(10);

    record Reply<T>(int status, T body, String error) {
    }

    record BrokenBike(String bikeId, String status, String faultSource, Instant reportedAt, String faultDescription,
                      String technicianId, Instant reservedUntil, List<String> incidentIds, String stationId,
                      BigDecimal latitude, BigDecimal longitude) {
    }

    record Reservation(String bikeId, String technicianId, Instant reservedUntil) {
    }

    record Unlock(String bikeId, String outcome) {
    }

    record IncidentReceipt(String incidentId, String type) {
    }

    record Incident(String incidentId, String riderId, String type, String status, String assignedAgentId, String team,
                    String resolution) {
    }

    private record ErrorBody(String error, String message) {
    }

    @Autowired
    RestClient restClient;
    @Autowired
    VelocityServiceUrls urls;
    @Autowired
    ApplicationEventMulticaster multicaster;
    @Autowired
    MutableClock clock;
    @Autowired
    SimulatedBikeLocks locks;
    @Autowired
    ReleaseExpiredReservationsUseCase releaseExpiredReservations;
    @Autowired
    EndOverdueRentalSessionsUseCase endOverdueRentalSessions;

    RentalsDriver rentals;
    final String suffix = UUID.randomUUID().toString().substring(0, 8);

    @BeforeEach
    void startDriver() {
        rentals = new RentalsDriver(restClient, urls, multicaster);
    }

    @AfterEach
    void stopDriver() {
        rentals.stop();
    }

    String bike(String name) {
        return name + "-" + suffix;
    }

    String technician(String name) {
        return name + "-" + suffix;
    }

    String rider(String name) {
        return rentals.activatedRider(name + "-" + suffix);
    }

    void at(String isoInstant) {
        clock.setTo(isoInstant);
    }

    // --- Rentals' clock-driven work ---

    void reservationsAreChecked() {
        releaseExpiredReservations.handle(new ReleaseExpiredReservations(clock.instant()));
    }

    void overdueSessionsAreEnded() {
        endOverdueRentalSessions.handle(new EndOverdueRentalSessions(clock.instant()));
    }

    // --- technician app (Fleet Maintenance) ---

    Reply<Void> reportFault(String bikeId, String technicianId, String description) {
        return post("/fleet-maintenance/bikes/" + bikeId + "/fault-reports",
                    Map.of("technicianId", technicianId, "description", description), Void.class);
    }

    Reply<Reservation> reserveBrokenBike(String bikeId, String technicianId) {
        return post("/fleet-maintenance/bikes/" + bikeId + "/reservation", Map.of("technicianId", technicianId), Reservation.class);
    }

    Reply<Unlock> unlockBrokenBike(String bikeId, String technicianId) {
        return post("/fleet-maintenance/bikes/" + bikeId + "/unlock", Map.of("technicianId", technicianId), Unlock.class);
    }

    Reply<Void> returnToCirculationAtStation(String bikeId, String technicianId, String stationId) {
        return post("/fleet-maintenance/bikes/" + bikeId + "/return-to-circulation",
                    Map.of("technicianId", technicianId, "stationId", stationId), Void.class);
    }

    Reply<Void> returnToCirculationAt(String bikeId, String technicianId, double latitude, double longitude) {
        return post("/fleet-maintenance/bikes/" + bikeId + "/return-to-circulation",
                    Map.of("technicianId", technicianId, "latitude", latitude, "longitude", longitude), Void.class);
    }

    Reply<BrokenBike> brokenBike(String bikeId) {
        return get("/fleet-maintenance/bikes/" + bikeId, BrokenBike.class);
    }

    List<BrokenBike> brokenBikes() {
        return List.of(get("/fleet-maintenance/broken-bikes", BrokenBike[].class).body());
    }

    // --- rider app and support agents (Customer Support) ---

    String reportFleetIssue(String riderId, String bikeId, String description) {
        var body = new HashMap<String, Object>();
        body.put("riderId", riderId);
        body.put("category", "BROKEN_BIKE");
        body.put("description", description);
        body.put("bikeId", bikeId);
        var reply = post("/customer-support/incidents", body, IncidentReceipt.class);
        assertThat(reply.status()).isEqualTo(201);
        assertThat(reply.body().type()).isEqualTo("FLEET_ISSUE");
        return reply.body().incidentId();
    }

    Reply<Void> supportAgent(String action, String incidentId, Map<String, Object> body) {
        return post("/customer-support/incidents/" + incidentId + "/" + action, body, Void.class);
    }

    Incident incident(String incidentId) {
        return get("/customer-support/incidents/" + incidentId, Incident.class).body();
    }

    // --- arrangement ---

    /// The bike is broken in Fleet Maintenance and withdrawn in Rentals, through a technician's fault report.
    void brokenAt(String bikeId, RentalsDriver.Place station, String technicianId) {
        rentals.bikeAt(bikeId, station);
        assertThat(reportFault(bikeId, technicianId, "found during a routine check").status()).isEqualTo(200);
        awaitWithdrawn(bikeId);
    }

    /// The bike is under maintenance by `technicianId`, unlocked at the clock's now.
    void underMaintenance(String bikeId, RentalsDriver.Place station, String technicianId) {
        brokenAt(bikeId, station, technicianId);
        assertThat(reserveBrokenBike(bikeId, technicianId).status()).isEqualTo(200);
        assertThat(unlockBrokenBike(bikeId, technicianId).body().outcome()).isEqualTo("UNLOCKED");
        await().atMost(HAND_OFF).untilAsserted(() ->
            assertThat(brokenBike(bikeId).body().status()).isEqualTo("UNDER_MAINTENANCE"));
    }

    void awaitWithdrawn(String bikeId) {
        await().atMost(HAND_OFF).untilAsserted(() ->
            assertThat(rentals.bike(bikeId).body().availability()).isEqualTo("WITHDRAWN"));
    }

    void awaitBroken(String bikeId) {
        await().atMost(HAND_OFF).untilAsserted(() ->
            assertThat(brokenBike(bikeId).status()).isEqualTo(200));
    }

    // --- HTTP ---

    <T> Reply<T> post(String path, Object body, Class<T> type) {
        return restClient.post().uri(urls.url(path)).contentType(MediaType.APPLICATION_JSON).body(body)
                         .exchange((request, response) -> read(response.getStatusCode().value(), response, type));
    }

    <T> Reply<T> get(String path, Class<T> type) {
        return restClient.get().uri(urls.url(path))
                         .exchange((request, response) -> read(response.getStatusCode().value(), response, type));
    }

    private static <T> Reply<T> read(int status, RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse response, Class<T> type)
        throws java.io.IOException {
        if (status >= 200 && status < 300) {
            return new Reply<>(status, type == Void.class ? null : response.bodyTo(type), null);
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
