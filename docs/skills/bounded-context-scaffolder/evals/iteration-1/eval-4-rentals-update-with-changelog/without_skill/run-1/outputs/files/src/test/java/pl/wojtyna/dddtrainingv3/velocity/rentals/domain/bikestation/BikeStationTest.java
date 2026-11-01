package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

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
         given bike station where bike "bike01" was rented by George,
         when bike "bike01" is returned to the station,
         then bike is returned and is available again
        """
    )
    // @formatter:on
    @Test
    void test4() {
        // given
        var bikeStation = new BikeStation(new BikeStationId("Warsaw01"));
        var bike = new Bike(new BikeId("bike01"));
        bikeStation.placeNew(bike);
        var borrower = new Borrower(new BorrowerId("george"));
        bikeStation.rent(bike).by(borrower).forDuration(new RentalDuration(Duration.ofHours(6)));

        // when
        var events = bikeStation.returnBike(bike);

        // then
        assertThat(events.hasAtLeastOneEventMatching(
            event -> event.equals(new BikeRentalEvent.BikeReturned(new BikeStationId("Warsaw01"), bike, borrower))))
            .isTrue();
        assertThat(bikeStation.availableBikes()).isEqualTo(List.of(bike));
    }

    // @formatter:off
    @DisplayName(
        """
         given bike station where bike "bike01" is available,
         when bike "bike01" is returned to the station,
         then return is rejected
        """
    )
    // @formatter:on
    @Test
    void test5() {
        // given
        var bikeStation = new BikeStation(new BikeStationId("Warsaw01"));
        var bike = new Bike(new BikeId("bike01"));
        bikeStation.placeNew(bike);

        // when
        var exception = catchThrowable(() -> bikeStation.returnBike(bike));

        // then
        assertThat(exception).isInstanceOf(BikeNotRentedException.class);
    }

    // @formatter:off
    @DisplayName(
        """
         given bike station with 1 bike,
         when George rents a bike for 11 hours and 1 minute,
         then bike is not rent
        """
    )
    // @formatter:on
    @Test
    void test6() {
        // given
        var bikeStation = new BikeStation(new BikeStationId("Warsaw01"));
        var bike = new Bike(new BikeId("bike01"));
        bikeStation.placeNew(bike);
        var borrower = new Borrower(new BorrowerId("george"));

        // when
        var exception = catchThrowable(() -> bikeStation.rent(bike).by(borrower)
                                                        .forDuration(new RentalDuration(
                                                            Duration.ofHours(11).plusMinutes(1))));

        // then
        assertThat(exception).isInstanceOf(MaximumRentalDurationExceededException.class);
    }

    // @formatter:off
    @DisplayName(
        """
         given bike station with bikes "bike01" and "bike02",
         when George rents bike "bike02",
         then only bike "bike01" is available at the station
        """
    )
    // @formatter:on
    @Test
    void test7() {
        // given
        var bikeStation = new BikeStation(new BikeStationId("Warsaw01"));
        var bike01 = new Bike(new BikeId("bike01"));
        var bike02 = new Bike(new BikeId("bike02"));
        bikeStation.placeNew(bike01);
        bikeStation.placeNew(bike02);
        var borrower = new Borrower(new BorrowerId("george"));

        // when
        bikeStation.rent(bike02).by(borrower).forDuration(new RentalDuration(Duration.ofHours(6)));

        // then
        assertThat(bikeStation.availableBikes()).isEqualTo(List.of(bike01));
    }
}