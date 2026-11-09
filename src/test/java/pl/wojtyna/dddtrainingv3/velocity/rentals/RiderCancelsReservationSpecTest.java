package pl.wojtyna.dddtrainingv3.velocity.rentals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.velocity.common.VelocitySpringTest;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.wojtyna.dddtrainingv3.velocity.rentals.RentalsDriver.CENTRAL;

/// Acceptance tests of `docs/specs/rider-cancels-reservation.md`.
@VelocitySpringTest
@DisplayName("Spec rider-cancels-reservation")
class RiderCancelsReservationSpecTest extends RentalsSpecTestBase {

    // @formatter:off
    @DisplayName(
        """
         E1.1 given B-17 is reserved for Anna until 10:02,
         when Anna cancels her reservation at 10:01,
         then the reservation is cancelled and Ben, standing 10 m from B-17, can reserve it at 10:01:30
        """
    )
    // @formatter:on
    @Test
    void e1_1_rider_cancels_reservation_and_bike_is_free_at_once() {
        // given
        at("2026-11-10T09:00:00Z");
        var anna = rider("anna");
        var ben = rider("ben");
        at("2026-11-10T10:00:00Z");
        var b17 = bike("B-17");
        rentals.reserved(b17, CENTRAL, anna);

        // when
        at("2026-11-10T10:01:00Z");
        var cancellation = rentals.cancelReservation(b17, anna);

        // then
        assertThat(cancellation.status()).isEqualTo(200);
        assertThat(rentals.activeRentalOf(anna).status()).isEqualTo(404);
        at("2026-11-10T10:01:30Z");
        assertThat(rentals.reserve(b17, ben, CENTRAL.northBy(10)).status()).isEqualTo(201);
    }

    // @formatter:off
    @DisplayName(
        """
         E2.1 given B-17 is reserved for Anna until 10:02,
         when Ben cancels a reservation of B-17 at 10:01,
         then the cancellation is rejected and B-17 stays reserved for Anna
        """
    )
    // @formatter:on
    @Test
    void e2_1_other_rider_cannot_cancel() {
        // given
        at("2026-11-10T09:00:00Z");
        var anna = rider("anna");
        var ben = rider("ben");
        at("2026-11-10T10:00:00Z");
        var b17 = bike("B-17");
        rentals.reserved(b17, CENTRAL, anna);

        // when
        at("2026-11-10T10:01:00Z");
        var cancellation = rentals.cancelReservation(b17, ben);

        // then
        assertThat(cancellation.status()).isEqualTo(409);
        assertThat(cancellation.error()).isEqualTo("NotReservationHolder");
        assertThat(rentals.bike(b17).body().holderId()).isEqualTo(anna);
    }

    // @formatter:off
    @DisplayName(
        """
         E3.1 given Anna unlocked B-17 at 10:01 and is riding it,
         when Anna cancels her reservation of B-17 at 10:05,
         then the cancellation is rejected and Anna's ride goes on
        """
    )
    // @formatter:on
    @Test
    void e3_1_ride_in_progress_cannot_be_cancelled() {
        // given
        at("2026-11-10T09:00:00Z");
        var anna = rider("anna");
        at("2026-11-10T10:01:00Z");
        var b17 = bike("B-17");
        rentals.riding(b17, CENTRAL, anna);

        // when
        at("2026-11-10T10:05:00Z");
        var cancellation = rentals.cancelReservation(b17, anna);

        // then
        assertThat(cancellation.status()).isEqualTo(409);
        assertThat(cancellation.error()).isEqualTo("NoReservationToCancel");
        assertThat(rentals.activeRentalOf(anna).body().kind()).isEqualTo("SESSION");
    }
}
