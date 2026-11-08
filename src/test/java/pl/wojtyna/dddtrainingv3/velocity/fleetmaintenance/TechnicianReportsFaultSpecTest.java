package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.velocity.common.VelocitySpringTest;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.events.BikeWithdrawn;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static pl.wojtyna.dddtrainingv3.velocity.rentals.RentalsDriver.CENTRAL;

/// Acceptance tests of spec `technician-reports-fault`, over Fleet Maintenance's and Rentals' REST resources; the
/// Fleet Maintenance → Rentals withdrawal is asserted with Awaitility.
@VelocitySpringTest
@DisplayName("technician-reports-fault")
class TechnicianReportsFaultSpecTest extends FleetMaintenanceSpecTestBase {

    @Test
    @DisplayName("E1.1 a technician's fault report makes the bike broken and withdrawn")
    void e1_1_fault_report_makes_bike_broken_and_withdrawn() {
        // given
        var b22 = rentals.bikeAt(bike("B-22"), CENTRAL);
        var tom = technician("tom");
        at("2026-11-09T09:00:00Z");

        // when
        var reply = reportFault(b22, tom, "loose handlebar");

        // then
        assertThat(reply.status()).isEqualTo(200);
        var bike = brokenBike(b22).body();
        assertThat(bike.status()).isEqualTo("BROKEN");
        assertThat(bike.faultSource()).isEqualTo("TECHNICIAN");
        assertThat(bike.faultDescription()).isEqualTo("loose handlebar");
        await().atMost(HAND_OFF).untilAsserted(() -> {
            assertThat(rentals.bike(b22).body().availability()).isEqualTo("WITHDRAWN");
            assertThat(rentals.published(BikeWithdrawn.class, event -> event.bikeId().equals(b22)))
                .extracting(BikeWithdrawn::reason).containsExactly("REQUESTED");
        });
    }

    @Test
    @DisplayName("E1.2 a rider can't reserve a bike a technician reported broken")
    void e1_2_rider_cannot_reserve_reported_bike() {
        // given
        var b22 = bike("B-22");
        at("2026-11-09T09:00:00Z");
        brokenAt(b22, CENTRAL, technician("tom"));
        var anna = rider("anna");

        // when
        at("2026-11-09T09:05:00Z");
        var reply = rentals.reserve(b22, anna, CENTRAL.northBy(10));

        // then
        assertThat(reply.status()).isEqualTo(409);
    }

    @Test
    @DisplayName("E1.3 a fault report during a ride breaks and withdraws the bike, but the ride goes on")
    void e1_3_fault_report_during_ride_lets_ride_go_on() {
        // given: Anna's session on B-22 started at 08:50
        var b22 = bike("B-22");
        var anna = rider("anna");
        at("2026-11-09T08:50:00Z");
        rentals.riding(b22, CENTRAL, anna);

        // when
        at("2026-11-09T09:00:00Z");
        var reply = reportFault(b22, technician("tom"), "cracked rear light");

        // then
        assertThat(reply.status()).isEqualTo(200);
        assertThat(brokenBike(b22).body().status()).isEqualTo("BROKEN");
        await().atMost(HAND_OFF).untilAsserted(() -> assertThat(rentals.bike(b22).body().availability()).isEqualTo("WITHDRAWN"));
        assertThat(rentals.activeRentalOf(anna).body().kind()).isEqualTo("SESSION");
    }

    @Test
    @DisplayName("E2.1 a fault report cancels a rider's reservation")
    void e2_1_fault_report_cancels_riders_reservation() {
        // given: B-22 reserved for Anna until 09:02
        var b22 = bike("B-22");
        var anna = rider("anna");
        at("2026-11-09T09:00:00Z");
        rentals.reserved(b22, CENTRAL, anna);

        // when
        at("2026-11-09T09:01:00Z");
        var reply = reportFault(b22, technician("tom"), "worn brake pads");

        // then
        assertThat(reply.status()).isEqualTo(200);
        assertThat(brokenBike(b22).body().status()).isEqualTo("BROKEN");
        await().atMost(HAND_OFF).untilAsserted(() -> {
            var bike = rentals.bike(b22).body();
            assertThat(bike.availability()).isEqualTo("WITHDRAWN");
            assertThat(bike.hold()).isEqualTo("FREE");
        });
        assertThat(rentals.activeRentalOf(anna).status()).isEqualTo(404);
    }

    @Test
    @DisplayName("E3.1 a fault report on a broken bike changes nothing")
    void e3_1_fault_report_on_broken_bike_changes_nothing() {
        // given: B-17 broken since 08:00 (lock failed)
        var b17 = bike("B-17");
        var anna = rider("anna");
        at("2026-11-09T07:30:00Z");
        rentals.riding(b17, CENTRAL, anna);
        locks.registerFaulty(b17);
        at("2026-11-09T08:00:00Z");
        rentals.returnAtStation(b17, anna, CENTRAL.stationId());
        locks.registerRepaired(b17);
        awaitBroken(b17);

        // when
        at("2026-11-09T09:00:00Z");
        var reply = reportFault(b17, technician("tom"), "bent mudguard");

        // then
        assertThat(reply.status()).isEqualTo(200);
        var bike = brokenBike(b17).body();
        assertThat(bike.faultSource()).isEqualTo("LOCK_FAILED");
        assertThat(bike.faultDescription()).isNull();
        await().during(Duration.ofSeconds(1)).atMost(HAND_OFF).untilAsserted(() ->
            assertThat(rentals.published(BikeWithdrawn.class, event -> event.bikeId().equals(b17)))
                .extracting(BikeWithdrawn::reason).containsExactly("LOCK_FAILED"));
    }

    @Test
    @DisplayName("derived (plan D5): a fault report on a bike unknown to the fleet is rejected and nothing is recorded")
    void fault_report_on_unknown_bike_is_rejected() {
        // when
        var reply = reportFault(bike("B-404"), technician("tom"), "no such bike");

        // then
        assertThat(reply.status()).isEqualTo(404);
        assertThat(reply.error()).isEqualTo("UnknownBike");
        assertThat(brokenBike(bike("B-404")).status()).isEqualTo(404);
    }

    @Test
    @DisplayName("derived: a fault report without a description is malformed")
    void fault_report_without_description_is_bad_request() {
        // when
        var reply = reportFault(rentals.bikeAt(bike("B-22"), CENTRAL), technician("tom"), " ");

        // then
        assertThat(reply.status()).isEqualTo(400);
    }
}
