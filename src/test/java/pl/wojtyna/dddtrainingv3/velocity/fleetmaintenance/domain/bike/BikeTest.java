package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.BikeMaintenanceEvent.*;

@DisplayName("Bike maintenance examples")
class BikeTest {

    private static final BikeId B_17 = new BikeId("B-17");
    private static final TechnicianId T_1 = new TechnicianId("T-1");
    private static final TechnicianId T_2 = new TechnicianId("T-2");

    // @formatter:off
    @DisplayName(
        """
         given bike B-17 in circulation,
         when a fault is reported,
         then B-17 is broken
        """
    )
    // @formatter:on
    @Test
    void faultReportMakesBikeBroken() {
        // given
        var bike = Bike.inCirculation(B_17);

        // when
        var events = bike.reportFault();

        // then
        assertThat(events.hasAtLeastOneEventMatching(new BikeReportedBroken(B_17)::equals)).isTrue();
    }

    // @formatter:off
    @DisplayName(
        """
         given bike B-17 is already broken,
         when a second fault report for B-17 arrives,
         then nothing changes and nothing is recorded
        """
    )
    // @formatter:on
    @Test
    void duplicateFaultReportIsIgnored() {
        // given
        var bike = brokenBike();

        // when
        var events = bike.reportFault();

        // then
        assertThat(events.stream()).isEmpty();
        assertThat(bike.startMaintenance(T_1).hasOccurredEventOfType(MaintenanceStarted.class)).isTrue();
    }

    // @formatter:off
    @DisplayName(
        """
         given bike B-17 is broken,
         when technician T-1's unlock of B-17 is reported,
         then Maintenance Started by T-1 is recorded
        """
    )
    // @formatter:on
    @Test
    void technicianUnlockOfBrokenBikeStartsMaintenance() {
        // given
        var bike = brokenBike();

        // when
        var events = bike.startMaintenance(T_1);

        // then
        assertThat(events.hasAtLeastOneEventMatching(new MaintenanceStarted(B_17, T_1)::equals)).isTrue();
    }

    // @formatter:off
    @DisplayName(
        """
         given bike B-17 is not broken,
         when a technician's unlock of B-17 is reported,
         then the unlock is ignored: nothing is recorded and no Maintenance Started is published
        """
    )
    // @formatter:on
    @Test
    void technicianUnlockOfBikeThatIsNotBrokenIsIgnored() {
        // given
        var bike = Bike.inCirculation(B_17);

        // when
        var events = bike.startMaintenance(T_1);

        // then
        assertThat(events.stream()).isEmpty();
        assertThat(catchThrowable(() -> bike.returnToCirculation(T_1))).isInstanceOf(MaintenanceNotStarted.class);
    }

    // @formatter:off
    @DisplayName(
        """
         given bike B-17 is broken and reserved by technician T-1,
         when the reservation expires before T-1 unlocks it,
         then B-17 is still broken, is not offered to riders, and can be reserved by any technician
        """
    )
    // @formatter:on
    @Test
    void anyTechnicianCanStartMaintenanceAfterExpiredReservation() {
        // given
        var bike = brokenBike();
        // T-1's reservation and its expiry are Rentals' state: nothing reaches Fleet Maintenance (plan D6)

        // when
        var events = bike.startMaintenance(T_2);

        // then
        assertThat(events.hasAtLeastOneEventMatching(new MaintenanceStarted(B_17, T_2)::equals)).isTrue();
        assertThat(events.hasOccurredEventOfType(BikeFixed.class)).isFalse();
    }

    // @formatter:off
    @DisplayName(
        """
         given bike B-17 is broken and maintenance on it has not started,
         when a technician returns B-17 to circulation,
         then the request is rejected and B-17 stays withdrawn
        """
    )
    // @formatter:on
    @Test
    void returnWithoutMaintenanceIsRejected() {
        // given
        var bike = brokenBike();

        // when
        var exception = catchThrowable(() -> bike.returnToCirculation(T_1));

        // then
        assertThat(exception).isInstanceOf(MaintenanceNotStarted.class);
        assertThat(bike.startMaintenance(T_1).hasOccurredEventOfType(MaintenanceStarted.class)).isTrue();
    }

    // @formatter:off
    @DisplayName(
        """
         given bike B-17 is broken and maintenance on it has started,
         when a technician returns B-17 to circulation,
         then Bike Fixed is recorded for B-17
        """
    )
    // @formatter:on
    @Test
    void returningBikeUnderMaintenanceFixesIt() {
        // given
        var bike = brokenBike();
        bike.startMaintenance(T_1);

        // when
        var events = bike.returnToCirculation(T_1);

        // then
        assertThat(events.hasAtLeastOneEventMatching(new BikeFixed(B_17, T_1)::equals)).isTrue();
        assertThat(bike.reportFault().hasOccurredEventOfType(BikeReportedBroken.class)).isTrue();
    }

    private static Bike brokenBike() {
        var bike = Bike.inCirculation(B_17);
        bike.reportFault();
        return bike;
    }
}
