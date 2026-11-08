package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeEvent.BikeReserved;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeEvent.BikeWithdrawn;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeEvent.RentalSessionEnded;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.Station.StationId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeFixture.*;

/// Spec `rider-returns-bike-at-station`.
class StationReturnTest {

    // @formatter:off
    @DisplayName(
        """
         given Anna's rental session on B-17 started at 10:01,
         when Anna returns B-17 at station Old Town at 10:40 and the lock closes,
         then the session ended at 10:40 at Old Town with end reason station return (Rental Session Ended) and B-17 is available at Old Town
        """
    )
    // @formatter:on
    @Test
    void stationReturnWithClosedLockEndsRideAtStation() {
        // given
        var b17 = inSessionFor(ANNA, "B-17", at("10:01"));

        // when
        var events = b17.returnAtStation(OLD_TOWN).by(ANNA).reporting(null).at(at("10:40")).using(FakeBikeLocks.working());

        // then
        assertThat(eventsOf(events, RentalSessionEnded.class)).singleElement().satisfies(ended -> {
            assertThat(ended.rider()).isEqualTo(ANNA);
            assertThat(ended.startedAt()).isEqualTo(at("10:01"));
            assertThat(ended.endedAt()).isEqualTo(at("10:40"));
            assertThat(ended.endReason()).isEqualTo(EndReason.STATION_RETURN);
            assertThat(ended.stationId()).isEqualTo(OLD_TOWN.id());
            assertThat(ended.position()).isEqualTo(OLD_TOWN.position());
        });
        assertThat(eventsOf(events, BikeWithdrawn.class)).isEmpty();
        assertThat(b17.reserveForRide(BEN).standingAt(near(OLD_TOWN, 10)).given(ELIGIBLE).at(at("10:41"))
                      .hasOccurredEventOfType(BikeReserved.class)).isTrue();
    }

    // @formatter:off
    @DisplayName(
        """
         given Anna's rental session on B-17 started at 10:01,
         when Anna returns B-17 at Old Town at 10:40 and the lock doesn't close,
         then the session ended at 10:40 at Old Town with end reason lock failure (Rental Session Ended)
         and B-17 is withdrawn with reason lock failed (Bike Withdrawn)
        """
    )
    // @formatter:on
    @Test
    void failedLockEndsRideAndWithdrawsBike() {
        // given
        var b17 = inSessionFor(ANNA, "B-17", at("10:01"));

        // when
        var events = b17.returnAtStation(OLD_TOWN).by(ANNA).reporting(null).at(at("10:40")).using(FakeBikeLocks.failing());

        // then
        assertThat(eventsOf(events, RentalSessionEnded.class)).singleElement().satisfies(ended -> {
            assertThat(ended.endReason()).isEqualTo(EndReason.LOCK_FAILURE);
            assertThat(ended.endedAt()).isEqualTo(at("10:40"));
            assertThat(ended.stationId()).isEqualTo(OLD_TOWN.id());
        });
        assertThat(eventsOf(events, BikeWithdrawn.class)).containsExactly(
            new BikeWithdrawn(new BikeId("B-17"), WithdrawalReason.LOCK_FAILED, null, at("10:40")));
        assertThatThrownBy(() -> b17.reserveForRide(BEN).standingAt(near(OLD_TOWN, 10)).given(ELIGIBLE).at(at("10:41")))
            .isInstanceOf(BikeNotAvailable.class);
    }

    // @formatter:off
    @DisplayName(
        """
         given Anna's rental session on B-17 started at 10:01,
         when Anna returns B-17 at Old Town at 10:40 reporting "chain slips" and the lock closes,
         then the session ended at 10:40 at Old Town with end reason station return (Rental Session Ended)
         and B-17 is withdrawn with reason fault reported at return (Bike Withdrawn)
        """
    )
    // @formatter:on
    @Test
    void reportedFaultEndsRideNormallyAndWithdrawsBike() {
        // given
        var b17 = inSessionFor(ANNA, "B-17", at("10:01"));

        // when
        var events = b17.returnAtStation(OLD_TOWN).by(ANNA).reporting("chain slips").at(at("10:40")).using(FakeBikeLocks.working());

        // then
        assertThat(eventsOf(events, RentalSessionEnded.class)).singleElement()
                                                              .extracting(RentalSessionEnded::endReason)
                                                              .isEqualTo(EndReason.STATION_RETURN);
        assertThat(eventsOf(events, BikeWithdrawn.class)).containsExactly(
            new BikeWithdrawn(new BikeId("B-17"), WithdrawalReason.FAULT_REPORTED_AT_RETURN, "chain slips", at("10:40")));
    }

    // @formatter:off
    @DisplayName(
        """
         given Anna's rental session on B-17 started at 10:01 (the bike was taken at Central and ridden away),
         when Anna returns B-17 at 10:40 naming station Central and the lock closes,
         then the session ended at Central, without checking Anna's position (Rental Session Ended)
        """
    )
    // @formatter:on
    @Test
    void rideEndsAtNamedStationWithoutPositionCheck() {
        // given
        var b17 = inSessionFor(ANNA, "B-17", at("10:01"));

        // when
        var events = b17.returnAtStation(CENTRAL).by(ANNA).reporting(null).at(at("10:40")).using(FakeBikeLocks.working());

        // then
        assertThat(eventsOf(events, RentalSessionEnded.class)).singleElement()
                                                              .extracting(RentalSessionEnded::stationId)
                                                              .isEqualTo(CENTRAL.id());
    }

    // @formatter:off
    @DisplayName(
        """
         given Anna's rental session on B-17 is running,
         when Anna returns B-17 naming "Harbour Café", which is not a station,
         then the return is rejected and the session keeps running
        """
    )
    // @formatter:on
    @Test
    void returnAtUnknownStationIsRejected() {
        // given
        var b17 = inSessionFor(ANNA, "B-17", at("10:01"));

        // when / then
        assertThatThrownBy(() -> b17.returnAtStation(STATIONS.named(new StationId("Harbour Café")))
                                    .by(ANNA).reporting(null).at(at("10:40")).using(FakeBikeLocks.working()))
            .isInstanceOf(UnknownStation.class);
        assertThat(b17.isHeldBy(ANNA, at("10:40"))).isTrue();
    }

    // @formatter:off
    @DisplayName(
        """
         given Anna's rental session on B-17 is running,
         when rider Ben returns B-17 at Old Town,
         then the return is rejected and Anna's session keeps running
        """
    )
    // @formatter:on
    @Test
    void otherRiderCannotReturnTheBike() {
        // given
        var b17 = inSessionFor(ANNA, "B-17", at("10:01"));
        var locks = FakeBikeLocks.working();

        // when / then
        assertThatThrownBy(() -> b17.returnAtStation(OLD_TOWN).by(BEN).reporting(null).at(at("10:40")).using(locks))
            .isInstanceOf(NoRunningSession.class);
        assertThat(locks.calls()).isEmpty();
        assertThat(b17.isHeldBy(ANNA, at("10:40"))).isTrue();
    }

    // @formatter:off
    @DisplayName(
        """
         given Anna has no running rental session,
         when Anna returns B-22 at Old Town,
         then the return is rejected
        """
    )
    // @formatter:on
    @Test
    void returnWithoutRunningSessionIsRejected() {
        // given
        var b22 = availableAt("B-22", CENTRAL);

        // when / then
        assertThatThrownBy(() -> b22.returnAtStation(OLD_TOWN).by(ANNA).reporting(null).at(at("10:40")).using(FakeBikeLocks.working()))
            .isInstanceOf(NoRunningSession.class);
    }
}
