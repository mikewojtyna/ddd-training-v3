package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.velocity.common.VelocitySpringTest;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static pl.wojtyna.dddtrainingv3.velocity.rentals.RentalsDriver.CENTRAL;
import static pl.wojtyna.dddtrainingv3.velocity.rentals.RentalsDriver.OLD_TOWN;

/// Acceptance tests of spec `technician-returns-fixed-bike-to-circulation`: Fleet Maintenance → Rentals (Make Bike
/// Available) and Fleet Maintenance → Customer Support (Resolve Fleet Issue), asserted with Awaitility. Set up through
/// the REST APIs: fleet issues via Customer Support, reservation and unlock via Fleet Maintenance.
@VelocitySpringTest
@DisplayName("technician-returns-fixed-bike-to-circulation")
class TechnicianReturnsFixedBikeToCirculationSpecTest extends FleetMaintenanceSpecTestBase {

    @Test
    @DisplayName("E1.1 returning a bike under maintenance at a station fixes it and makes it available there")
    void e1_1_return_at_station_fixes_bike_and_makes_it_available() {
        // given
        var b17 = bike("B-17");
        var tom = technician("tom");
        at("2026-11-09T09:20:00Z");
        underMaintenance(b17, OLD_TOWN, tom);

        // when
        at("2026-11-09T11:00:00Z");
        var reply = returnToCirculationAtStation(b17, tom, CENTRAL.stationId());

        // then
        assertThat(reply.status()).isEqualTo(200);
        assertThat(brokenBike(b17).body().status()).isEqualTo("IN_CIRCULATION");
        await().atMost(HAND_OFF).untilAsserted(() -> {
            var inRentals = rentals.bike(b17).body();
            assertThat(inRentals.availability()).isEqualTo("AVAILABLE");
            assertThat(inRentals.hold()).isEqualTo("FREE");
            assertThat(inRentals.stationId()).isEqualTo(CENTRAL.stationId());
        });
        assertThat(brokenBikes()).extracting(BrokenBike::bikeId).doesNotContain(b17);
    }

    @Test
    @DisplayName("E1.2 returning a bike in a drop zone makes it available at that position")
    void e1_2_return_in_drop_zone_makes_bike_available_at_position() {
        // given
        var b22 = bike("B-22");
        var tom = technician("tom");
        at("2026-11-09T09:20:00Z");
        underMaintenance(b22, CENTRAL, tom);

        // when
        at("2026-11-09T11:00:00Z");
        var reply = returnToCirculationAt(b22, tom, 52.2297, 21.0122);

        // then
        assertThat(reply.status()).isEqualTo(200);
        assertThat(brokenBike(b22).body().status()).isEqualTo("IN_CIRCULATION");
        await().atMost(HAND_OFF).untilAsserted(() -> {
            var inRentals = rentals.bike(b22).body();
            assertThat(inRentals.availability()).isEqualTo("AVAILABLE");
            assertThat(inRentals.stationId()).isNull();
            assertThat(inRentals.latitude()).isEqualByComparingTo(new BigDecimal("52.2297"));
            assertThat(inRentals.longitude()).isEqualByComparingTo(new BigDecimal("21.0122"));
        });
    }

    @Test
    @DisplayName("E1.3 any technician may return a bike under maintenance")
    void e1_3_any_technician_may_return_bike() {
        // given
        var b17 = bike("B-17");
        at("2026-11-09T09:20:00Z");
        underMaintenance(b17, CENTRAL, technician("tom"));

        // when
        at("2026-11-09T11:00:00Z");
        var reply = returnToCirculationAtStation(b17, technician("tara"), OLD_TOWN.stationId());

        // then
        assertThat(reply.status()).isEqualTo(200);
        assertThat(brokenBike(b17).body().status()).isEqualTo("IN_CIRCULATION");
        await().atMost(HAND_OFF).untilAsserted(() -> {
            var inRentals = rentals.bike(b17).body();
            assertThat(inRentals.availability()).isEqualTo("AVAILABLE");
            assertThat(inRentals.stationId()).isEqualTo(OLD_TOWN.stationId());
        });
    }

    @Test
    @DisplayName("E2.1 a bike can't return to circulation before maintenance started")
    void e2_1_return_before_maintenance_started_is_rejected() {
        // given: B-17 broken and reserved for Tom, not unlocked
        var b17 = bike("B-17");
        var tom = technician("tom");
        at("2026-11-09T09:00:00Z");
        brokenAt(b17, CENTRAL, technician("ivy"));
        assertThat(reserveBrokenBike(b17, tom).status()).isEqualTo(200);

        // when
        at("2026-11-09T09:15:00Z");
        var reply = returnToCirculationAtStation(b17, tom, CENTRAL.stationId());

        // then
        assertThat(reply.status()).isEqualTo(409);
        assertThat(reply.error()).isEqualTo("MaintenanceNotStarted");
        assertThat(brokenBike(b17).body().status()).isEqualTo("BROKEN");
        assertThat(rentals.bike(b17).body().availability()).isEqualTo("WITHDRAWN");
    }

    @Test
    @DisplayName("E2.2 a bike that isn't broken can't be returned to circulation")
    void e2_2_return_of_bike_not_broken_is_rejected() {
        // given
        var b40 = rentals.bikeAt(bike("B-40"), CENTRAL);

        // when
        at("2026-11-09T10:00:00Z");
        var reply = returnToCirculationAtStation(b40, technician("tom"), OLD_TOWN.stationId());

        // then
        assertThat(reply.status()).isEqualTo(409);
        assertThat(reply.error()).isEqualTo("NotBroken");
        var inRentals = rentals.bike(b40).body();
        assertThat(inRentals.availability()).isEqualTo("AVAILABLE");
        assertThat(inRentals.stationId()).isEqualTo(CENTRAL.stationId());
    }

    @Test
    @DisplayName("E3.1 fixing a bike resolves every open linked fleet-issue incident with \"bike fixed\"")
    void e3_1_fix_resolves_every_open_linked_incident() {
        // given: B-17 linked to I-1 (accepted by Zoe) and I-2 (reported), under maintenance by Tom
        var b17 = rentals.bikeAt(bike("B-17"), CENTRAL);
        var tom = technician("tom");
        var zoe = "zoe-" + suffix;
        at("2026-11-09T08:00:00Z");
        var i1 = reportFleetIssue("anna-" + suffix, b17, "rear brake doesn't work");
        awaitWithdrawn(b17);
        assertThat(supportAgent("assignment", i1, Map.of("agentId", zoe)).status()).isEqualTo(200);
        assertThat(supportAgent("acceptance", i1, Map.of("agentId", zoe)).status()).isEqualTo(200);
        var i2 = reportFleetIssue("ben-" + suffix, b17, "brakes squeal");
        await().atMost(HAND_OFF).untilAsserted(() -> assertThat(brokenBike(b17).body().incidentIds()).containsExactly(i1, i2));
        at("2026-11-09T09:00:00Z");
        assertThat(reserveBrokenBike(b17, tom).status()).isEqualTo(200);
        at("2026-11-09T09:20:00Z");
        unlockBrokenBike(b17, tom);
        await().atMost(HAND_OFF).untilAsserted(() -> assertThat(brokenBike(b17).body().status()).isEqualTo("UNDER_MAINTENANCE"));

        // when
        at("2026-11-09T11:00:00Z");
        var reply = returnToCirculationAtStation(b17, tom, CENTRAL.stationId());

        // then
        assertThat(reply.status()).isEqualTo(200);
        await().atMost(HAND_OFF).untilAsserted(() -> {
            assertThat(incident(i1).status()).isEqualTo("RESOLVED");
            assertThat(incident(i1).resolution()).isEqualTo("bike fixed");
            assertThat(incident(i2).status()).isEqualTo("RESOLVED");
            assertThat(incident(i2).resolution()).isEqualTo("bike fixed");
        });
    }

    @Test
    @DisplayName("E3.2 an incident already resolved keeps its resolution")
    void e3_2_already_resolved_incident_keeps_its_resolution() {
        // given: I-1 about B-17, resolved by Zoe at 10:00; B-17 under maintenance by Tom
        var b17 = rentals.bikeAt(bike("B-17"), CENTRAL);
        var tom = technician("tom");
        var zoe = "zoe-" + suffix;
        at("2026-11-09T08:00:00Z");
        var i1 = reportFleetIssue("anna-" + suffix, b17, "rear brake doesn't work");
        awaitWithdrawn(b17);
        at("2026-11-09T09:00:00Z");
        assertThat(reserveBrokenBike(b17, tom).status()).isEqualTo(200);
        at("2026-11-09T09:20:00Z");
        unlockBrokenBike(b17, tom);
        await().atMost(HAND_OFF).untilAsserted(() -> assertThat(brokenBike(b17).body().status()).isEqualTo("UNDER_MAINTENANCE"));
        at("2026-11-09T10:00:00Z");
        supportAgent("assignment", i1, Map.of("agentId", zoe));
        supportAgent("acceptance", i1, Map.of("agentId", zoe));
        assertThat(supportAgent("resolution", i1, Map.of("agentId", zoe, "resolution", "brake adjusted on site")).status()).isEqualTo(200);

        // when
        at("2026-11-09T11:00:00Z");
        var reply = returnToCirculationAtStation(b17, tom, CENTRAL.stationId());

        // then
        assertThat(reply.status()).isEqualTo(200);
        await().during(Duration.ofSeconds(1)).atMost(HAND_OFF).untilAsserted(() -> {
            assertThat(incident(i1).status()).isEqualTo("RESOLVED");
            assertThat(incident(i1).resolution()).isEqualTo("brake adjusted on site");
        });
    }
}
