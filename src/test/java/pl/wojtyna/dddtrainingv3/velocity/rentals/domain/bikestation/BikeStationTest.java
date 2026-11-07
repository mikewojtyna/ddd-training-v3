package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;

@DisplayName("Bike station examples")
class BikeStationTest {

    // @formatter:off
    @DisplayName(
        """
         given bike station with 1 bike,
         when George rents a bike,
         then bike is rent
        """
    )
    // @formatter:on
    @Test
    void test0() {
        // given
        var bikeStation = new BikeStation(new BikeStationId("Warsaw01"));
        var bike = new Bike(new BikeId("bike01"));
        bikeStation.placeNew(bike);
        var borrower = new Borrower(new BorrowerId("george"));

        // when
        var events = bikeStation
            .rent(bike)
            .by(borrower)
            .forDuration(new RentalDuration(Duration.ofHours(6))).at(at("09:00"));

        // then
        events.hasAtLeastOneEventMatching(event -> event instanceof BikeRentalEvent.BikeRented);
    }

    // @formatter:off
    @DisplayName(
        """
         given bike station with 0 bikes,
         when George rents a bike,
         then bike is not rent
        """
    )
    // @formatter:on
    @Test
    void test1() {
        // given
        var bikeStation = new BikeStation(new BikeStationId("Warsaw01"));
        var bike = new Bike(new BikeId("bike01"));
        var borrower = new Borrower(new BorrowerId("george"));

        // when
        var exception = catchThrowable(() -> bikeStation.rent(bike).by(borrower)
                                                        .forDuration(new RentalDuration(Duration.ofHours(6))).at(at("09:00")));

        // then
        assertThat(exception).isInstanceOf(BikeNotAvailableException.class);
    }

    // @formatter:off
    @DisplayName(
        """
         given bike station with 1 bike,
         when George rents a bike,
         then bike is rent,
         and when George rents a bike again,
         then bike is not rent
        """
    )
    // @formatter:on
    @Test
    void test2() {
        // given
        var bikeStation = new BikeStation(new BikeStationId("Warsaw01"));
        var bike = new Bike(new BikeId("bike01"));
        bikeStation.placeNew(bike);
        var borrower = new Borrower(new BorrowerId("george"));

        // when
        var events = bikeStation.rent(bike).by(borrower).forDuration(new RentalDuration(Duration.ofHours(6))).at(at("09:00"));

        // then
        events.hasAtLeastOneEventMatching(event -> event instanceof BikeRentalEvent.BikeRented);

        // when
        var exception = catchThrowable(() -> bikeStation.rent(bike).by(borrower)
                                                        .forDuration(new RentalDuration(Duration.ofHours(6))).at(at("09:00")));

        // then
        assertThat(exception).isInstanceOf(BikeNotAvailableException.class);
    }

    // @formatter:off
    @DisplayName(
        """
         given bike station with 1 bike,
         when George rents a bike for 12 hours,
         then bike is not rent
        """
    )
    // @formatter:on
    @Test
    void test3() {
        // given
        var bikeStation = new BikeStation(new BikeStationId("Warsaw01"));
        var bike = new Bike(new BikeId("bike01"));
        bikeStation.placeNew(bike);
        var borrower = new Borrower(new BorrowerId("george"));

        // when
        var exception = catchThrowable(() -> bikeStation.rent(bike).by(borrower)
                                                        .forDuration(new RentalDuration(Duration.ofHours(12))).at(at("09:00")));

        // then
        assertThat(exception).isInstanceOf(MaximumRentalDurationExceededException.class);
    }

    // @formatter:off
    @DisplayName(
        """
         given bikes B-17 and B-22 are available at station Central,
         when George rents B-17,
         then B-17 is rented and B-22 stays available
        """
    )
    // @formatter:on
    @Test
    void rentsExactlyTheRequestedBike() {
        // given
        var central = new BikeStation(new BikeStationId("Central"));
        var b17 = new Bike(new BikeId("B-17"));
        var b22 = new Bike(new BikeId("B-22"));
        central.placeNew(b17);
        central.placeNew(b22);
        var george = new Borrower(new BorrowerId("george"));
        var anna = new Borrower(new BorrowerId("anna"));
        var ben = new Borrower(new BorrowerId("ben"));
        var oneHour = new RentalDuration(Duration.ofHours(1));

        // when
        var events = central.rent(b17).by(george).forDuration(oneHour).at(at("09:00"));

        // then B-17 is rented: the event names it, and nobody else can rent it
        assertThat(events.hasAtLeastOneEventMatching(
            event -> event instanceof BikeRentalEvent.BikeRented rented && rented.bike().equals(b17.id()))).isTrue();
        assertThat(catchThrowable(() -> central.rent(b17).by(anna).forDuration(oneHour).at(at("09:00"))))
            .isInstanceOf(BikeNotAvailableException.class);

        // and B-22 stays available
        var benEvents = central.rent(b22).by(ben).forDuration(oneHour).at(at("09:00"));
        assertThat(benEvents.hasAtLeastOneEventMatching(
            event -> event instanceof BikeRentalEvent.BikeRented rented && rented.bike().equals(b22.id()))).isTrue();
    }

    // @formatter:off
    @DisplayName(
        """
         E1.1: given bike B-17 is available at station Central and Anna is at home 2 km away,
         when Anna reserves B-17 at 10:00,
         then B-17 is reserved for Anna until 10:15 (Bike Reserved)
        """
    )
    // @formatter:on
    @Test
    void reservesAnAvailableBikeForFifteenMinutes() {
        // given
        var central = new BikeStation(new BikeStationId("Central"));
        var b17 = new Bike(new BikeId("B-17"));
        central.placeNew(b17);
        var anna = new Borrower(new BorrowerId("anna"));

        // when
        var events = central.reserve(b17).by(anna).at(at("10:00"));

        // then
        assertThat(events.hasAtLeastOneEventMatching(event -> event.equals(new BikeRentalEvent.BikeReserved(
            new BikeStationId("Central"), b17.id(), anna, at("10:00"), at("10:15"))))).isTrue();
    }

    // @formatter:off
    @DisplayName(
        """
         E1.2: given B-17 at Central has just been rented by Ben,
         when Anna reserves B-17,
         then the reservation is rejected
        """
    )
    // @formatter:on
    @Test
    void rejectsReservingARentedBike() {
        // given
        var central = new BikeStation(new BikeStationId("Central"));
        var b17 = new Bike(new BikeId("B-17"));
        central.placeNew(b17);
        var ben = new Borrower(new BorrowerId("ben"));
        var anna = new Borrower(new BorrowerId("anna"));
        central.rent(b17).by(ben).forDuration(new RentalDuration(Duration.ofHours(1))).at(at("09:00"));

        // when
        var exception = catchThrowable(() -> central.reserve(b17).by(anna).at(at("10:05")));

        // then
        assertThat(exception).isInstanceOf(BikeNotAvailableException.class);
    }

    // @formatter:off
    @DisplayName(
        """
         E3.1: given B-17 is reserved for Anna until 10:15,
         when Ben reserves B-17 at 10:05,
         then the reservation is rejected
        """
    )
    // @formatter:on
    @Test
    void rejectsReservingABikeReservedForSomeoneElse() {
        // given
        var central = new BikeStation(new BikeStationId("Central"));
        var b17 = new Bike(new BikeId("B-17"));
        central.placeNew(b17);
        var anna = new Borrower(new BorrowerId("anna"));
        var ben = new Borrower(new BorrowerId("ben"));
        central.reserve(b17).by(anna).at(at("10:00"));

        // when
        var exception = catchThrowable(() -> central.reserve(b17).by(ben).at(at("10:05")));

        // then
        assertThat(exception).isInstanceOf(BikeNotAvailableException.class);
    }

    // @formatter:off
    @DisplayName(
        """
         E3.2: given B-17 is reserved for Anna until 10:15,
         when Ben rents B-17 at 10:15,
         then the rental is rejected, because the reservation still holds at its end time
        """
    )
    // @formatter:on
    @Test
    void rejectsRentingABikeReservedForSomeoneElseUpToTheEndOfTheWindow() {
        // given
        var central = new BikeStation(new BikeStationId("Central"));
        var b17 = new Bike(new BikeId("B-17"));
        central.placeNew(b17);
        var anna = new Borrower(new BorrowerId("anna"));
        var ben = new Borrower(new BorrowerId("ben"));
        central.reserve(b17).by(anna).at(at("10:00"));

        // when
        var exception = catchThrowable(
            () -> central.rent(b17).by(ben).forDuration(new RentalDuration(Duration.ofHours(1))).at(at("10:15")));

        // then
        assertThat(exception).isInstanceOf(BikeNotAvailableException.class);
    }

    // @formatter:off
    @DisplayName(
        """
         D9 (plan decision): given B-17 was reserved for Anna at 10:00,
         when Ben reserves B-17 at 10:16,
         then the reservation is accepted, because Anna's window has passed
        """
    )
    // @formatter:on
    @Test
    void aReservationStopsCountingAfterItsWindow() {
        // given
        var central = new BikeStation(new BikeStationId("Central"));
        var b17 = new Bike(new BikeId("B-17"));
        central.placeNew(b17);
        var anna = new Borrower(new BorrowerId("anna"));
        var ben = new Borrower(new BorrowerId("ben"));
        central.reserve(b17).by(anna).at(at("10:00"));

        // when
        var events = central.reserve(b17).by(ben).at(at("10:16"));

        // then
        assertThat(events.hasAtLeastOneEventMatching(event -> event.equals(new BikeRentalEvent.BikeReserved(
            new BikeStationId("Central"), b17.id(), ben, at("10:16"), at("10:31"))))).isTrue();
    }

    // @formatter:off
    @DisplayName(
        """
         E5.1: given B-31 at Central has been withdrawn (reported broken),
         when Anna reserves B-31,
         then the reservation is rejected
        """
    )
    // @formatter:on
    @Test
    void rejectsReservingAWithdrawnBike() {
        // given
        var central = new BikeStation(new BikeStationId("Central"));
        var b31 = new Bike(new BikeId("B-31"));
        central.placeNew(b31);
        central.withdraw(b31);
        var anna = new Borrower(new BorrowerId("anna"));

        // when
        var exception = catchThrowable(() -> central.reserve(b31).by(anna).at(at("10:00")));

        // then
        assertThat(exception).isInstanceOf(BikeNotAvailableException.class);
    }

    // @formatter:off
    @DisplayName(
        """
         E8.2: given Anna holds a reservation for B-17 until 10:15,
         when Anna rents B-17 at 10:10,
         then the rental starts for Anna and her reservation ends (Bike Rented, from reservation)
        """
    )
    // @formatter:on
    @Test
    void theHolderRentsTheReservedBikeAndTheReservationEnds() {
        // given
        var central = new BikeStation(new BikeStationId("Central"));
        var b17 = new Bike(new BikeId("B-17"));
        central.placeNew(b17);
        var anna = new Borrower(new BorrowerId("anna"));
        var oneHour = new RentalDuration(Duration.ofHours(1));
        central.reserve(b17).by(anna).at(at("10:00"));

        // when
        var events = central.rent(b17).by(anna).forDuration(oneHour).at(at("10:10"));

        // then
        assertThat(events.hasAtLeastOneEventMatching(
            event -> event.equals(new BikeRentalEvent.BikeRented(b17.id(), anna, oneHour, true)))).isTrue();
    }

    private static Instant at(String time) {
        return Instant.parse("2026-11-07T" + time + ":00Z");
    }
}
