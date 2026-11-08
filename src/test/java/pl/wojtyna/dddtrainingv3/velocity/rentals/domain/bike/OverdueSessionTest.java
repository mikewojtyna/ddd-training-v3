package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeEvent.BikeWithdrawn;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeEvent.RentalSessionEnded;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeFixture.*;

/// Spec `fleet-operations-ends-overdue-rental`.
class OverdueSessionTest {

    // @formatter:off
    @DisplayName(
        """
         given Anna's rental session on B-17 started at 08:00 and is still running,
         when the rides are checked at 20:00:01,
         then the session ended at 20:00 with end reason maximum rental time exceeded and no known place (Rental Session Ended)
        """
    )
    // @formatter:on
    @Test
    void sessionOverTwelveHoursEndsAtTwelveHourMark() {
        // given
        var b17 = inSessionFor(ANNA, "B-17", at("08:00"));

        // when
        var events = b17.endIfOverdue(at("20:00:01"));

        // then
        assertThat(eventsOf(events, RentalSessionEnded.class)).singleElement().satisfies(ended -> {
            assertThat(ended.endedAt()).isEqualTo(at("20:00"));
            assertThat(ended.endReason()).isEqualTo(EndReason.MAX_RENTAL_TIME_EXCEEDED);
            assertThat(ended.stationId()).isNull();
            assertThat(ended.position()).isNull();
        });
        assertThat(b17.isHeldBy(ANNA, at("20:00:01"))).isFalse();
    }

    // @formatter:off
    @DisplayName(
        """
         given Anna's rental session on B-17 started at 08:00 and is still running,
         when the rides are checked at exactly 20:00:00,
         then the session is still running
        """
    )
    // @formatter:on
    @Test
    void sessionOfExactlyTwelveHoursStillRuns() {
        // given
        var b17 = inSessionFor(ANNA, "B-17", at("08:00"));

        // when
        var events = b17.endIfOverdue(at("20:00:00"));

        // then
        assertThat(all(events)).isEmpty();
        assertThat(b17.isHeldBy(ANNA, at("20:00:00"))).isTrue();
    }

    // @formatter:off
    @DisplayName(
        """
         given Anna's rental session on B-17 started at 08:00 and is still running,
         when the rides are checked only at 22:15,
         then the session ended at 20:00 with end reason maximum rental time exceeded (Rental Session Ended)
        """
    )
    // @formatter:on
    @Test
    void lateCheckStillEndsSessionAtTwelveHourMark() {
        // given
        var b17 = inSessionFor(ANNA, "B-17", at("08:00"));

        // when
        var events = b17.endIfOverdue(at("22:15"));

        // then
        assertThat(eventsOf(events, RentalSessionEnded.class)).singleElement()
                                                              .extracting(RentalSessionEnded::endedAt)
                                                              .isEqualTo(at("20:00"));
    }

    // @formatter:off
    @DisplayName(
        """
         given Anna's rental session on B-17 started at 08:00,
         when the rides are checked at 20:00:01,
         then B-17 is withdrawn with reason not returned (Bike Withdrawn) and isn't offered to riders
        """
    )
    // @formatter:on
    @Test
    void bikeOfOverdueSessionIsWithdrawnAsNotReturned() {
        // given
        var b17 = inSessionFor(ANNA, "B-17", at("08:00"));

        // when
        var events = b17.endIfOverdue(at("20:00:01"));

        // then
        assertThat(eventsOf(events, BikeWithdrawn.class)).containsExactly(
            new BikeWithdrawn(new BikeId("B-17"), WithdrawalReason.NOT_RETURNED, null, at("20:00:01")));
        assertThatThrownBy(() -> b17.reserveForRide(BEN).standingAt(near(CENTRAL, 5)).given(ELIGIBLE).at(at("20:01")))
            .isInstanceOf(BikeNotAvailable.class);
    }

    // @formatter:off
    @DisplayName(
        """
         given Anna's rental session on B-17 started at 08:00 and she returned B-17 at station Old Town at 19:59,
         when the rides are checked at 20:00:01,
         then nothing happens: the ride stays ended at Old Town at 19:59 and B-17 stays available
        """
    )
    // @formatter:on
    @Test
    void sessionReturnedInTimeIsLeftAlone() {
        // given
        var b17 = inSessionFor(ANNA, "B-17", at("08:00"));
        b17.returnAtStation(OLD_TOWN).by(ANNA).reporting(null).at(at("19:59")).using(FakeBikeLocks.working());

        // when
        var events = b17.endIfOverdue(at("20:00:01"));

        // then
        assertThat(all(events)).isEmpty();
        assertThat(b17.reserveForRide(BEN).standingAt(near(OLD_TOWN, 5)).given(ELIGIBLE).at(at("20:01")).stream()).hasSize(1);
    }
}
