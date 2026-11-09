package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.velocity.common.VelocitySpringTest;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.events.BikeUnlocked;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.events.RentalSessionEnded;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static pl.wojtyna.dddtrainingv3.velocity.rentals.RentalsDriver.OLD_TOWN;

/// Acceptance tests of spec `technician-starts-maintenance`: Fleet Maintenance → Rentals (Unlock Bike) and Rentals →
/// Fleet Maintenance (Bike Unlocked, Unlock Failed, Reservation Expired), asserted with Awaitility. Lock failures come
/// from Rentals' `SimulatedBikeLocks`.
@VelocitySpringTest
@DisplayName("technician-starts-maintenance")
class TechnicianStartsMaintenanceSpecTest extends FleetMaintenanceSpecTestBase {

    /// B-17 broken and reserved for Tom from 09:00 until 09:30.
    private String reservedForTom(String bikeId, String tom) {
        at("2026-11-09T08:50:00Z");
        brokenAt(bikeId, OLD_TOWN, technician("ivy"));
        at("2026-11-09T09:00:00Z");
        assertThat(reserveBrokenBike(bikeId, tom).status()).isEqualTo(200);
        return bikeId;
    }

    @Test
    @DisplayName("E1.1 a technician's unlock of the broken bike they reserved starts maintenance")
    void e1_1_technician_unlock_starts_maintenance() {
        // given
        var tom = technician("tom");
        var b17 = reservedForTom(bike("B-17"), tom);

        // when
        at("2026-11-09T09:20:00Z");
        var reply = unlockBrokenBike(b17, tom);

        // then
        assertThat(reply.status()).isEqualTo(200);
        assertThat(reply.body().outcome()).isEqualTo("UNLOCKED");
        await().atMost(HAND_OFF).untilAsserted(() -> {
            var bike = brokenBike(b17).body();
            assertThat(bike.status()).isEqualTo("UNDER_MAINTENANCE");
            assertThat(bike.technicianId()).isEqualTo(tom);
        });
        assertThat(rentals.published(BikeUnlocked.class, event -> event.bikeId().equals(b17)))
            .singleElement().satisfies(event -> {
                assertThat(event.holderType()).isEqualTo("TECHNICIAN");
                assertThat(event.unlockedAt()).isEqualTo(java.time.Instant.parse("2026-11-09T09:20:00Z"));
            });
    }

    @Test
    @DisplayName("E2.1 maintenance starts no rental session and nothing is priced or charged")
    void e2_1_maintenance_starts_no_rental_session_and_no_charge() {
        // given: Tom unlocked B-17 at 09:20 and maintenance started
        var tom = technician("tom");
        var b17 = reservedForTom(bike("B-17"), tom);
        at("2026-11-09T09:20:00Z");
        unlockBrokenBike(b17, tom);
        await().atMost(HAND_OFF).untilAsserted(() -> assertThat(brokenBike(b17).body().status()).isEqualTo("UNDER_MAINTENANCE"));

        // when: Tom works on B-17 until 11:00
        at("2026-11-09T11:00:00Z");

        // then
        var inRentals = rentals.bike(b17).body();
        assertThat(inRentals.hold()).isEqualTo("UNLOCKED");
        assertThat(inRentals.holderType()).isEqualTo("TECHNICIAN");
        assertThat(rentals.activeRentalOf(tom).status()).isEqualTo(404);
        assertThat(rentals.published(RentalSessionEnded.class, event -> event.bikeId().equals(b17))).isEmpty();
    }

    @Test
    @DisplayName("E3.1 another technician can't unlock a bike reserved for Tom")
    void e3_1_other_technician_cannot_unlock() {
        // given
        var tom = technician("tom");
        var b17 = reservedForTom(bike("B-17"), tom);

        // when
        at("2026-11-09T09:20:00Z");
        var reply = unlockBrokenBike(b17, technician("tara"));

        // then
        assertThat(reply.status()).isEqualTo(409);
        assertThat(reply.error()).isEqualTo("NotYourReservation");
        assertThat(rentals.bike(b17).body().hold()).isEqualTo("RESERVED");
        assertThat(rentals.bike(b17).body().holderId()).isEqualTo(tom);
        assertThat(brokenBike(b17).body().status()).isEqualTo("BROKEN");
    }

    @Test
    @DisplayName("E4.1 a failed unlock releases the reservation and the bike stays broken")
    void e4_1_failed_unlock_releases_reservation_bike_stays_broken() {
        // given
        var tom = technician("tom");
        var b17 = reservedForTom(bike("B-17"), tom);
        locks.registerFaulty(b17);

        // when
        at("2026-11-09T09:20:00Z");
        var reply = unlockBrokenBike(b17, tom);

        // then
        locks.registerRepaired(b17);
        assertThat(reply.status()).isEqualTo(200);
        assertThat(reply.body().outcome()).isEqualTo("UNLOCK_FAILED");
        await().atMost(HAND_OFF).untilAsserted(() -> {
            var bike = brokenBike(b17).body();
            assertThat(bike.status()).isEqualTo("BROKEN");
            assertThat(bike.technicianId()).isNull();
        });
        var inRentals = rentals.bike(b17).body();
        assertThat(inRentals.availability()).isEqualTo("WITHDRAWN");
        assertThat(inRentals.hold()).isEqualTo("FREE");
        assertThat(reserveBrokenBike(b17, technician("tara")).status()).isEqualTo(200);
    }
}
