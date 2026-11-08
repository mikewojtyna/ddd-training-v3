package pl.wojtyna.dddtrainingv3.velocity.rentals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.velocity.common.VelocitySpringTest;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.events.BikeWithdrawn;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static pl.wojtyna.dddtrainingv3.velocity.rentals.RentalsDriver.CENTRAL;

/// Acceptance tests of `docs/specs/fleet-operations-withdraws-bike-signalling-fault.md`. The bike's telemetry calls
/// `POST /rentals/bikes/{bikeId}/fault-signals`; the published `BikeWithdrawn` (for Fleet Maintenance) is awaited.
@VelocitySpringTest
@DisplayName("Spec fleet-operations-withdraws-bike-signalling-fault")
class FleetOperationsWithdrawsBikeSignallingFaultSpecTest extends RentalsSpecTestBase {

    // @formatter:off
    @DisplayName(
        """
         E1.1 given B-17 is available at station Central and nobody holds it,
         when B-17 signals fault "BRAKE_SENSOR" at 09:00,
         then B-17 is withdrawn with reason fault signalled (Bike Withdrawn)
        """
    )
    // @formatter:on
    @Test
    void e1_1_free_bike_signalling_fault_is_withdrawn() {
        // given
        at("2026-11-08T08:00:00Z");
        var b17 = rentals.bikeAt(bike("B-17"), CENTRAL);

        // when
        at("2026-11-08T09:00:00Z");
        var signal = rentals.signalFault(b17, "BRAKE_SENSOR");

        // then
        assertThat(signal.status()).isEqualTo(202);
        assertThat(rentals.bike(b17).body().availability()).isEqualTo("WITHDRAWN");
        await().atMost(HAND_OFF).untilAsserted(() -> assertThat(rentals.published(BikeWithdrawn.class, event -> event.bikeId().equals(b17)))
            .containsExactly(new BikeWithdrawn(b17, "FAULT_SIGNALLED", Instant.parse("2026-11-08T09:00:00Z"), "BRAKE_SENSOR")));
    }

    // @formatter:off
    @DisplayName(
        """
         E1.2 given B-17 signalled fault "BRAKE_SENSOR" at 09:00 and was withdrawn,
         when rider Anna, standing 10 m from B-17, reserves it at 09:05,
         then the reservation is rejected
        """
    )
    // @formatter:on
    @Test
    void e1_2_withdrawn_bike_cannot_be_reserved_by_rider() {
        // given
        at("2026-11-08T09:00:00Z");
        var anna = rider("anna");
        var b17 = rentals.bikeAt(bike("B-17"), CENTRAL);
        rentals.signalFault(b17, "BRAKE_SENSOR");

        // when
        at("2026-11-08T09:05:00Z");
        var reservation = rentals.reserve(b17, anna, CENTRAL.northBy(10));

        // then
        assertThat(reservation.status()).isEqualTo(409);
        assertThat(reservation.error()).isEqualTo("BikeNotAvailable");
    }

    // @formatter:off
    @DisplayName(
        """
         E2.1 given B-17 at Central is reserved for Anna from 10:00 until 10:02,
         when B-17 signals fault "BRAKE_SENSOR" at 10:01,
         then Anna's reservation is cancelled, Anna may reserve another bike, and B-17 is withdrawn with reason fault signalled
        """
    )
    // @formatter:on
    @Test
    void e2_1_reservation_on_faulty_bike_is_cancelled_and_bike_withdrawn() {
        // given
        at("2026-11-08T10:00:00Z");
        var anna = rider("anna");
        var b17 = bike("B-17");
        rentals.reserved(b17, CENTRAL, anna);
        var b22 = rentals.bikeAt(bike("B-22"), CENTRAL);

        // when
        at("2026-11-08T10:01:00Z");
        rentals.signalFault(b17, "BRAKE_SENSOR");

        // then
        assertThat(rentals.activeRentalOf(anna).status()).isEqualTo(404);
        assertThat(rentals.bike(b17).body().availability()).isEqualTo("WITHDRAWN");
        assertThat(rentals.bike(b17).body().hold()).isEqualTo("FREE");
        await().atMost(HAND_OFF).untilAsserted(() -> assertThat(rentals.published(BikeWithdrawn.class, event -> event.bikeId().equals(b17)))
            .containsExactly(new BikeWithdrawn(b17, "FAULT_SIGNALLED", Instant.parse("2026-11-08T10:01:00Z"), "BRAKE_SENSOR")));
        assertThat(rentals.reserve(b22, anna, CENTRAL.northBy(10)).status()).isEqualTo(201);
    }

    // @formatter:off
    @DisplayName(
        """
         E3.1 given Anna's rental session on B-17 started at 10:01,
         when B-17 signals fault "BRAKE_SENSOR" at 10:15,
         then Anna's session keeps running and B-17 isn't withdrawn yet
        """
    )
    // @formatter:on
    @Test
    void e3_1_fault_during_ride_does_not_interrupt_it() {
        // given
        at("2026-11-08T10:01:00Z");
        var anna = rider("anna");
        var b17 = bike("B-17");
        rentals.riding(b17, CENTRAL, anna);

        // when
        at("2026-11-08T10:15:00Z");
        var signal = rentals.signalFault(b17, "BRAKE_SENSOR");

        // then
        assertThat(signal.status()).isEqualTo(202);
        assertThat(rentals.activeRentalOf(anna).body().kind()).isEqualTo("SESSION");
        assertThat(rentals.bike(b17).body().availability()).isEqualTo("AVAILABLE");
        await().during(Duration.ofMillis(300)).atMost(HAND_OFF)
               .untilAsserted(() -> assertThat(rentals.published(BikeWithdrawn.class, event -> event.bikeId().equals(b17))).isEmpty());
    }

    // @formatter:off
    @DisplayName(
        """
         E3.2 given Anna's rental session on B-17 started at 10:01 and B-17 signalled fault "BRAKE_SENSOR" at 10:15,
         when Anna returns B-17 at station Old Town at 10:40 and the lock closes,
         then the session ended at 10:40 at Old Town with end reason station return and B-17 is withdrawn with reason fault signalled
        """
    )
    // @formatter:on
    @Test
    void e3_2_bike_with_fault_during_ride_is_withdrawn_at_return() {
        // given
        at("2026-11-08T10:01:00Z");
        var anna = rider("anna");
        var b17 = bike("B-17");
        rentals.riding(b17, CENTRAL, anna);
        at("2026-11-08T10:15:00Z");
        rentals.signalFault(b17, "BRAKE_SENSOR");

        // when
        at("2026-11-08T10:40:00Z");
        var returned = rentals.returnAtStation(b17, anna, "Old Town");

        // then
        assertThat(returned.body().endReason()).isEqualTo("STATION_RETURN");
        assertThat(rentals.bike(b17).body().availability()).isEqualTo("WITHDRAWN");
        await().atMost(HAND_OFF).untilAsserted(() -> assertThat(rentals.published(BikeWithdrawn.class, event -> event.bikeId().equals(b17)))
            .containsExactly(new BikeWithdrawn(b17, "FAULT_SIGNALLED", Instant.parse("2026-11-08T10:40:00Z"), "BRAKE_SENSOR")));
    }

    // @formatter:off
    @DisplayName(
        """
         E4.1 given B-17 was withdrawn at 08:00 with reason lock failed,
         when B-17 signals fault "BRAKE_SENSOR" at 09:00,
         then nothing changes: B-17 stays withdrawn and no second Bike Withdrawn happens
        """
    )
    // @formatter:on
    @Test
    void e4_1_withdrawn_bike_signalling_fault_stays_withdrawn_once() {
        // given
        at("2026-11-08T07:30:00Z");
        var anna = rider("anna");
        var b17 = bike("B-17");
        rentals.riding(b17, CENTRAL, anna);
        locks.registerFaulty(b17);
        at("2026-11-08T08:00:00Z");
        rentals.returnAtStation(b17, anna, "Central");

        // when
        at("2026-11-08T09:00:00Z");
        rentals.signalFault(b17, "BRAKE_SENSOR");

        // then
        assertThat(rentals.bike(b17).body().availability()).isEqualTo("WITHDRAWN");
        await().during(Duration.ofMillis(300)).atMost(HAND_OFF)
               .untilAsserted(() -> assertThat(rentals.published(BikeWithdrawn.class, event -> event.bikeId().equals(b17)))
                   .containsExactly(new BikeWithdrawn(b17, "LOCK_FAILED", Instant.parse("2026-11-08T08:00:00Z"), null)));
    }
}
