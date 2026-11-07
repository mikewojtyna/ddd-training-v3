package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

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
            .forDuration(new RentalDuration(Duration.ofHours(6)));

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
                                                        .forDuration(new RentalDuration(Duration.ofHours(6))));

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
        var events = bikeStation.rent(bike).by(borrower).forDuration(new RentalDuration(Duration.ofHours(6)));

        // then
        events.hasAtLeastOneEventMatching(event -> event instanceof BikeRentalEvent.BikeRented);

        // when
        var exception = catchThrowable(() -> bikeStation.rent(bike).by(borrower)
                                                        .forDuration(new RentalDuration(Duration.ofHours(6))));

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
                                                        .forDuration(new RentalDuration(Duration.ofHours(12))));

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
        var events = central.rent(b17).by(george).forDuration(oneHour);

        // then B-17 is rented: the event names it, and nobody else can rent it
        assertThat(events.hasAtLeastOneEventMatching(
            event -> event instanceof BikeRentalEvent.BikeRented rented && rented.bike().equals(b17.id()))).isTrue();
        assertThat(catchThrowable(() -> central.rent(b17).by(anna).forDuration(oneHour)))
            .isInstanceOf(BikeNotAvailableException.class);

        // and B-22 stays available
        var benEvents = central.rent(b22).by(ben).forDuration(oneHour);
        assertThat(benEvents.hasAtLeastOneEventMatching(
            event -> event instanceof BikeRentalEvent.BikeRented rented && rented.bike().equals(b22.id()))).isTrue();
    }
}
