package pl.wojtyna.dddtrainingv3.velocity.rentals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvent;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.queries.AvailableBikesAtStation;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.queries.AvailableBikesAtStationHandler;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases.*;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.*;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.borrowing.AlreadyHoldsReservationException;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.borrowing.AlreadyRidingException;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.borrowing.BorrowerNotEligibleException;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.borrowing.HoldsReservationForAnotherBikeException;
import pl.wojtyna.dddtrainingv3.velocity.rentals.infra.adapters.secondary.InMemoryBikeStationRepo;
import pl.wojtyna.dddtrainingv3.velocity.rentals.infra.adapters.secondary.InMemoryBorrowingRepository;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/// Acceptance tests for `docs/specs/rider-reserves-bike-at-station.md`: one test per spec example, driving the
/// Rentals use cases with in-memory adapters, a settable clock and a fake Rider Accounts eligibility.
/// These prove the spec; the domain tests (`BikeStationTest`, `BorrowingTest`) prove the design.
@DisplayName("Spec: a rider wants to reserve a bike at a bike station")
class RiderReservesBikeAtStationSpecTest {

    private final InMemoryBikeStationRepo stations = new InMemoryBikeStationRepo();
    private final InMemoryBorrowingRepository borrowings = new InMemoryBorrowingRepository();
    private final SettableClock clock = new SettableClock();
    private final RecordingPublisher published = new RecordingPublisher();
    private final Set<BorrowerId> notActivated = new java.util.HashSet<>();

    private final ReserveBikeUseCase reserveBike =
        new ReserveBikeUseCase(stations, borrowings, borrower -> !notActivated.contains(borrower), clock, published);
    private final RentBikeUseCase rentBike = new RentBikeUseCase(stations, borrowings, clock, published);
    private final WithdrawBikeUseCase withdrawBike = new WithdrawBikeUseCase(stations, published);
    private final AvailableBikesAtStationHandler availableBikes = new AvailableBikesAtStationHandler(stations, clock);

    @BeforeEach
    void stations() {
        var central = new BikeStation(new BikeStationId("Central"));
        List.of("B-05", "B-17", "B-22", "B-31").forEach(bike -> central.placeNew(new Bike(new BikeId(bike))));
        stations.save(central);
        var riverside = new BikeStation(new BikeStationId("Riverside"));
        riverside.placeNew(new Bike(new BikeId("B-22R")));
        stations.save(riverside);
    }

    @Test
    @DisplayName("E1.1: given B-17 is available at Central and Anna is at home 2 km away, when Anna reserves B-17 at 10:00, then B-17 is reserved for Anna until 10:15 (Bike Reserved)")
    void e1_1_reservesAnAvailableBikeRemotely() {
        reserveAt("10:00", "Central", "B-17", "anna");

        assertThat(published.events).contains(new BikeRentalEvent.BikeReserved(
            new BikeStationId("Central"), new BikeId("B-17"), borrower("anna"), at("10:00"), at("10:15")));
    }

    @Test
    @DisplayName("E1.2: given B-17 at Central has just been rented by Ben, when Anna reserves B-17, then the reservation is rejected")
    void e1_2_aRentedBikeCantBeReserved() {
        rentAt("09:55", "Central", "B-17", "ben");

        assertThat(catchThrowable(() -> reserveAt("10:00", "Central", "B-17", "anna")))
            .isInstanceOf(BikeNotAvailableException.class);
    }

    @Test
    @DisplayName("E2.1: given Anna holds a reservation for B-17 at Central until 10:15, when Anna reserves B-22 at Riverside at 10:05, then the reservation is rejected and B-22 stays available")
    void e2_1_oneActiveReservationPerRider() {
        reserveAt("10:00", "Central", "B-17", "anna");

        assertThat(catchThrowable(() -> reserveAt("10:05", "Riverside", "B-22R", "anna")))
            .isInstanceOf(AlreadyHoldsReservationException.class);
        assertThat(availableAt("10:05", "Riverside")).containsExactly("B-22R");
    }

    @Test
    @DisplayName("E3.1: given B-17 is reserved for Anna until 10:15, when Ben reserves B-17 at 10:05, then the reservation is rejected")
    void e3_1_nobodyElseCanReserveAReservedBike() {
        reserveAt("10:00", "Central", "B-17", "anna");

        assertThat(catchThrowable(() -> reserveAt("10:05", "Central", "B-17", "ben")))
            .isInstanceOf(BikeNotAvailableException.class);
    }

    @Test
    @DisplayName("E3.2: given B-17 is reserved for Anna until 10:15, when Ben rents B-17 at 10:15, then the rental is rejected, because the reservation still holds at its end time")
    void e3_2_nobodyElseCanRentAReservedBikeUpToTheEndOfItsWindow() {
        reserveAt("10:00", "Central", "B-17", "anna");

        assertThat(catchThrowable(() -> rentAt("10:15", "Central", "B-17", "ben")))
            .isInstanceOf(BikeNotAvailableException.class);
    }

    @Test
    @DisplayName("E4.1: given B-17 and B-22 are at Central, B-17 is reserved for Anna and B-22 is available, when Ben asks which bikes are available at Central, then he doesn't see B-17")
    void e4_1_aReservedBikeIsNotOfferedToOthers() {
        reserveAt("10:00", "Central", "B-17", "anna");

        assertThat(availableAt("10:05", "Central")).contains("B-22").doesNotContain("B-17");
    }

    @Test
    @DisplayName("E5.1: given B-31 at Central has been reported broken, when Anna reserves B-31, then the reservation is rejected")
    void e5_1_aWithdrawnBikeCantBeReserved() {
        withdrawBike.handle(new WithdrawBike("B-31"));

        assertThat(catchThrowable(() -> reserveAt("10:00", "Central", "B-31", "anna")))
            .isInstanceOf(BikeNotAvailableException.class);
    }

    @Test
    @DisplayName("E6.1: given rider Carl hasn't finished onboarding (no verified payment method), when Carl reserves B-17, then the reservation is rejected and B-17 stays available")
    void e6_1_onlyAnActivatedAccountCanReserve() {
        notActivated.add(new BorrowerId("carl"));

        assertThat(catchThrowable(() -> reserveAt("10:00", "Central", "B-17", "carl")))
            .isInstanceOf(BorrowerNotEligibleException.class);
        assertThat(availableAt("10:00", "Central")).contains("B-17");
    }

    @Test
    @DisplayName("E7.1: given Anna rented B-05 at 09:40 and is still riding it, when Anna reserves B-17 at Central at 10:00, then the reservation is rejected and B-17 stays available")
    void e7_1_noReservingWhileRiding() {
        rentAt("09:40", "Central", "B-05", "anna");

        assertThat(catchThrowable(() -> reserveAt("10:00", "Central", "B-17", "anna")))
            .isInstanceOf(AlreadyRidingException.class);
        assertThat(availableAt("10:00", "Central")).contains("B-17");
    }

    @Test
    @DisplayName("E8.1: given Anna holds a reservation for B-17 at Central until 10:15 and B-22 is available, when Anna rents B-22 at 10:05, then the rental is rejected and B-17 stays reserved for her")
    void e8_1_aHolderCanRentOnlyTheReservedBike() {
        reserveAt("10:00", "Central", "B-17", "anna");

        assertThat(catchThrowable(() -> rentAt("10:05", "Central", "B-22", "anna")))
            .isInstanceOf(HoldsReservationForAnotherBikeException.class);
        assertThat(availableAt("10:05", "Central")).contains("B-22").doesNotContain("B-17");
    }

    @Test
    @DisplayName("E8.2: given Anna holds a reservation for B-17 until 10:15, when Anna rents B-17 at 10:10, then the rental starts for Anna and her reservation ends (Bike Rented)")
    void e8_2_theHolderRentsTheReservedBike() {
        reserveAt("10:00", "Central", "B-17", "anna");

        rentAt("10:10", "Central", "B-17", "anna");

        assertThat(published.events).contains(new BikeRentalEvent.BikeRented(
            new BikeId("B-17"), borrower("anna"), new RentalDuration(Duration.ofHours(1)), true));
    }

    // ---- helpers ----

    private void reserveAt(String time, String station, String bike, String rider) {
        clock.set(at(time));
        reserveBike.handle(new ReserveBike(station, bike, rider));
    }

    private void rentAt(String time, String station, String bike, String rider) {
        clock.set(at(time));
        rentBike.rent(new BikeStationId(station), new BikeId(bike), new BorrowerId(rider),
                      new RentalDuration(Duration.ofHours(1)));
    }

    private List<String> availableAt(String time, String station) {
        clock.set(at(time));
        return availableBikes.handle(new AvailableBikesAtStation(station)).bikeIds();
    }

    private static Borrower borrower(String id) {
        return new Borrower(new BorrowerId(id));
    }

    private static Instant at(String time) {
        return Instant.parse("2026-11-07T" + time + ":00Z");
    }

    private static final class SettableClock extends Clock {

        private Instant now = Instant.parse("2026-11-07T09:00:00Z");

        void set(Instant instant) {
            this.now = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private static final class RecordingPublisher implements DomainEventPublisher {

        private final List<DomainEvent> events = new ArrayList<>();

        @Override
        public void publish(DomainEvents domainEvents) {
            domainEvents.stream().forEach(events::add);
        }

        @Override
        public void publish(DomainEvent event) {
            events.add(event);
        }
    }
}
