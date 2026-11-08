package pl.wojtyna.dddtrainingv3.velocity.journeys;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pl.wojtyna.dddtrainingv3.velocity.common.VelocitySpringTest;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeLocks.LockResult;
import pl.wojtyna.dddtrainingv3.velocity.rentals.infra.adapters.secondary.bikelocksacl.SimulatedBikeLocks;

import java.math.BigDecimal;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.wojtyna.dddtrainingv3.velocity.journeys.VelocityApps.CENTRAL;
import static pl.wojtyna.dddtrainingv3.velocity.journeys.VelocityApps.RIVERSIDE;
import static pl.wojtyna.dddtrainingv3.velocity.journeys.VelocityApps.expect;
import static pl.wojtyna.dddtrainingv3.velocity.journeys.VelocityApps.unique;

/// A bike whose lock fails goes through maintenance and back to riders: Rentals → Settlement (`RentalSessionEnded`,
/// lock failure), Rentals → Fleet Maintenance (`BikeWithdrawn`), Fleet Maintenance → Rentals (technician's
/// reservation and unlock, REST), Rentals → Fleet Maintenance (`BikeUnlocked`), Fleet Maintenance → Rentals (make
/// available, REST).
@VelocitySpringTest
class BrokenBikeMaintenanceJourneyTest extends JourneyTestBase {

    @Autowired
    SimulatedBikeLocks bikeLocks;

    @Test
    @DisplayName("""
        A rider returns a bike at Central after 40 minutes but its lock fails: the session ends with LOCK_FAILURE \
        and is priced without a return fee (PLN 3.00), Rentals withdraws the bike and Fleet Maintenance registers it \
        as broken. A technician reserves and unlocks it through Fleet Maintenance, which starts maintenance, then \
        returns it to circulation at Riverside: Rentals makes it available there and another rider reserves it""")
    void brokenBikeIsRepairedAndRidden() {
        clock.setTo("2027-05-19T07:30:00Z");
        var riderId = apps.onboardedRider("tok_visa_" + unique("lock-failure"));
        var bikeId = unique("bike-lock");
        expect(apps.addBikeToFleet(bikeId, CENTRAL.stationId()), 201);
        expect(apps.reserve(bikeId, riderId, CENTRAL.tenMetresNorth()), 201);
        var sessionId = expect(apps.unlock(bikeId, riderId), 200).get("sessionId");
        clock.advanceBy(Duration.ofMinutes(40));

        // the bike's lock fails when the rider returns it
        bikeLocks.scriptNext(bikeId, LockResult.FAILED);
        assertThat(expect(apps.returnAtStation(bikeId, riderId, CENTRAL.stationId()), 200).get("endReason"))
            .isEqualTo("LOCK_FAILURE");

        // priced up to the failed lock, nothing extra (DE-32)
        eventually(() -> {
            var ride = expect(apps.rideSettlement(sessionId), 200);
            assertThat(ride.get("status")).isEqualTo("SETTLED");
            assertThat(new BigDecimal(ride.get("fee"))).isEqualByComparingTo("3.00");
        });

        // withdrawn from riders and registered as broken
        eventually(() -> assertThat(expect(apps.bike(bikeId), 200).get("availability")).isEqualTo("WITHDRAWN"));
        eventually(() -> assertThat(expect(apps.brokenBike(bikeId), 200).get("status")).isEqualTo("BROKEN"));

        // a technician reserves and unlocks it through Fleet Maintenance: maintenance starts
        var technicianId = unique("tech");
        expect(apps.reserveBrokenBike(bikeId, technicianId), 200);
        eventually(() -> {
            var bike = expect(apps.bike(bikeId), 200);
            assertThat(bike.get("hold")).isEqualTo("RESERVED");
            assertThat(bike.get("holderType")).isEqualTo("TECHNICIAN");
        });
        assertThat(expect(apps.unlockBrokenBike(bikeId, technicianId), 200).get("outcome")).isEqualTo("UNLOCKED");
        eventually(() -> {
            var broken = expect(apps.brokenBike(bikeId), 200);
            assertThat(broken.get("status")).isEqualTo("UNDER_MAINTENANCE");
            assertThat(broken.get("technicianId")).isEqualTo(technicianId);
        });

        // the technician returns it to circulation at Riverside
        clock.advanceBy(Duration.ofHours(2));
        expect(apps.returnToCirculationAtStation(bikeId, technicianId, RIVERSIDE.stationId()), 200);
        eventually(() -> {
            var bike = expect(apps.bike(bikeId), 200);
            assertThat(bike.get("availability")).isEqualTo("AVAILABLE");
            assertThat(bike.get("hold")).isEqualTo("FREE");
            assertThat(bike.get("stationId")).isEqualTo(RIVERSIDE.stationId());
        });
        eventually(() -> assertThat(expect(apps.brokenBike(bikeId), 200).get("status")).isEqualTo("IN_CIRCULATION"));

        // another rider reserves it standing next to it at Riverside
        var anotherRider = apps.onboardedRider("tok_visa_" + unique("after-repair"));
        expect(apps.reserve(bikeId, anotherRider, RIVERSIDE.tenMetresNorth()), 201);
    }
}
