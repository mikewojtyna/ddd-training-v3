package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.velocity.common.VelocitySpringTest;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static pl.wojtyna.dddtrainingv3.velocity.rentals.RentalsDriver.CENTRAL;
import static pl.wojtyna.dddtrainingv3.velocity.rentals.RentalsDriver.OLD_TOWN;

/// Acceptance tests of spec `technician-reserves-broken-bike`: Fleet Maintenance → Rentals (Reserve Bike for service)
/// and Rentals → Fleet Maintenance (Reservation Expired, asserted with Awaitility). The expiry is triggered by
/// calling Rentals' Release Expired Reservations with the clock moved.
@VelocitySpringTest
@DisplayName("technician-reserves-broken-bike")
class TechnicianReservesBrokenBikeSpecTest extends FleetMaintenanceSpecTestBase {

    /// B-17 broken at Old Town and reserved for Tom from 09:00 until 09:30.
    private String reservedForTom(String bikeId, String tom) {
        at("2026-11-09T08:50:00Z");
        brokenAt(bikeId, OLD_TOWN, technician("ivy"));
        at("2026-11-09T09:00:00Z");
        assertThat(reserveBrokenBike(bikeId, tom).status()).isEqualTo(200);
        return bikeId;
    }

    @Test
    @DisplayName("E1.1 a technician reserves a broken bike for 30 minutes")
    void e1_1_technician_reserves_broken_bike_for_30_minutes() {
        // given
        var b17 = bike("B-17");
        var tom = technician("tom");
        at("2026-11-09T08:50:00Z");
        brokenAt(b17, OLD_TOWN, technician("ivy"));

        // when
        at("2026-11-09T09:00:00Z");
        var reply = reserveBrokenBike(b17, tom);

        // then
        assertThat(reply.status()).isEqualTo(200);
        assertThat(reply.body().reservedUntil()).isEqualTo(Instant.parse("2026-11-09T09:30:00Z"));
        var bike = brokenBike(b17).body();
        assertThat(bike.technicianId()).isEqualTo(tom);
        assertThat(bike.reservedUntil()).isEqualTo(Instant.parse("2026-11-09T09:30:00Z"));
        assertThat(bike.stationId()).isEqualTo(OLD_TOWN.stationId());
        var inRentals = rentals.bike(b17).body();
        assertThat(inRentals.hold()).isEqualTo("RESERVED");
        assertThat(inRentals.holderType()).isEqualTo("TECHNICIAN");
        assertThat(inRentals.holderId()).isEqualTo(tom);
    }

    @Test
    @DisplayName("E1.2 a technician reserves a broken bike whose position isn't known, without giving a position")
    void e1_2_technician_reserves_bike_with_unknown_position() {
        // given: B-22 wasn't returned, so it is broken (not returned) with no known position
        var b22 = bike("B-22");
        var ben = rider("ben");
        at("2026-11-08T20:00:00Z");
        rentals.riding(b22, CENTRAL, ben);
        at("2026-11-09T08:30:00Z");
        overdueSessionsAreEnded();
        await().atMost(HAND_OFF).untilAsserted(() -> assertThat(brokenBike(b22).body().faultSource()).isEqualTo("NOT_RETURNED"));
        assertThat(rentals.bike(b22).body().latitude()).isNull();

        // when
        at("2026-11-09T09:00:00Z");
        var reply = reserveBrokenBike(b22, technician("tom"));

        // then
        assertThat(reply.status()).isEqualTo(200);
        assertThat(reply.body().reservedUntil()).isEqualTo(Instant.parse("2026-11-09T09:30:00Z"));
    }

    @Test
    @DisplayName("E1.3 a technician holds several reservations")
    void e1_3_technician_holds_several_reservations() {
        // given
        var tom = technician("tom");
        var b17 = reservedForTom(bike("B-17"), tom);
        var b22 = bike("B-22");
        brokenAt(b22, CENTRAL, technician("ivy"));

        // when
        at("2026-11-09T09:05:00Z");
        var reply = reserveBrokenBike(b22, tom);

        // then
        assertThat(reply.status()).isEqualTo(200);
        assertThat(reply.body().reservedUntil()).isEqualTo(Instant.parse("2026-11-09T09:35:00Z"));
        assertThat(brokenBike(b17).body().technicianId()).isEqualTo(tom);
        assertThat(rentals.bike(b17).body().holderId()).isEqualTo(tom);
    }

    @Test
    @DisplayName("E2.1 a technician can't reserve a bike that isn't broken")
    void e2_1_technician_cannot_reserve_bike_that_is_not_broken() {
        // given
        var b40 = rentals.bikeAt(bike("B-40"), CENTRAL);
        at("2026-11-09T09:00:00Z");

        // when
        var reply = reserveBrokenBike(b40, technician("tom"));

        // then
        assertThat(reply.status()).isEqualTo(409);
        assertThat(reply.error()).isEqualTo("NotBroken");
        var inRentals = rentals.bike(b40).body();
        assertThat(inRentals.availability()).isEqualTo("AVAILABLE");
        assertThat(inRentals.hold()).isEqualTo("FREE");
    }

    @Test
    @DisplayName("E3.1 a second technician can't reserve a reserved broken bike")
    void e3_1_second_technician_cannot_reserve_reserved_bike() {
        // given
        var tom = technician("tom");
        var b17 = reservedForTom(bike("B-17"), tom);

        // when
        at("2026-11-09T09:10:00Z");
        var reply = reserveBrokenBike(b17, technician("tara"));

        // then
        assertThat(reply.status()).isEqualTo(409);
        assertThat(reply.error()).isEqualTo("AlreadyReserved");
        assertThat(brokenBike(b17).body().technicianId()).isEqualTo(tom);
        assertThat(rentals.bike(b17).body().holderId()).isEqualTo(tom);
    }

    @Test
    @DisplayName("E4.1 an unused reservation expires and the bike stays broken, waiting for any technician")
    void e4_1_unused_reservation_expires_and_bike_stays_broken() {
        // given
        var b17 = reservedForTom(bike("B-17"), technician("tom"));

        // when
        at("2026-11-09T09:30:01Z");
        reservationsAreChecked();

        // then
        await().atMost(HAND_OFF).untilAsserted(() -> {
            var bike = brokenBike(b17).body();
            assertThat(bike.status()).isEqualTo("BROKEN");
            assertThat(bike.technicianId()).isNull();
            assertThat(bike.reservedUntil()).isNull();
        });
        var inRentals = rentals.bike(b17).body();
        assertThat(inRentals.availability()).isEqualTo("WITHDRAWN");
        assertThat(inRentals.hold()).isEqualTo("FREE");
        assertThat(reserveBrokenBike(b17, technician("tara")).status()).isEqualTo(200);
    }

    @Test
    @DisplayName("E4.2 the reservation is still held before its end")
    void e4_2_reservation_still_held_before_end() {
        // given
        var tom = technician("tom");
        var b17 = reservedForTom(bike("B-17"), tom);

        // when
        at("2026-11-09T09:29:00Z");
        reservationsAreChecked();

        // then
        assertThat(rentals.bike(b17).body().holderId()).isEqualTo(tom);
        assertThat(brokenBike(b17).body().technicianId()).isEqualTo(tom);
    }

    @Test
    @DisplayName("E4.3 the reservation is still held at its exact end (DE-81)")
    void e4_3_reservation_still_held_at_exact_end() {
        // given
        var tom = technician("tom");
        var b17 = reservedForTom(bike("B-17"), tom);

        // when
        at("2026-11-09T09:30:00Z");
        reservationsAreChecked();

        // then
        assertThat(rentals.bike(b17).body().holderId()).isEqualTo(tom);
        assertThat(brokenBike(b17).body().technicianId()).isEqualTo(tom);
    }

    @Test
    @DisplayName("derived: the broken bikes list shows broken bikes with where they are")
    void broken_bikes_list_shows_where_they_are() {
        // given
        var b17 = bike("B-17");
        at("2026-11-09T08:50:00Z");
        brokenAt(b17, OLD_TOWN, technician("ivy"));

        // when
        var list = brokenBikes();

        // then
        assertThat(list).filteredOn(bike -> bike.bikeId().equals(b17)).singleElement()
                        .satisfies(bike -> {
                            assertThat(bike.status()).isEqualTo("BROKEN");
                            assertThat(bike.stationId()).isEqualTo(OLD_TOWN.stationId());
                        });
    }
}
