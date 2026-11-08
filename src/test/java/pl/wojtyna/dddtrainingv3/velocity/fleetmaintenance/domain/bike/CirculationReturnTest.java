package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.BikeEvent.BikeFixed;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.BikeFixture.FakeCirculationReturns;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.BikeFixture.*;

/// Returning bikes to circulation: spec `technician-returns-fixed-bike-to-circulation` (Fleet Maintenance's half).
@DisplayName("Circulation return examples")
class CirculationReturnTest {

    static final Place CENTRAL = Place.station("Central");

    // @formatter:off
    @DisplayName(
        """
         E1.1 given B-17 has been under maintenance by Tom since 09:20,
         when Tom returns B-17 to circulation at station Central at 11:00,
         then Rentals is asked to make B-17 available at Central and B-17 is fixed (Bike Fixed)
        """
    )
    // @formatter:on
    @Test
    void returnAtStationFixesBike() {
        // given
        var bike = bikeUnderMaintenance(B17, TOM, "09:20");
        var rentals = new FakeCirculationReturns();

        // when
        var events = bike.returnToCirculation(new CirculationReturn(TOM, CENTRAL, at("11:00"))).through(rentals);

        // then
        assertThat(events.stream().toList()).containsExactly(new BikeFixed(B17, TOM, CENTRAL, at("11:00"), List.of()));
        assertThat(rentals.places).containsExactly(CENTRAL);
        assertThat(bike.condition()).isEqualTo(BikeCondition.IN_CIRCULATION);
        assertThat(bike.maintenance()).isEmpty();
    }

    // @formatter:off
    @DisplayName(
        """
         E1.2 given B-22 has been under maintenance by Tom since 09:20,
         when Tom returns B-22 to circulation at position (52.2297, 21.0122) in drop zone Old Town Square at 11:00,
         then Rentals is asked to make B-22 available at that position and B-22 is fixed (Bike Fixed)
        """
    )
    // @formatter:on
    @Test
    void returnAtPositionFixesBike() {
        // given
        var bike = bikeUnderMaintenance(B22, TOM, "09:20");
        var rentals = new FakeCirculationReturns();
        var dropZone = Place.position(new BigDecimal("52.2297"), new BigDecimal("21.0122"));

        // when
        var events = bike.returnToCirculation(new CirculationReturn(TOM, dropZone, at("11:00"))).through(rentals);

        // then
        assertThat(events.stream().toList()).containsExactly(new BikeFixed(B22, TOM, dropZone, at("11:00"), List.of()));
        assertThat(rentals.places).containsExactly(dropZone);
    }

    // @formatter:off
    @DisplayName(
        """
         E1.3 given B-17 has been under maintenance by Tom since 09:20,
         when technician Tara returns B-17 to circulation at Old Town at 11:00,
         then B-17 is fixed by Tara: any technician may return it (DE-91)
        """
    )
    // @formatter:on
    @Test
    void anyTechnicianMayReturnBike() {
        // given
        var bike = bikeUnderMaintenance(B17, TOM, "09:20");
        var oldTown = Place.station("Old Town");

        // when
        var events = bike.returnToCirculation(new CirculationReturn(TARA, oldTown, at("11:00"))).through(new FakeCirculationReturns());

        // then
        assertThat(events.stream().toList()).containsExactly(new BikeFixed(B17, TARA, oldTown, at("11:00"), List.of()));
    }

    // @formatter:off
    @DisplayName(
        """
         E2.1 given B-17 is broken and reserved for Tom, but Tom hasn't unlocked it,
         when Tom returns B-17 to circulation at Central at 09:15,
         then it is rejected, Rentals isn't asked and B-17 stays broken
        """
    )
    // @formatter:on
    @Test
    void returnBeforeMaintenanceStartedIsRejected() {
        // given
        var bike = reservedBrokenBike(B17, TOM, "09:30");
        var rentals = new FakeCirculationReturns();

        // when
        var thrown = catchThrowable(() -> bike.returnToCirculation(new CirculationReturn(TOM, CENTRAL, at("09:15"))).through(rentals));

        // then
        assertThat(thrown).isInstanceOf(MaintenanceNotStarted.class);
        assertThat(rentals.places).isEmpty();
        assertThat(bike.condition()).isEqualTo(BikeCondition.BROKEN);
    }

    // @formatter:off
    @DisplayName(
        """
         E2.2 given B-40 isn't broken,
         when Tom returns B-40 to circulation at Old Town at 10:00,
         then it is rejected and nothing changes
        """
    )
    // @formatter:on
    @Test
    void returnOfBikeThatIsNotBrokenIsRejected() {
        // given
        var bike = Bike.unknown(B40);
        var rentals = new FakeCirculationReturns();

        // when
        var thrown = catchThrowable(() -> bike.returnToCirculation(new CirculationReturn(TOM, Place.station("Old Town"), at("10:00")))
                                              .through(rentals));

        // then
        assertThat(thrown).isInstanceOf(NotBroken.class);
        assertThat(rentals.places).isEmpty();
        assertThat(bike.condition()).isEqualTo(BikeCondition.IN_CIRCULATION);
    }

    // @formatter:off
    @DisplayName(
        """
         E3.1 given B-17 is under maintenance by Tom and linked to fleet-issue incidents I-1 and I-2,
         when Tom returns B-17 to circulation at Central at 11:00,
         then Bike Fixed names I-1 and I-2, the incidents to resolve with "bike fixed"
        """
    )
    // @formatter:on
    @Test
    void fixedBikeNamesItsLinkedIncidents() {
        // given
        var bike = Bike.unknown(B17);
        bike.reportFault(fleetIssue(I1, "rear brake doesn't work")).at(at("08:00"));
        bike.reportFault(fleetIssue(I2, "brakes squeal")).at(at("08:10"));
        bike.reserveFor(TOM).through(new BikeFixture.FakeServiceReservations(at("09:30")));
        bike.startMaintenanceBy(TOM).at(at("09:20"));

        // when
        var events = bike.returnToCirculation(new CirculationReturn(TOM, CENTRAL, at("11:00"))).through(new FakeCirculationReturns());

        // then
        assertThat(events.stream().toList()).containsExactly(new BikeFixed(B17, TOM, CENTRAL, at("11:00"), List.of(I1, I2)));
    }

    // @formatter:off
    @DisplayName(
        """
         derived (plan D5): given B-17 is under maintenance by Tom,
         when Tom returns it to circulation and Rentals refuses to make it available,
         then B-17 isn't fixed and stays under maintenance
        """
    )
    // @formatter:on
    @Test
    void bikeIsNotFixedWhenRentalsRefuses() {
        // given
        var bike = bikeUnderMaintenance(B17, TOM, "09:20");

        // when
        var thrown = catchThrowable(() -> bike.returnToCirculation(new CirculationReturn(TOM, CENTRAL, at("11:00")))
                                              .through(FakeCirculationReturns.refusing()));

        // then
        assertThat(thrown).isInstanceOf(CirculationReturnRefused.class);
        assertThat(bike.condition()).isEqualTo(BikeCondition.UNDER_MAINTENANCE);
    }
}
