package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeEvent.BikeWithdrawn;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeEvent.RentalSessionEnded;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeEvent.ReservationCancelled;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeFixture.*;

/// Spec `fleet-operations-withdraws-bike-signalling-fault`.
class FaultSignalTest {

    // @formatter:off
    @DisplayName(
        """
         given B-17 is available at station Central and nobody holds it,
         when B-17 signals fault "BRAKE_SENSOR" at 09:00,
         then B-17 is withdrawn with reason fault signalled (Bike Withdrawn)
        """
    )
    // @formatter:on
    @Test
    void freeBikeSignallingFaultIsWithdrawn() {
        // given
        var b17 = availableAt("B-17", CENTRAL);

        // when
        var events = b17.signalFault("BRAKE_SENSOR").at(at("09:00"));

        // then
        assertThat(all(events)).containsExactly(
            new BikeWithdrawn(new BikeId("B-17"), WithdrawalReason.FAULT_SIGNALLED, "BRAKE_SENSOR", at("09:00")));
    }

    // @formatter:off
    @DisplayName(
        """
         given B-17 signalled fault "BRAKE_SENSOR" at 09:00 and was withdrawn,
         when rider Anna, standing 10 m from B-17, reserves it at 09:05,
         then the reservation is rejected
        """
    )
    // @formatter:on
    @Test
    void withdrawnBikeCannotBeReservedByRider() {
        // given
        var b17 = availableAt("B-17", CENTRAL);
        b17.signalFault("BRAKE_SENSOR").at(at("09:00"));

        // when / then
        assertThatThrownBy(() -> b17.reserveForRide(ANNA).standingAt(near(CENTRAL, 10)).given(ELIGIBLE).at(at("09:05")))
            .isInstanceOf(BikeNotAvailable.class);
    }

    // @formatter:off
    @DisplayName(
        """
         given B-17 at Central is reserved for Anna from 10:00 until 10:02,
         when B-17 signals fault "BRAKE_SENSOR" at 10:01,
         then Anna's reservation is cancelled, Anna may reserve another bike,
         and B-17 is withdrawn with reason fault signalled (Bike Withdrawn)
        """
    )
    // @formatter:on
    @Test
    void reservationOnFaultyBikeIsCancelledAndBikeWithdrawn() {
        // given
        var b17 = reservedFor(ANNA, "B-17", at("10:00"));

        // when
        var events = b17.signalFault("BRAKE_SENSOR").at(at("10:01"));

        // then
        assertThat(all(events)).containsExactly(
            new ReservationCancelled(new BikeId("B-17"), ANNA, at("10:01")),
            new BikeWithdrawn(new BikeId("B-17"), WithdrawalReason.FAULT_SIGNALLED, "BRAKE_SENSOR", at("10:01")));
        assertThat(b17.isHeldBy(ANNA, at("10:01"))).isFalse();
    }

    // @formatter:off
    @DisplayName(
        """
         given Anna's rental session on B-17 started at 10:01,
         when B-17 signals fault "BRAKE_SENSOR" at 10:15,
         then Anna's session keeps running and B-17 isn't withdrawn yet
        """
    )
    // @formatter:on
    @Test
    void faultDuringRideDoesNotInterruptIt() {
        // given
        var b17 = inSessionFor(ANNA, "B-17", at("10:01"));

        // when
        var events = b17.signalFault("BRAKE_SENSOR").at(at("10:15"));

        // then
        assertThat(all(events)).isEmpty();
        assertThat(b17.isHeldBy(ANNA, at("10:15"))).isTrue();
    }

    // @formatter:off
    @DisplayName(
        """
         given Anna's rental session on B-17 started at 10:01 and B-17 signalled fault "BRAKE_SENSOR" at 10:15,
         when Anna returns B-17 at station Old Town at 10:40 and the lock closes,
         then the session ended at 10:40 at Old Town with end reason station return (Rental Session Ended)
         and B-17 is withdrawn with reason fault signalled (Bike Withdrawn)
        """
    )
    // @formatter:on
    @Test
    void bikeWithFaultDuringRideIsWithdrawnAtReturn() {
        // given
        var b17 = inSessionFor(ANNA, "B-17", at("10:01"));
        b17.signalFault("BRAKE_SENSOR").at(at("10:15"));

        // when
        var events = b17.returnAtStation(OLD_TOWN).by(ANNA).reporting(null).at(at("10:40")).using(FakeBikeLocks.working());

        // then
        assertThat(eventsOf(events, RentalSessionEnded.class)).singleElement()
                                                              .extracting(RentalSessionEnded::endReason)
                                                              .isEqualTo(EndReason.STATION_RETURN);
        assertThat(eventsOf(events, BikeWithdrawn.class)).containsExactly(
            new BikeWithdrawn(new BikeId("B-17"), WithdrawalReason.FAULT_SIGNALLED, "BRAKE_SENSOR", at("10:40")));
    }

    // @formatter:off
    @DisplayName(
        """
         given Anna's session on B-17 started at 10:01 and B-17 signalled fault "BRAKE_SENSOR" at 10:15,
         when Anna's return at Old Town at 10:40 fails to lock,
         then B-17 is withdrawn once, with the reason found at the session end: lock failed (plan decision D6)
        """
    )
    // @formatter:on
    @Test
    void pendingFaultAndLockFailureWithdrawOnceWithLockFailed() {
        // given
        var b17 = inSessionFor(ANNA, "B-17", at("10:01"));
        b17.signalFault("BRAKE_SENSOR").at(at("10:15"));

        // when
        var events = b17.returnAtStation(OLD_TOWN).by(ANNA).reporting(null).at(at("10:40")).using(FakeBikeLocks.failing());

        // then
        assertThat(eventsOf(events, BikeWithdrawn.class)).singleElement()
                                                         .extracting(BikeWithdrawn::reason)
                                                         .isEqualTo(WithdrawalReason.LOCK_FAILED);
    }

    // @formatter:off
    @DisplayName(
        """
         given B-17 was withdrawn at 08:00 with reason lock failed,
         when B-17 signals fault "BRAKE_SENSOR" at 09:00,
         then nothing changes: B-17 stays withdrawn and no second Bike Withdrawn happens
        """
    )
    // @formatter:on
    @Test
    void withdrawnBikeSignallingFaultStaysWithdrawnOnce() {
        // given
        var b17 = inSessionFor(ANNA, "B-17", at("07:30"));
        b17.returnAtStation(CENTRAL).by(ANNA).reporting(null).at(at("08:00")).using(FakeBikeLocks.failing());

        // when
        var events = b17.signalFault("BRAKE_SENSOR").at(at("09:00"));

        // then
        assertThat(all(events)).isEmpty();
        assertThatThrownBy(() -> b17.reserveForRide(BEN).standingAt(near(CENTRAL, 5)).given(ELIGIBLE).at(at("09:05")))
            .isInstanceOf(BikeNotAvailable.class);
    }
}
