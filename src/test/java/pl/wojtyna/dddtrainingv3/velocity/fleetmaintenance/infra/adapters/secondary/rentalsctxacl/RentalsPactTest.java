package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.infra.adapters.secondary.rentalsctxacl;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslRequestWithPath;
import au.com.dius.pact.consumer.dsl.PactDslRequestWithoutPath;
import au.com.dius.pact.consumer.dsl.PactDslWithState;
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
import pl.wojtyna.dddtrainingv3.velocity.common.infra.VelocityServiceUrls;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.AlreadyReserved;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.BikeId;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.NotYourReservation;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.Place;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.ReservationRefused;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.ServiceUnlocks;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.TechnicianId;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.UnknownBike;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.UnknownStation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.FLEET_MAINTENANCE;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.INSTANT;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.PACT_DIRECTORY;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.RENTALS;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.urlsOf;

/// Consumer side of `Rentals [U,OHS,PL]->[D,ACL] FleetMaintenance`: Fleet Maintenance's five Rentals adapters
/// (*Bike Availability*, *Withdraw Bike*, *Reserve Bike for service*, *Unlock Bike*, *Make Bike Available*). A broken
/// bike is a *withdrawn* bike in Rentals, a technician a holder of type `TECHNICIAN`. Rentals' rejections are
/// `{error, message}` bodies; the adapters tell `BikeAlreadyHeld`, `NotReservationHolder` and `UnknownStation` apart.
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = RENTALS, pactVersion = PactSpecVersion.V4)
@PactDirectory(PACT_DIRECTORY)
@DisplayName("Contract: Fleet Maintenance → Rentals (withdraw, reserve, unlock, make available, whereabouts)")
class RentalsPactTest {

    private static final String BIKE = "bike-17";
    private static final String UNKNOWN_BIKE = "bike-unknown-to-rentals";
    private static final String TECHNICIAN = "tech-pact-1";
    private static final String EXPIRES_AT = "2026-11-08T10:30:00Z";

    // --- Bike Availability (where is the bike?) ---

    @Pact(consumer = FLEET_MAINTENANCE)
    V4Pact whereaboutsOfBikeAtStation(PactDslWithProvider builder) {
        return builder.given("a bike parked at station Central")
                      .uponReceiving("a bike availability query for a bike at a station")
                      .pathFromProviderState("/rentals/bikes/${bikeId}", "/rentals/bikes/" + BIKE)
                      .method("GET")
                      .willRespondWith()
                      .status(200)
                      .body(new PactDslJsonBody().stringValue("stationId", "Central")
                                                 .decimalType("latitude", new BigDecimal("52.23"))
                                                 .decimalType("longitude", new BigDecimal("21.011")))
                      .toPact(V4Pact.class);
    }

    @Pact(consumer = FLEET_MAINTENANCE)
    V4Pact whereaboutsOfUnknownBike(PactDslWithProvider builder) {
        return builder.uponReceiving("a bike availability query for a bike Rentals doesn't know")
                      .path("/rentals/bikes/" + UNKNOWN_BIKE)
                      .method("GET")
                      .willRespondWith()
                      .status(404)
                      .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "whereaboutsOfBikeAtStation")
    void whereaboutsOfBikeAtStation(MockServer rentals) {
        var whereabouts = new RentalsBikeWhereaboutsAdapter(client(), urls(rentals)).of(BIKE);

        assertThat(whereabouts).hasValueSatisfying(place -> {
            assertThat(place.stationId()).isEqualTo("Central");
            assertThat(place.latitude()).isEqualByComparingTo("52.23");
            assertThat(place.longitude()).isEqualByComparingTo("21.011");
        });
    }

    @Test
    @PactTestFor(pactMethod = "whereaboutsOfUnknownBike")
    void whereaboutsOfUnknownBike(MockServer rentals) {
        assertThat(new RentalsBikeWhereaboutsAdapter(client(), urls(rentals)).of(UNKNOWN_BIKE)).isEmpty();
    }

    // --- Withdraw Bike ---

    @Pact(consumer = FLEET_MAINTENANCE)
    V4Pact withdrawBike(PactDslWithProvider builder) {
        return builder.given("a bike parked at station Central")
                      .uponReceiving("a request to withdraw a bike")
                      .pathFromProviderState("/rentals/bikes/${bikeId}/withdrawal", "/rentals/bikes/" + BIKE + "/withdrawal")
                      .method("POST")
                      .headers(json())
                      .body(new PactDslJsonBody())
                      .willRespondWith()
                      .status(200)
                      .toPact(V4Pact.class);
    }

    @Pact(consumer = FLEET_MAINTENANCE)
    V4Pact withdrawUnknownBike(PactDslWithProvider builder) {
        return builder.uponReceiving("a request to withdraw a bike Rentals doesn't know")
                      .path("/rentals/bikes/" + UNKNOWN_BIKE + "/withdrawal")
                      .method("POST")
                      .headers(json())
                      .body(new PactDslJsonBody())
                      .willRespondWith()
                      .status(404)
                      .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "withdrawBike")
    void withdrawBike(MockServer rentals) {
        assertThatCode(() -> new RentalsBikeWithdrawalsAdapter(client(), urls(rentals)).withdraw(new BikeId(BIKE)))
            .doesNotThrowAnyException();
    }

    @Test
    @PactTestFor(pactMethod = "withdrawUnknownBike")
    void withdrawUnknownBike(MockServer rentals) {
        assertThatThrownBy(() -> new RentalsBikeWithdrawalsAdapter(client(), urls(rentals)).withdraw(new BikeId(UNKNOWN_BIKE)))
            .isInstanceOf(UnknownBike.class);
    }

    // --- Reserve Bike for service ---

    @Pact(consumer = FLEET_MAINTENANCE)
    V4Pact reserveWithdrawnBike(PactDslWithProvider builder) {
        return reservation(builder.given("a withdrawn bike").uponReceiving("a technician's reservation of a withdrawn bike"), true)
            .willRespondWith()
            .status(201)
            .body(new PactDslJsonBody().stringMatcher("expiresAt", INSTANT, EXPIRES_AT))
            .toPact(V4Pact.class);
    }

    @Pact(consumer = FLEET_MAINTENANCE)
    V4Pact reserveBikeHeldByAnotherTechnician(PactDslWithProvider builder) {
        return reservation(builder.given("a withdrawn bike reserved by another technician")
                                  .uponReceiving("a technician's reservation of a bike another technician holds"), true)
            .willRespondWith()
            .status(409)
            .body(new PactDslJsonBody().stringValue("error", "BikeAlreadyHeld").stringType("message", "bike-17 is already held"))
            .toPact(V4Pact.class);
    }

    @Pact(consumer = FLEET_MAINTENANCE)
    V4Pact reserveBikeInCirculation(PactDslWithProvider builder) {
        return reservation(builder.given("a bike parked at station Central")
                                  .uponReceiving("a technician's reservation of a bike that is not withdrawn"), true)
            .willRespondWith()
            .status(409)
            .body(new PactDslJsonBody().stringType("error", "BikeNotWithdrawn").stringType("message", "bike-17 is not withdrawn"))
            .toPact(V4Pact.class);
    }

    @Pact(consumer = FLEET_MAINTENANCE)
    V4Pact reserveUnknownBike(PactDslWithProvider builder) {
        return reservation(builder.uponReceiving("a technician's reservation of a bike Rentals doesn't know"), false)
            .willRespondWith()
            .status(404)
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "reserveWithdrawnBike")
    void reserveWithdrawnBike(MockServer rentals) {
        var expiresAt = reservations(rentals).reserve(new BikeId(BIKE), new TechnicianId(TECHNICIAN));

        assertThat(expiresAt).isEqualTo(Instant.parse(EXPIRES_AT));
    }

    @Test
    @PactTestFor(pactMethod = "reserveBikeHeldByAnotherTechnician")
    void reserveBikeHeldByAnotherTechnician(MockServer rentals) {
        assertThatThrownBy(() -> reservations(rentals).reserve(new BikeId(BIKE), new TechnicianId(TECHNICIAN)))
            .isInstanceOf(AlreadyReserved.class);
    }

    @Test
    @PactTestFor(pactMethod = "reserveBikeInCirculation")
    void reserveBikeInCirculation(MockServer rentals) {
        assertThatThrownBy(() -> reservations(rentals).reserve(new BikeId(BIKE), new TechnicianId(TECHNICIAN)))
            .isInstanceOf(ReservationRefused.class);
    }

    @Test
    @PactTestFor(pactMethod = "reserveUnknownBike")
    void reserveUnknownBike(MockServer rentals) {
        assertThatThrownBy(() -> reservations(rentals).reserve(new BikeId(UNKNOWN_BIKE), new TechnicianId(TECHNICIAN)))
            .isInstanceOf(UnknownBike.class);
    }

    // --- Unlock Bike (for service) ---

    @Pact(consumer = FLEET_MAINTENANCE)
    V4Pact unlockReservedBike(PactDslWithProvider builder) {
        return unlock(builder.given("a withdrawn bike reserved by technician " + TECHNICIAN),
                      "a technician unlocking the withdrawn bike they reserved")
            .willRespondWith()
            .status(200)
            .body(new PactDslJsonBody().stringValue("outcome", "UNLOCKED"))
            .toPact(V4Pact.class);
    }

    @Pact(consumer = FLEET_MAINTENANCE)
    V4Pact unlockBikeReservedByAnotherTechnician(PactDslWithProvider builder) {
        return unlock(builder.given("a withdrawn bike reserved by another technician"),
                      "a technician unlocking a bike another technician reserved")
            .willRespondWith()
            .status(409)
            .body(new PactDslJsonBody().stringValue("error", "NotReservationHolder").stringType("message", "no reservation"))
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "unlockReservedBike")
    void unlockReservedBike(MockServer rentals) {
        var outcome = new RentalsServiceUnlocksAdapter(client(), urls(rentals)).unlock(new BikeId(BIKE), new TechnicianId(TECHNICIAN));

        assertThat(outcome).isEqualTo(ServiceUnlocks.Outcome.UNLOCKED);
    }

    @Test
    @PactTestFor(pactMethod = "unlockBikeReservedByAnotherTechnician")
    void unlockBikeReservedByAnotherTechnician(MockServer rentals) {
        assertThatThrownBy(() -> new RentalsServiceUnlocksAdapter(client(), urls(rentals)).unlock(new BikeId(BIKE),
                                                                                                    new TechnicianId(TECHNICIAN)))
            .isInstanceOf(NotYourReservation.class);
    }

    // --- Make Bike Available ---

    @Pact(consumer = FLEET_MAINTENANCE)
    V4Pact makeAvailableAtStation(PactDslWithProvider builder) {
        return availability(builder, "making a serviced bike available at a station",
                            new PactDslJsonBody().stringValue("stationId", "Central"))
            .willRespondWith()
            .status(200)
            .toPact(V4Pact.class);
    }

    @Pact(consumer = FLEET_MAINTENANCE)
    V4Pact makeAvailableAtPosition(PactDslWithProvider builder) {
        return availability(builder, "making a serviced bike available where the technician left it",
                            new PactDslJsonBody().decimalType("latitude", new BigDecimal("52.2301"))
                                                 .decimalType("longitude", new BigDecimal("21.0111")))
            .willRespondWith()
            .status(200)
            .toPact(V4Pact.class);
    }

    @Pact(consumer = FLEET_MAINTENANCE)
    V4Pact makeAvailableAtUnknownStation(PactDslWithProvider builder) {
        return availability(builder, "making a serviced bike available at a station Rentals doesn't know",
                            new PactDslJsonBody().stringValue("stationId", "Nowhere"))
            .willRespondWith()
            .status(409)
            .body(new PactDslJsonBody().stringValue("error", "UnknownStation").stringType("message", "Unknown station Nowhere"))
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "makeAvailableAtStation")
    void makeAvailableAtStation(MockServer rentals) {
        assertThatCode(() -> circulation(rentals).makeAvailable(new BikeId(BIKE), Place.station("Central")))
            .doesNotThrowAnyException();
    }

    @Test
    @PactTestFor(pactMethod = "makeAvailableAtPosition")
    void makeAvailableAtPosition(MockServer rentals) {
        assertThatCode(() -> circulation(rentals).makeAvailable(new BikeId(BIKE), Place.position(new BigDecimal("52.2301"),
                                                                                                new BigDecimal("21.0111"))))
            .doesNotThrowAnyException();
    }

    @Test
    @PactTestFor(pactMethod = "makeAvailableAtUnknownStation")
    void makeAvailableAtUnknownStation(MockServer rentals) {
        assertThatThrownBy(() -> circulation(rentals).makeAvailable(new BikeId(BIKE), Place.station("Nowhere")))
            .isInstanceOf(UnknownStation.class);
    }

    // --- helpers ---

    private static PactDslRequestWithPath reservation(PactDslRequestWithoutPath request, boolean bikeFromProviderState) {
        var withPath = bikeFromProviderState
            ? request.pathFromProviderState("/rentals/bikes/${bikeId}/reservations", "/rentals/bikes/" + BIKE + "/reservations")
            : request.path("/rentals/bikes/" + UNKNOWN_BIKE + "/reservations");
        return withPath.method("POST")
                       .headers(json())
                       .body(new PactDslJsonBody().stringValue("holderType", "TECHNICIAN").stringValue("holderId", TECHNICIAN));
    }

    private static PactDslRequestWithPath unlock(PactDslWithState builder, String description) {
        return builder.uponReceiving(description)
                      .pathFromProviderState("/rentals/bikes/${bikeId}/unlock", "/rentals/bikes/" + BIKE + "/unlock")
                      .method("POST")
                      .headers(json())
                      .body(new PactDslJsonBody().stringValue("holderType", "TECHNICIAN").stringValue("holderId", TECHNICIAN));
    }

    private static PactDslRequestWithPath availability(PactDslWithProvider builder, String description, PactDslJsonBody body) {
        return builder.given("a withdrawn bike unlocked for technician " + TECHNICIAN)
                      .uponReceiving(description)
                      .pathFromProviderState("/rentals/bikes/${bikeId}/availability", "/rentals/bikes/" + BIKE + "/availability")
                      .method("POST")
                      .headers(json())
                      .body(body);
    }

    private static Map<String, String> json() {
        return Map.of("Content-Type", "application/json");
    }

    private static RentalsServiceReservationsAdapter reservations(MockServer rentals) {
        return new RentalsServiceReservationsAdapter(client(), urls(rentals));
    }

    private static RentalsCirculationReturnsAdapter circulation(MockServer rentals) {
        return new RentalsCirculationReturnsAdapter(client(), urls(rentals));
    }

    private static RestClient client() {
        return RestClient.builder().build();
    }

    private static VelocityServiceUrls urls(MockServer rentals) {
        return urlsOf(rentals.getUrl());
    }
}
