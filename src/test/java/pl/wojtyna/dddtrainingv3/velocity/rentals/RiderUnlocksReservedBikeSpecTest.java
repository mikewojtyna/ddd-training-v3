package pl.wojtyna.dddtrainingv3.velocity.rentals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.velocity.common.VelocitySpringTest;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.events.BikeUnlocked;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.events.UnlockFailed;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static pl.wojtyna.dddtrainingv3.velocity.rentals.RentalsDriver.CENTRAL;

/// Acceptance tests of `docs/specs/rider-unlocks-reserved-bike.md`. The lock hardware is `SimulatedBikeLocks`; E4.1
/// blocks the rider through Payments → Accounts (asynchronous hand-off, awaited) before the unlock.
@VelocitySpringTest
@DisplayName("Spec rider-unlocks-reserved-bike")
class RiderUnlocksReservedBikeSpecTest extends RentalsSpecTestBase {

    // @formatter:off
    @DisplayName(
        """
         E1.1 given B-17 at station Central is reserved for Anna from 10:00 until 10:02,
         when Anna unlocks B-17 at 10:01 and the lock opens,
         then a rental session starts for Anna on B-17 at 10:01 and the reservation is used up
        """
    )
    // @formatter:on
    @Test
    void e1_1_holder_unlocks_and_session_starts() {
        // given
        at("2026-11-08T10:00:00Z");
        var anna = rider("anna");
        var b17 = bike("B-17");
        rentals.reserved(b17, CENTRAL, anna);

        // when
        at("2026-11-08T10:01:00Z");
        var unlock = rentals.unlock(b17, anna);

        // then
        assertThat(unlock.status()).isEqualTo(200);
        assertThat(unlock.body().outcome()).isEqualTo("UNLOCKED");
        assertThat(unlock.body().sessionId()).isNotBlank();
        var active = rentals.activeRentalOf(anna).body();
        assertThat(active.kind()).isEqualTo("SESSION");
        assertThat(active.sessionId()).isEqualTo(unlock.body().sessionId());
        assertThat(active.since()).isEqualTo(Instant.parse("2026-11-08T10:01:00Z"));
        assertThat(rentals.bike(b17).body().hold()).isEqualTo("UNLOCKED");
        assertThat(rentals.published(BikeUnlocked.class, event -> event.bikeId().equals(b17))).isEmpty();
    }

    // @formatter:off
    @DisplayName(
        """
         E1.2 given B-17 is reserved for Anna from 10:00 until 10:02,
         when Anna unlocks B-17 at exactly 10:02:00 and the lock opens,
         then a rental session starts for Anna on B-17 at 10:02
        """
    )
    // @formatter:on
    @Test
    void e1_2_unlock_at_exact_end_of_reservation_starts_session() {
        // given
        at("2026-11-08T10:00:00Z");
        var anna = rider("anna");
        var b17 = bike("B-17");
        rentals.reserved(b17, CENTRAL, anna);

        // when
        at("2026-11-08T10:02:00Z");
        var unlock = rentals.unlock(b17, anna);

        // then
        assertThat(unlock.body().outcome()).isEqualTo("UNLOCKED");
        assertThat(rentals.activeRentalOf(anna).body().since()).isEqualTo(Instant.parse("2026-11-08T10:02:00Z"));
    }

    // @formatter:off
    @DisplayName(
        """
         E2.1 given B-17 at Central is reserved for Anna until 10:02,
         when Anna unlocks B-17 at 10:01 and the lock doesn't open,
         then no rental session starts, Anna holds no reservation and B-17 is available to anyone again (Unlock Failed)
        """
    )
    // @formatter:on
    @Test
    void e2_1_failed_unlock_releases_reservation_and_bike_is_available() {
        // given
        at("2026-11-08T10:00:00Z");
        var anna = rider("anna");
        var b17 = bike("B-17");
        rentals.reserved(b17, CENTRAL, anna);
        locks.registerFaulty(b17);

        // when
        at("2026-11-08T10:01:00Z");
        var unlock = rentals.unlock(b17, anna);

        // then
        assertThat(unlock.status()).isEqualTo(200);
        assertThat(unlock.body().outcome()).isEqualTo("UNLOCK_FAILED");
        assertThat(unlock.body().sessionId()).isNull();
        assertThat(rentals.activeRentalOf(anna).status()).isEqualTo(404);
        assertThat(rentals.bike(b17).body().hold()).isEqualTo("FREE");
        assertThat(rentals.bike(b17).body().availability()).isEqualTo("AVAILABLE");
        await().atMost(HAND_OFF).untilAsserted(() -> assertThat(rentals.published(UnlockFailed.class, event -> event.bikeId().equals(b17)))
            .containsExactly(new UnlockFailed(b17, "RIDER", anna, Instant.parse("2026-11-08T10:01:00Z"))));
    }

    // @formatter:off
    @DisplayName(
        """
         E2.2 given Anna's unlock of B-17 failed at 10:01,
         when Anna, standing 10 m from available B-22, reserves B-22 at 10:01:30,
         then B-22 is reserved for Anna
        """
    )
    // @formatter:on
    @Test
    void e2_2_after_failed_unlock_rider_reserves_another_bike() {
        // given
        at("2026-11-08T10:00:00Z");
        var anna = rider("anna");
        var b17 = bike("B-17");
        rentals.reserved(b17, CENTRAL, anna);
        locks.registerFaulty(b17);
        at("2026-11-08T10:01:00Z");
        rentals.unlock(b17, anna);
        var b22 = rentals.bikeAt(bike("B-22"), CENTRAL);

        // when
        at("2026-11-08T10:01:30Z");
        var reservation = rentals.reserve(b22, anna, CENTRAL.northBy(10));

        // then
        assertThat(reservation.status()).isEqualTo(201);
        assertThat(rentals.activeRentalOf(anna).body().bikeId()).isEqualTo(b22);
    }

    // @formatter:off
    @DisplayName(
        """
         E3.1 given B-17 is reserved for Anna until 10:02,
         when rider Ben unlocks B-17 at 10:01,
         then the unlock is rejected, the lock stays closed and B-17 stays reserved for Anna
        """
    )
    // @formatter:on
    @Test
    void e3_1_other_rider_cannot_unlock_reserved_bike() {
        // given
        at("2026-11-08T10:00:00Z");
        var anna = rider("anna");
        var ben = rider("ben");
        var b17 = bike("B-17");
        rentals.reserved(b17, CENTRAL, anna);

        // when
        at("2026-11-08T10:01:00Z");
        var unlock = rentals.unlock(b17, ben);

        // then
        assertThat(unlock.status()).isEqualTo(409);
        assertThat(unlock.error()).isEqualTo("NotReservationHolder");
        assertThat(rentals.bike(b17).body().hold()).isEqualTo("RESERVED");
        assertThat(rentals.bike(b17).body().holderId()).isEqualTo(anna);
    }

    // @formatter:off
    @DisplayName(
        """
         E3.2 given B-22 is available and Anna holds no reservation,
         when Anna unlocks B-22,
         then the unlock is rejected and the lock stays closed
        """
    )
    // @formatter:on
    @Test
    void e3_2_unlock_without_reservation_is_rejected() {
        // given
        at("2026-11-08T10:00:00Z");
        var anna = rider("anna");
        var b22 = rentals.bikeAt(bike("B-22"), CENTRAL);

        // when
        var unlock = rentals.unlock(b22, anna);

        // then
        assertThat(unlock.status()).isEqualTo(409);
        assertThat(unlock.error()).isEqualTo("NotReservationHolder");
        assertThat(rentals.bike(b22).body().hold()).isEqualTo("FREE");
    }

    // @formatter:off
    @DisplayName(
        """
         E3.3 given B-17 was reserved for Anna until 10:02 and the reservation hasn't been released yet,
         when Anna unlocks B-17 at 10:02:30,
         then the unlock is rejected and the lock stays closed
        """
    )
    // @formatter:on
    @Test
    void e3_3_unlock_after_reservation_period_is_rejected() {
        // given
        at("2026-11-08T10:00:00Z");
        var anna = rider("anna");
        var b17 = bike("B-17");
        rentals.reserved(b17, CENTRAL, anna);

        // when
        at("2026-11-08T10:02:30Z");
        var unlock = rentals.unlock(b17, anna);

        // then
        assertThat(unlock.status()).isEqualTo(409);
        assertThat(unlock.error()).isEqualTo("NotReservationHolder");
        assertThat(rentals.activeRentalOf(anna).status()).isEqualTo(404);
    }

    // @formatter:off
    @DisplayName(
        """
         E4.1 given eligible Anna reserved B-17 at 10:00, until 10:02, and her account was blocked at 10:01 because her card became invalid,
         when Anna unlocks B-17 at 10:01:30,
         then the unlock is rejected, the lock stays closed, no rental session starts and B-17 stays reserved for Anna until 10:02
        """
    )
    // @formatter:on
    @Test
    void e4_1_rider_blocked_during_reservation_cannot_unlock() {
        // given
        at("2026-11-08T10:00:00Z");
        var annaSuffix = "anna-" + suffix;
        var anna = rentals.activatedRider(annaSuffix);
        var b17 = bike("B-17");
        rentals.reserved(b17, CENTRAL, anna);
        at("2026-11-08T10:01:00Z");
        assertThat(rentals.reportCardLost(annaSuffix)).isEqualTo(200);
        await().atMost(HAND_OFF).until(() -> !rentals.isEligibleToRent(anna));

        // when
        at("2026-11-08T10:01:30Z");
        var unlock = rentals.unlock(b17, anna);

        // then
        assertThat(unlock.status()).isEqualTo(409);
        assertThat(unlock.error()).isEqualTo("RiderNotEligible");
        var active = rentals.activeRentalOf(anna).body();
        assertThat(active.kind()).isEqualTo("RESERVATION");
        assertThat(active.expiresAt()).isEqualTo(Instant.parse("2026-11-08T10:02:00Z"));
    }
}
