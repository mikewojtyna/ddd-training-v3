package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.borrowing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BikeId;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BorrowerId;

import java.time.Instant;

import static pl.wojtyna.dddtrainingv3.velocity.rentals.domain.eligibility.Eligibility.ELIGIBLE;
import static pl.wojtyna.dddtrainingv3.velocity.rentals.domain.eligibility.Eligibility.NOT_ELIGIBLE;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@DisplayName("Borrowing examples")
class BorrowingTest {

    private static final BikeId B17 = new BikeId("B-17");
    private static final BikeId B22 = new BikeId("B-22");

    // @formatter:off
    @DisplayName(
        """
         E2.1: given Anna holds a reservation for B-17 at Central until 10:15,
         when Anna reserves B-22 at station Riverside at 10:05,
         then the reservation is rejected
        """
    )
    // @formatter:on
    @Test
    void rejectsASecondActiveReservation() {
        // given
        var anna = new Borrowing(new BorrowerId("anna"));
        anna.reserve(B17).as(ELIGIBLE).at(at("10:00"));

        // when
        var exception = catchThrowable(() -> anna.reserve(B22).as(ELIGIBLE).at(at("10:05")));

        // then
        assertThat(exception).isInstanceOf(AlreadyHoldsReservationException.class);
    }

    // @formatter:off
    @DisplayName(
        """
         E7.1: given Anna rented B-05 at 09:40 and is still riding it,
         when Anna reserves B-17 at Central at 10:00,
         then the reservation is rejected
        """
    )
    // @formatter:on
    @Test
    void rejectsReservingWhileRiding() {
        // given
        var anna = new Borrowing(new BorrowerId("anna"));
        anna.rent(new BikeId("B-05")).at(at("09:40"));

        // when
        var exception = catchThrowable(() -> anna.reserve(B17).as(ELIGIBLE).at(at("10:00")));

        // then
        assertThat(exception).isInstanceOf(AlreadyRidingException.class);
    }

    // @formatter:off
    @DisplayName(
        """
         E8.1: given Anna holds a reservation for B-17 at Central until 10:15 and B-22 is available at Central,
         when Anna rents B-22 at 10:05,
         then the rental is rejected and B-17 stays reserved for her
        """
    )
    // @formatter:on
    @Test
    void aHolderCanRentOnlyTheReservedBike() {
        // given
        var anna = new Borrowing(new BorrowerId("anna"));
        anna.reserve(B17).as(ELIGIBLE).at(at("10:00"));

        // when
        var exception = catchThrowable(() -> anna.rent(B22).at(at("10:05")));

        // then
        assertThat(exception).isInstanceOf(HoldsReservationForAnotherBikeException.class);
        // and B-17 stays reserved for her: she still can't hold a second reservation
        assertThat(catchThrowable(() -> anna.reserve(B22).as(ELIGIBLE).at(at("10:06"))))
            .isInstanceOf(AlreadyHoldsReservationException.class);
    }

    // @formatter:off
    @DisplayName(
        """
         E8.2 (borrower side): given Anna holds a reservation for B-17 until 10:15,
         when Anna rents B-17 at 10:10,
         then the rental is accepted
        """
    )
    // @formatter:on
    @Test
    void aHolderCanRentTheReservedBike() {
        // given
        var anna = new Borrowing(new BorrowerId("anna"));
        anna.reserve(B17).as(ELIGIBLE).at(at("10:00"));

        // when
        var exception = catchThrowable(() -> anna.rent(B17).at(at("10:10")));

        // then
        assertThat(exception).isNull();
    }

    // @formatter:off
    @DisplayName(
        """
         E6.1: given rider Carl hasn't finished onboarding (no verified payment method),
         when Carl reserves B-17,
         then the reservation is rejected
        """
    )
    // @formatter:on
    @Test
    void onlyAnEligibleBorrowerCanReserve() {
        // given
        var carl = new Borrowing(new BorrowerId("carl"));

        // when
        var exception = catchThrowable(() -> carl.reserve(B17).as(NOT_ELIGIBLE).at(at("10:00")));

        // then
        assertThat(exception).isInstanceOf(BorrowerNotEligibleException.class);
    }

    private static Instant at(String time) {
        return Instant.parse("2026-11-07T" + time + ":00Z");
    }
}
