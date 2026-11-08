package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.BikeEvent.BikeReservedForTechnician;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.BikeEvent.TechnicianReservationReleased;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.BikeFixture.FakeServiceReservations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.BikeFixture.*;

/// Technician reservations: spec `technician-reserves-broken-bike` (Fleet Maintenance's half; the hold itself and
/// its 30 minutes are Rentals').
@DisplayName("Technician reservation examples")
class TechnicianReservationTest {

    // @formatter:off
    @DisplayName(
        """
         E1.1 given B-17 is broken and nobody holds it,
         when Tom reserves B-17 at 09:00 and Rentals holds it for him until 09:30,
         then B-17 is reserved for Tom until 09:30 (Bike Reserved) and stays broken
        """
    )
    // @formatter:on
    @Test
    void technicianReservesBrokenBikeUntilRentalsSays() {
        // given
        var bike = brokenBike(B17, FaultSource.LOCK_FAILED, "08:00");
        var rentals = new FakeServiceReservations(at("09:30"));

        // when
        var events = bike.reserveFor(TOM).through(rentals);

        // then
        assertThat(events.stream().toList()).containsExactly(new BikeReservedForTechnician(B17, TOM, at("09:30")));
        assertThat(bike.reservation()).contains(new TechnicianReservation(TOM, at("09:30")));
        assertThat(bike.condition()).isEqualTo(BikeCondition.BROKEN);
        assertThat(rentals.asked).containsExactly(TOM);
    }

    // @formatter:off
    @DisplayName(
        """
         E2.1 given B-40 isn't broken,
         when Tom reserves B-40 at 09:00,
         then the reservation is rejected and Rentals isn't asked
        """
    )
    // @formatter:on
    @Test
    void technicianCannotReserveBikeThatIsNotBroken() {
        // given
        var bike = Bike.unknown(B40);
        var rentals = new FakeServiceReservations(at("09:30"));

        // when
        var thrown = catchThrowable(() -> bike.reserveFor(TOM).through(rentals));

        // then
        assertThat(thrown).isInstanceOf(NotBroken.class);
        assertThat(rentals.asked).isEmpty();
        assertThat(bike.reservation()).isEmpty();
    }

    // @formatter:off
    @DisplayName(
        """
         E3.1 given B-17 is reserved for Tom until 09:30,
         when Tara reserves B-17 at 09:10 and Rentals answers that someone else holds it,
         then the reservation is rejected and B-17 stays reserved for Tom
        """
    )
    // @formatter:on
    @Test
    void secondTechnicianCannotReserveReservedBike() {
        // given
        var bike = reservedBrokenBike(B17, TOM, "09:30");

        // when
        var thrown = catchThrowable(() -> bike.reserveFor(TARA).through(FakeServiceReservations.heldBySomeoneElse()));

        // then
        assertThat(thrown).isInstanceOf(AlreadyReserved.class);
        assertThat(bike.reservation()).contains(new TechnicianReservation(TOM, at("09:30")));
    }

    // @formatter:off
    @DisplayName(
        """
         E4.1 given B-17 is reserved for Tom from 09:00 until 09:30,
         when Rentals reports Tom's reservation expired at 09:30:01,
         then B-17 is still broken, no longer reserved, and any technician may reserve it
        """
    )
    // @formatter:on
    @Test
    void expiredReservationLeavesBikeBroken() {
        // given
        var bike = reservedBrokenBike(B17, TOM, "09:30");

        // when
        var events = bike.releaseReservationOf(TOM).at(at("09:30:01"));

        // then
        assertThat(events.stream().toList()).containsExactly(new TechnicianReservationReleased(B17, TOM, at("09:30:01")));
        assertThat(bike.condition()).isEqualTo(BikeCondition.BROKEN);
        assertThat(bike.reservation()).isEmpty();
        assertThat(bike.reserveFor(TARA).through(new FakeServiceReservations(at("10:00"))).stream()).hasSize(1);
    }

    // @formatter:off
    @DisplayName(
        """
         derived: given B-17 is reserved for Tara until 10:00 (Tom's old reservation expired in the meantime),
         when Rentals reports Tom's reservation expired,
         then Tara keeps her reservation
        """
    )
    // @formatter:on
    @Test
    void releasingAnotherTechniciansReservationChangesNothing() {
        // given
        var bike = reservedBrokenBike(B17, TARA, "10:00");

        // when
        var events = bike.releaseReservationOf(TOM).at(at("09:30:01"));

        // then
        assertThat(events.stream().toList()).isEmpty();
        assertThat(bike.reservation()).contains(new TechnicianReservation(TARA, at("10:00")));
    }
}
