package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeEvent.BikeReserved;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeEvent.BikeWithdrawn;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeEvent.RentalSessionEnded;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeFixture.*;

/// Spec `rider-ends-ride-in-drop-zone`.
class FreeFloatingReturnTest {

    private static final Position OLD_TOWN_SQUARE = new Position(52.2297, 21.0122);
    private static final Position OUTSKIRTS = new Position(52.2500, 21.1000);

    // @formatter:off
    @DisplayName(
        """
         given Anna's rental session on B-17 started at 10:01,
         when Anna ends the ride at position (52.2297, 21.0122), inside a drop zone, at 10:30 and the lock closes,
         then the session ended at 10:30 at (52.2297, 21.0122) with end reason free-floating return (Rental Session Ended)
         and B-17 is available there
        """
    )
    // @formatter:on
    @Test
    void rideEndsAtPositionInDropZone() {
        // given
        var b17 = inSessionFor(ANNA, "B-17", at("10:01"));

        // when
        var events = b17.endRideHere(OLD_TOWN_SQUARE).by(ANNA).at(at("10:30")).using(FakeBikeLocks.working());

        // then
        assertThat(eventsOf(events, RentalSessionEnded.class)).singleElement().satisfies(ended -> {
            assertThat(ended.endReason()).isEqualTo(EndReason.FREE_FLOATING_RETURN);
            assertThat(ended.endedAt()).isEqualTo(at("10:30"));
            assertThat(ended.position()).isEqualTo(OLD_TOWN_SQUARE);
            assertThat(ended.stationId()).isNull();
        });
        assertThat(b17.reserveForRide(BEN).standingAt(OLD_TOWN_SQUARE.northBy(10)).given(ELIGIBLE).at(at("10:31"))
                      .hasOccurredEventOfType(BikeReserved.class)).isTrue();
    }

    // @formatter:off
    @DisplayName(
        """
         given Anna's rental session on B-17 started at 10:01,
         when Anna ends the ride at (52.2500, 21.1000), in an unauthorized zone, at 10:30 and the lock closes,
         then the session ended at 10:30 at (52.2500, 21.1000) with end reason free-floating return (Rental Session Ended)
        """
    )
    // @formatter:on
    @Test
    void rideEndsAtPositionInUnauthorizedZone() {
        // given
        var b17 = inSessionFor(ANNA, "B-17", at("10:01"));

        // when
        var events = b17.endRideHere(OUTSKIRTS).by(ANNA).at(at("10:30")).using(FakeBikeLocks.working());

        // then
        assertThat(eventsOf(events, RentalSessionEnded.class)).singleElement().satisfies(ended -> {
            assertThat(ended.endReason()).isEqualTo(EndReason.FREE_FLOATING_RETURN);
            assertThat(ended.position()).isEqualTo(OUTSKIRTS);
        });
    }

    // @formatter:off
    @DisplayName(
        """
         given Anna's rental session on B-17 started at 10:01,
         when Anna ends the ride at (52.2297, 21.0122) at 10:30 and the lock doesn't close,
         then the session ended at 10:30 at (52.2297, 21.0122) with end reason lock failure (Rental Session Ended)
         and B-17 is withdrawn with reason lock failed (Bike Withdrawn)
        """
    )
    // @formatter:on
    @Test
    void failedLockEndsRideAndWithdrawsBike() {
        // given
        var b17 = inSessionFor(ANNA, "B-17", at("10:01"));

        // when
        var events = b17.endRideHere(OLD_TOWN_SQUARE).by(ANNA).at(at("10:30")).using(FakeBikeLocks.failing());

        // then
        assertThat(eventsOf(events, RentalSessionEnded.class)).singleElement().satisfies(ended -> {
            assertThat(ended.endReason()).isEqualTo(EndReason.LOCK_FAILURE);
            assertThat(ended.position()).isEqualTo(OLD_TOWN_SQUARE);
        });
        assertThat(eventsOf(events, BikeWithdrawn.class)).containsExactly(
            new BikeWithdrawn(new BikeId("B-17"), WithdrawalReason.LOCK_FAILED, null, at("10:30")));
    }

    // @formatter:off
    @DisplayName(
        """
         given Anna's rental session on B-17 is running,
         when rider Ben ends the ride on B-17 at (52.2297, 21.0122),
         then it is rejected and Anna's session keeps running
        """
    )
    // @formatter:on
    @Test
    void otherRiderCannotEndTheRide() {
        // given
        var b17 = inSessionFor(ANNA, "B-17", at("10:01"));

        // when / then
        assertThatThrownBy(() -> b17.endRideHere(OLD_TOWN_SQUARE).by(BEN).at(at("10:30")).using(FakeBikeLocks.working()))
            .isInstanceOf(NoRunningSession.class);
        assertThat(b17.isHeldBy(ANNA, at("10:30"))).isTrue();
    }

    // @formatter:off
    @DisplayName(
        """
         given Anna has no running rental session,
         when Anna ends a ride on B-22 at (52.2297, 21.0122),
         then it is rejected
        """
    )
    // @formatter:on
    @Test
    void endingWithoutRunningSessionIsRejected() {
        // given
        var b22 = availableAt("B-22", CENTRAL);

        // when / then
        assertThatThrownBy(() -> b22.endRideHere(OLD_TOWN_SQUARE).by(ANNA).at(at("10:30")).using(FakeBikeLocks.working()))
            .isInstanceOf(NoRunningSession.class);
    }
}
