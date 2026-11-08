package pl.wojtyna.dddtrainingv3.velocity.rentals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pl.wojtyna.dddtrainingv3.velocity.common.VelocitySpringTest;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.events.ReservationExpired;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases.ReleaseExpiredReservations;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases.ReleaseExpiredReservationsUseCase;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static pl.wojtyna.dddtrainingv3.velocity.rentals.RentalsDriver.CENTRAL;
import static pl.wojtyna.dddtrainingv3.velocity.rentals.RentalsDriver.OLD_TOWN;

/// Acceptance tests of `docs/specs/fleet-operations-releases-expired-reservations.md`. "The reservations are checked at
/// T" sets the clock to T and calls the use case the scheduled job calls in production.
@VelocitySpringTest
@DisplayName("Spec fleet-operations-releases-expired-reservations")
class FleetOperationsReleasesExpiredReservationsSpecTest extends RentalsSpecTestBase {

    @Autowired
    private ReleaseExpiredReservationsUseCase releaseExpiredReservations;

    private void reservationsCheckedAt(String isoInstant) {
        at(isoInstant);
        releaseExpiredReservations.handle(new ReleaseExpiredReservations(Instant.parse(isoInstant)));
    }

    // @formatter:off
    @DisplayName(
        """
         E1.1 given B-17 at station Central was reserved for Anna at 10:00, until 10:02, and Anna hasn't unlocked it,
         when the reservations are checked at 10:02:01,
         then Anna's reservation expired and B-17 is available to anyone (Reservation Expired)
        """
    )
    // @formatter:on
    @Test
    void e1_1_unused_rider_reservation_expires_and_bike_is_available() {
        // given
        at("2026-11-08T10:00:00Z");
        var anna = rider("anna");
        var b17 = bike("B-17");
        rentals.reserved(b17, CENTRAL, anna);

        // when
        reservationsCheckedAt("2026-11-08T10:02:01Z");

        // then
        assertThat(rentals.bike(b17).body().hold()).isEqualTo("FREE");
        assertThat(rentals.activeRentalOf(anna).status()).isEqualTo(404);
        await().atMost(HAND_OFF).untilAsserted(() -> assertThat(rentals.published(ReservationExpired.class, event -> event.bikeId().equals(b17)))
            .containsExactly(new ReservationExpired(b17, "RIDER", anna, Instant.parse("2026-11-08T10:02:01Z"))));
    }

    // @formatter:off
    @DisplayName(
        """
         E1.2 given B-17 was reserved for Anna at 10:00, until 10:02,
         when the reservations are checked at exactly 10:02:00,
         then B-17 is still reserved for Anna
        """
    )
    // @formatter:on
    @Test
    void e1_2_rider_reservation_still_holds_at_exact_end() {
        // given
        at("2026-11-08T10:00:00Z");
        var anna = rider("anna");
        var b17 = bike("B-17");
        rentals.reserved(b17, CENTRAL, anna);

        // when
        reservationsCheckedAt("2026-11-08T10:02:00Z");

        // then
        assertThat(rentals.bike(b17).body().hold()).isEqualTo("RESERVED");
        assertThat(rentals.bike(b17).body().holderId()).isEqualTo(anna);
    }

    // @formatter:off
    @DisplayName(
        """
         E1.3 given Anna's reservation of B-17 expired at 10:02:01,
         when rider Ben, standing 10 m from B-17, reserves it at 10:04,
         then B-17 is reserved for Ben until 10:06
        """
    )
    // @formatter:on
    @Test
    void e1_3_next_rider_reserves_bike_after_expiry() {
        // given
        at("2026-11-08T10:00:00Z");
        var anna = rider("anna");
        var ben = rider("ben");
        var b17 = bike("B-17");
        rentals.reserved(b17, CENTRAL, anna);
        reservationsCheckedAt("2026-11-08T10:02:01Z");

        // when
        at("2026-11-08T10:04:00Z");
        var reservation = rentals.reserve(b17, ben, CENTRAL.northBy(10));

        // then
        assertThat(reservation.status()).isEqualTo(201);
        assertThat(reservation.body().expiresAt()).isEqualTo(Instant.parse("2026-11-08T10:06:00Z"));
    }

    // @formatter:off
    @DisplayName(
        """
         E2.1 given broken B-31 was reserved for technician Tom at 09:00, until 09:30, and Tom hasn't unlocked it,
         when the reservations are checked at 09:30:01,
         then Tom's reservation expired and B-31 stays withdrawn from riders, waiting for a technician to reserve it again
        """
    )
    // @formatter:on
    @Test
    void e2_1_unused_technician_reservation_expires_and_bike_stays_withdrawn() {
        // given
        at("2026-11-08T08:00:00Z");
        var b31 = rentals.bikeAt(bike("B-31"), OLD_TOWN);
        rentals.withdraw(b31);
        var tom = "tom-" + suffix;
        at("2026-11-08T09:00:00Z");
        assertThat(rentals.reserveForService(b31, tom).status()).isEqualTo(201);

        // when
        reservationsCheckedAt("2026-11-08T09:30:01Z");

        // then
        var bike = rentals.bike(b31).body();
        assertThat(bike.availability()).isEqualTo("WITHDRAWN");
        assertThat(bike.hold()).isEqualTo("FREE");
        await().atMost(HAND_OFF).untilAsserted(() -> assertThat(rentals.published(ReservationExpired.class, event -> event.bikeId().equals(b31)))
            .containsExactly(new ReservationExpired(b31, "TECHNICIAN", tom, Instant.parse("2026-11-08T09:30:01Z"))));
        assertThat(rentals.reserveForService(b31, "tara-" + suffix).status()).isEqualTo(201);
    }

    // @formatter:off
    @DisplayName(
        """
         E2.2 given broken B-31 was reserved for Tom at 09:00,
         when the reservations are checked at exactly 09:30:00,
         then B-31 is still reserved for Tom
        """
    )
    // @formatter:on
    @Test
    void e2_2_technician_reservation_still_holds_at_exact_end() {
        // given
        at("2026-11-08T08:00:00Z");
        var b31 = rentals.bikeAt(bike("B-31"), OLD_TOWN);
        rentals.withdraw(b31);
        var tom = "tom-" + suffix;
        at("2026-11-08T09:00:00Z");
        rentals.reserveForService(b31, tom);

        // when
        reservationsCheckedAt("2026-11-08T09:30:00Z");

        // then
        assertThat(rentals.bike(b31).body().hold()).isEqualTo("RESERVED");
        assertThat(rentals.bike(b31).body().holderId()).isEqualTo(tom);
    }

    // @formatter:off
    @DisplayName(
        """
         E3.1 given B-17 was reserved for Anna at 10:00 and Anna unlocked it at 10:01, starting her ride,
         when the reservations are checked at 10:03,
         then nothing expires and Anna's ride on B-17 keeps running
        """
    )
    // @formatter:on
    @Test
    void e3_1_unlocked_reservation_does_not_expire() {
        // given
        at("2026-11-08T10:00:00Z");
        var anna = rider("anna");
        var b17 = bike("B-17");
        rentals.reserved(b17, CENTRAL, anna);
        at("2026-11-08T10:01:00Z");
        rentals.unlock(b17, anna);

        // when
        reservationsCheckedAt("2026-11-08T10:03:00Z");

        // then
        assertThat(rentals.activeRentalOf(anna).body().kind()).isEqualTo("SESSION");
        assertThat(rentals.published(ReservationExpired.class, event -> event.bikeId().equals(b17))).isEmpty();
    }

    // @formatter:off
    @DisplayName(
        """
         E3.2 given Anna's unlock of B-17 failed at 10:01, releasing her reservation,
         when the reservations are checked at 10:03,
         then nothing expires
        """
    )
    // @formatter:on
    @Test
    void e3_2_reservation_released_by_failed_unlock_does_not_expire() {
        // given
        at("2026-11-08T10:00:00Z");
        var anna = rider("anna");
        var b17 = bike("B-17");
        rentals.reserved(b17, CENTRAL, anna);
        locks.registerFaulty(b17);
        at("2026-11-08T10:01:00Z");
        rentals.unlock(b17, anna);

        // when
        reservationsCheckedAt("2026-11-08T10:03:00Z");

        // then
        assertThat(rentals.bike(b17).body().hold()).isEqualTo("FREE");
        assertThat(rentals.published(ReservationExpired.class, event -> event.bikeId().equals(b17))).isEmpty();
    }
}
