package pl.wojtyna.dddtrainingv3.velocity.journeys;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.velocity.common.VelocitySpringTest;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.wojtyna.dddtrainingv3.velocity.journeys.VelocityApps.CENTRAL;
import static pl.wojtyna.dddtrainingv3.velocity.journeys.VelocityApps.expect;
import static pl.wojtyna.dddtrainingv3.velocity.journeys.VelocityApps.unique;

/// A rider's broken-bike report is handled by support and fixed by fleet maintenance: Customer Support → Fleet
/// Maintenance (`FleetIssueReported`), Fleet Maintenance → Rentals (withdraw, reserve, unlock, make available; REST),
/// Rentals → Fleet Maintenance (`BikeUnlocked`), Fleet Maintenance → Customer Support (resolve fleet issue, REST).
@VelocitySpringTest
class FleetIssueIncidentJourneyTest extends JourneyTestBase {

    @Test
    @DisplayName("""
        A rider reports a broken bike in the app: the incident is a FLEET_ISSUE, Fleet Maintenance registers the \
        bike as broken and Rentals withdraws it. A support agent picks the incident up and accepts it. A technician \
        reserves, unlocks and returns the bike to circulation, and the incident is resolved automatically with \
        "bike fixed" (DE-55)""")
    void fleetIssueIsResolvedWhenTheBikeIsFixed() {
        clock.setTo("2027-06-23T12:00:00Z");
        var bikeId = unique("bike-incident");
        expect(apps.addBikeToFleet(bikeId, CENTRAL.stationId()), 201);
        var riderId = apps.onboardedRider("tok_visa_" + unique("reporter"));

        // the rider reports the bike broken
        var report = expect(apps.reportIncident(riderId, "BROKEN_BIKE", "The chain is off", bikeId), 201);
        assertThat(report.get("type")).isEqualTo("FLEET_ISSUE");
        var incidentId = report.get("incidentId");

        // safety first: broken in Fleet Maintenance, withdrawn from riders in Rentals (DE-52)
        eventually(() -> {
            var broken = expect(apps.brokenBike(bikeId), 200);
            assertThat(broken.get("status")).isEqualTo("BROKEN");
            assertThat(broken.list("incidentIds")).containsExactly(incidentId);
        });
        eventually(() -> assertThat(expect(apps.bike(bikeId), 200).get("availability")).isEqualTo("WITHDRAWN"));

        // a support agent picks the incident up and accepts it
        var agentId = unique("agent");
        expect(apps.pickUpIncident(incidentId, agentId), 200);
        expect(apps.acceptIncident(incidentId, agentId), 200);
        var accepted = expect(apps.incident(incidentId), 200);
        assertThat(accepted.get("status")).isEqualTo("ACCEPTED");
        assertThat(accepted.get("assignedAgentId")).isEqualTo(agentId);

        // a technician reserves, unlocks and fixes the bike
        var technicianId = unique("tech");
        expect(apps.reserveBrokenBike(bikeId, technicianId), 200);
        assertThat(expect(apps.unlockBrokenBike(bikeId, technicianId), 200).get("outcome")).isEqualTo("UNLOCKED");
        eventually(() -> assertThat(expect(apps.brokenBike(bikeId), 200).get("status")).isEqualTo("UNDER_MAINTENANCE"));
        clock.advanceBy(Duration.ofHours(1));
        expect(apps.returnToCirculationAtStation(bikeId, technicianId, CENTRAL.stationId()), 200);

        // the incident is resolved automatically
        eventually(() -> {
            var incident = expect(apps.incident(incidentId), 200);
            assertThat(incident.get("status")).isEqualTo("RESOLVED");
            assertThat(incident.get("resolution")).isEqualTo("bike fixed");
        });
        eventually(() -> assertThat(expect(apps.bike(bikeId), 200).get("availability")).isEqualTo("AVAILABLE"));
    }
}
