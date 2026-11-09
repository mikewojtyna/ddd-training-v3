package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeEvent.BikeReserved;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeEvent.ReservationCancelled;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeFixture.*;

/// Spec `rider-cancels-reservation`.
class BikeCancellationTest {

    // @formatter:off
    @DisplayName(
        """
         given B-17 is reserved for Anna until 10:02,
         when Anna cancels her reservation at 10:01,
         then the reservation is cancelled at 10:01 (Reservation Cancelled) and Ben can reserve B-17 at 10:01:30
        """
    )
    // @formatter:on
    @Test
    void holderCancelsAndBikeIsFreeAtOnce() {
        // given
        var b17 = reservedFor(ANNA, "B-17", at("10:00"));

        // when
        var events = b17.cancelReservationOf(ANNA).at(at("10:01"));

        // then
        assertThat(eventsOf(events, ReservationCancelled.class)).singleElement()
            .isEqualTo(new ReservationCancelled(b17.id(), ANNA, at("10:01")));
        var bensReservation = b17.reserveForRide(BEN).standingAt(near(CENTRAL, 10)).given(ELIGIBLE).at(at("10:01:30"));
        assertThat(eventsOf(bensReservation, BikeReserved.class)).singleElement()
            .satisfies(reserved -> assertThat(reserved.holder()).isEqualTo(BEN));
    }

    // @formatter:off
    @DisplayName(
        """
         given B-17 is reserved for Anna until 10:02,
         when Ben cancels a reservation of B-17 at 10:01,
         then the cancellation is rejected and B-17 stays reserved for Anna
        """
    )
    // @formatter:on
    @Test
    void otherRiderCannotCancel() {
        // given
        var b17 = reservedFor(ANNA, "B-17", at("10:00"));

        // when / then
        assertThatThrownBy(() -> b17.cancelReservationOf(BEN).at(at("10:01"))).isInstanceOf(NotReservationHolder.class);
        assertThat(b17.isHeldBy(ANNA, at("10:01"))).isTrue();
    }

    // @formatter:off
    @DisplayName(
        """
         given Anna unlocked B-17 at 10:01 and is riding it,
         when Anna cancels her reservation of B-17 at 10:05,
         then the cancellation is rejected and Anna's ride goes on
        """
    )
    // @formatter:on
    @Test
    void rideInProgressCannotBeCancelled() {
        // given
        var b17 = inSessionFor(ANNA, "B-17", at("10:01"));

        // when / then
        assertThatThrownBy(() -> b17.cancelReservationOf(ANNA).at(at("10:05"))).isInstanceOf(NoReservationToCancel.class);
        assertThat(b17.isHeldBy(ANNA, at("10:05"))).isTrue();
    }
}
