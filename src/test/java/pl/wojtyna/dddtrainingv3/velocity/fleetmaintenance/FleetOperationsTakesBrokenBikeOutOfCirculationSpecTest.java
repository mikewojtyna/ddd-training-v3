package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.velocity.common.VelocitySpringTest;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.events.BikeWithdrawn;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static pl.wojtyna.dddtrainingv3.velocity.rentals.RentalsDriver.CENTRAL;
import static pl.wojtyna.dddtrainingv3.velocity.rentals.RentalsDriver.OLD_TOWN;

/// Acceptance tests of spec `fleet-operations-takes-broken-bike-out-of-circulation`: Rentals → Fleet Maintenance
/// (Bike Withdrawn), Customer Support → Fleet Maintenance (Fleet Issue Reported), Fleet Maintenance → Rentals
/// (Withdraw Bike), all asserted with Awaitility.
@VelocitySpringTest
@DisplayName("fleet-operations-takes-broken-bike-out-of-circulation")
class FleetOperationsTakesBrokenBikeOutOfCirculationSpecTest extends FleetMaintenanceSpecTestBase {

    /// Anna returns the bike at Old Town at 10:40 and the lock doesn't close: Rentals withdraws it (lock failed).
    private void lockFailedAtReturn(String bike, String day) {
        var anna = rider("anna");
        at(day + "T10:00:00Z");
        rentals.riding(bike, OLD_TOWN, anna);
        locks.registerFaulty(bike);
        at(day + "T10:40:00Z");
        assertThat(rentals.returnAtStation(bike, anna, OLD_TOWN.stationId()).body().endReason()).isEqualTo("LOCK_FAILURE");
        locks.registerRepaired(bike);
    }

    @Test
    @DisplayName("E1.1 a bike withdrawn for a failed lock becomes broken with fault source lock failed")
    void e1_1_bike_withdrawn_for_failed_lock_becomes_broken() {
        // given
        var b17 = bike("B-17");

        // when
        lockFailedAtReturn(b17, "2026-11-09");

        // then
        await().atMost(HAND_OFF).untilAsserted(() -> {
            var bike = brokenBike(b17).body();
            assertThat(bike).isNotNull();
            assertThat(bike.status()).isEqualTo("BROKEN");
            assertThat(bike.faultSource()).isEqualTo("LOCK_FAILED");
            assertThat(bike.reportedAt()).isEqualTo(Instant.parse("2026-11-09T10:40:00Z"));
        });
    }

    @Test
    @DisplayName("E1.2 a bike not returned within the maximum rental time becomes broken with fault source not returned")
    void e1_2_bike_not_returned_becomes_broken() {
        // given: Ben's session on B-22 started at 08:00
        var b22 = bike("B-22");
        var ben = rider("ben");
        at("2026-11-10T08:00:00Z");
        rentals.riding(b22, CENTRAL, ben);

        // when: fleet operations' clock ends it after the 12 hours (DE-20)
        at("2026-11-10T20:00:30Z");
        overdueSessionsAreEnded();

        // then
        await().atMost(HAND_OFF).untilAsserted(() -> {
            var bike = brokenBike(b22).body();
            assertThat(bike).isNotNull();
            assertThat(bike.status()).isEqualTo("BROKEN");
            assertThat(bike.faultSource()).isEqualTo("NOT_RETURNED");
        });
    }

    @Test
    @DisplayName("E2.1 a fleet issue makes an available bike broken, linked to the incident, and withdrawn")
    void e2_1_fleet_issue_makes_available_bike_broken_and_withdrawn() {
        // given
        var b17 = rentals.bikeAt(bike("B-17"), CENTRAL);
        at("2026-11-09T10:00:00Z");

        // when
        var i1 = reportFleetIssue("anna-" + suffix, b17, "rear brake doesn't work");

        // then
        await().atMost(HAND_OFF).untilAsserted(() -> {
            var bike = brokenBike(b17).body();
            assertThat(bike).isNotNull();
            assertThat(bike.status()).isEqualTo("BROKEN");
            assertThat(bike.faultSource()).isEqualTo("FLEET_ISSUE");
            assertThat(bike.incidentIds()).containsExactly(i1);
            assertThat(bike.reportedAt()).isEqualTo(Instant.parse("2026-11-09T10:00:00Z"));
        });
        await().atMost(HAND_OFF).untilAsserted(() -> {
            assertThat(rentals.bike(b17).body().availability()).isEqualTo("WITHDRAWN");
            assertThat(rentals.published(BikeWithdrawn.class, event -> event.bikeId().equals(b17)))
                .extracting(BikeWithdrawn::reason).containsExactly("REQUESTED");
        });
    }

    @Test
    @DisplayName("E2.2 a fleet issue cancels a rider's reservation; the bike is broken and withdrawn")
    void e2_2_fleet_issue_cancels_riders_reservation() {
        // given: B-17 reserved for Ben until 10:01
        var b17 = bike("B-17");
        var ben = rider("ben");
        at("2026-11-09T09:59:00Z");
        rentals.reserved(b17, CENTRAL, ben);

        // when
        at("2026-11-09T10:00:00Z");
        reportFleetIssue("anna-" + suffix, b17, "rear brake doesn't work");

        // then
        await().atMost(HAND_OFF).untilAsserted(() -> {
            var bike = rentals.bike(b17).body();
            assertThat(bike.availability()).isEqualTo("WITHDRAWN");
            assertThat(bike.hold()).isEqualTo("FREE");
            assertThat(rentals.activeRentalOf(ben).status()).isEqualTo(404);
        });
        await().atMost(HAND_OFF).untilAsserted(() -> assertThat(brokenBike(b17).body().status()).isEqualTo("BROKEN"));
    }

    @Test
    @DisplayName("E2.3 a fleet issue during a ride breaks and withdraws the bike, but the ride goes on")
    void e2_3_fleet_issue_during_ride_lets_ride_go_on() {
        // given: Anna's session on B-17 started at 10:00
        var b17 = bike("B-17");
        var anna = rider("anna");
        at("2026-11-09T10:00:00Z");
        rentals.riding(b17, CENTRAL, anna);

        // when
        at("2026-11-09T10:10:00Z");
        reportFleetIssue(anna, b17, "brake squeals");

        // then
        await().atMost(HAND_OFF).untilAsserted(() -> {
            assertThat(brokenBike(b17).body().status()).isEqualTo("BROKEN");
            assertThat(rentals.bike(b17).body().availability()).isEqualTo("WITHDRAWN");
        });
        assertThat(rentals.activeRentalOf(anna).body().kind()).isEqualTo("SESSION");
    }

    @Test
    @DisplayName("E3.1 a fleet issue on a broken bike only links the incident")
    void e3_1_fleet_issue_on_broken_bike_only_links_incident() {
        // given: B-17 broken since 10:40 (lock failed)
        var b17 = bike("B-17");
        lockFailedAtReturn(b17, "2026-11-09");
        awaitBroken(b17);

        // when
        at("2026-11-09T11:00:00Z");
        var i2 = reportFleetIssue("ben-" + suffix, b17, "brake squeals");

        // then
        await().atMost(HAND_OFF).untilAsserted(() -> assertThat(brokenBike(b17).body().incidentIds()).containsExactly(i2));
        var bike = brokenBike(b17).body();
        assertThat(bike.faultSource()).isEqualTo("LOCK_FAILED");
        assertThat(bike.reportedAt()).isEqualTo(Instant.parse("2026-11-09T10:40:00Z"));
        await().during(Duration.ofSeconds(1)).atMost(HAND_OFF).untilAsserted(() ->
            assertThat(rentals.published(BikeWithdrawn.class, event -> event.bikeId().equals(b17)))
                .extracting(BikeWithdrawn::reason).containsExactly("LOCK_FAILED"));
    }

    @Test
    @DisplayName("E4.1 a rider can't reserve a broken bike")
    void e4_1_rider_cannot_reserve_broken_bike() {
        // given: B-17 broken since 10:40 (lock failed) at Old Town
        var b17 = bike("B-17");
        lockFailedAtReturn(b17, "2026-11-09");
        awaitBroken(b17);
        var ben = rider("ben");

        // when
        at("2026-11-09T11:00:00Z");
        var reply = rentals.reserve(b17, ben, OLD_TOWN.northBy(10));

        // then
        assertThat(reply.status()).isEqualTo(409);
    }
}
