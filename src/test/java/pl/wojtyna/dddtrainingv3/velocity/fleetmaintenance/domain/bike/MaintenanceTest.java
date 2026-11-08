package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.BikeEvent.MaintenanceStarted;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.BikeEvent.TechnicianReservationReleased;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.BikeFixture.FakeServiceUnlocks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.BikeFixture.*;

/// Maintenance: spec `technician-starts-maintenance` (Fleet Maintenance's half; the unlock itself is Rentals').
@DisplayName("Maintenance examples")
class MaintenanceTest {

    // @formatter:off
    @DisplayName(
        """
         E1.1 given B-17 is broken and reserved for Tom from 09:00 until 09:30,
         when Rentals reports B-17 unlocked by Tom at 09:20,
         then maintenance on B-17 started at 09:20 (Maintenance Started)
        """
    )
    // @formatter:on
    @Test
    void technicianUnlockStartsMaintenance() {
        // given
        var bike = reservedBrokenBike(B17, TOM, "09:30");

        // when
        var events = bike.startMaintenanceBy(TOM).at(at("09:20"));

        // then
        assertThat(events.stream().toList()).containsExactly(new MaintenanceStarted(B17, TOM, at("09:20")));
        assertThat(bike.condition()).isEqualTo(BikeCondition.UNDER_MAINTENANCE);
        assertThat(bike.maintenance()).contains(new Maintenance(TOM, at("09:20")));
        assertThat(bike.reservation()).isEmpty();
    }

    // @formatter:off
    @DisplayName(
        """
         E4.1 given B-17 is broken and reserved for Tom until 09:30,
         when Rentals reports Tom's unlock of B-17 failed at 09:20,
         then no maintenance starts, B-17 is still broken and any technician can reserve it
        """
    )
    // @formatter:on
    @Test
    void failedUnlockLeavesBikeBrokenAndUnreserved() {
        // given
        var bike = reservedBrokenBike(B17, TOM, "09:30");

        // when
        var events = bike.releaseReservationOf(TOM).at(at("09:20"));

        // then
        assertThat(events.stream().toList()).containsExactly(new TechnicianReservationReleased(B17, TOM, at("09:20")));
        assertThat(bike.condition()).isEqualTo(BikeCondition.BROKEN);
        assertThat(bike.maintenance()).isEmpty();
        assertThat(bike.reservation()).isEmpty();
    }

    // @formatter:off
    @DisplayName(
        """
         derived: given B-17 is broken,
         when Tom unlocks it,
         then Rentals is asked to unlock it for Tom; maintenance starts only when Rentals reports the unlock
        """
    )
    // @formatter:on
    @Test
    void unlockOfBrokenBikeIsForwardedToRentals() {
        // given
        var bike = reservedBrokenBike(B17, TOM, "09:30");
        var rentals = new FakeServiceUnlocks();

        // when
        var outcome = bike.unlockFor(TOM).through(rentals);

        // then
        assertThat(outcome).isEqualTo(ServiceUnlocks.Outcome.UNLOCKED);
        assertThat(rentals.asked).containsExactly(TOM);
        assertThat(bike.condition()).isEqualTo(BikeCondition.BROKEN);
    }

    // @formatter:off
    @DisplayName(
        """
         derived: given B-40 isn't broken,
         when Tom unlocks it,
         then it is rejected and Rentals isn't asked
        """
    )
    // @formatter:on
    @Test
    void unlockOfBikeThatIsNotBrokenIsRejected() {
        // given
        var bike = Bike.unknown(B40);
        var rentals = new FakeServiceUnlocks();

        // when
        var thrown = catchThrowable(() -> bike.unlockFor(TOM).through(rentals));

        // then
        assertThat(thrown).isInstanceOf(NotBroken.class);
        assertThat(rentals.asked).isEmpty();
    }

    // @formatter:off
    @DisplayName(
        """
         derived: given B-17 is under maintenance by Tom since 09:20,
         when Rentals reports the unlock again,
         then nothing changes
        """
    )
    // @formatter:on
    @Test
    void repeatedUnlockReportChangesNothing() {
        // given
        var bike = bikeUnderMaintenance(B17, TOM, "09:20");

        // when
        var events = bike.startMaintenanceBy(TOM).at(at("09:25"));

        // then
        assertThat(events.stream().toList()).isEmpty();
        assertThat(bike.maintenance()).contains(new Maintenance(TOM, at("09:20")));
    }
}
