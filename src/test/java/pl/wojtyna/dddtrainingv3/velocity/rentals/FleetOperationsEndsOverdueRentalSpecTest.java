package pl.wojtyna.dddtrainingv3.velocity.rentals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pl.wojtyna.dddtrainingv3.velocity.common.VelocitySpringTest;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.events.BikeWithdrawn;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.events.RentalSessionEnded;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases.EndOverdueRentalSessions;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases.EndOverdueRentalSessionsUseCase;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static pl.wojtyna.dddtrainingv3.velocity.rentals.RentalsDriver.CENTRAL;

/// Acceptance tests of `docs/specs/fleet-operations-ends-overdue-rental.md`. "The rides are checked at T" sets the
/// clock to T and calls the use case the scheduled job calls in production.
@VelocitySpringTest
@DisplayName("Spec fleet-operations-ends-overdue-rental")
class FleetOperationsEndsOverdueRentalSpecTest extends RentalsSpecTestBase {

    private static final Instant STARTED = Instant.parse("2026-11-09T08:00:00Z");
    private static final Instant TWELVE_HOUR_MARK = Instant.parse("2026-11-09T20:00:00Z");

    @Autowired
    private EndOverdueRentalSessionsUseCase endOverdueRentalSessions;

    private void ridesCheckedAt(String isoInstant) {
        at(isoInstant);
        endOverdueRentalSessions.handle(new EndOverdueRentalSessions(Instant.parse(isoInstant)));
    }

    // @formatter:off
    @DisplayName(
        """
         E1.1 given Anna's rental session on B-17 started at 08:00 and is still running,
         when the rides are checked at 20:00:01,
         then the session ended at 20:00 with end reason maximum rental time exceeded and no known place (Rental Session Ended)
        """
    )
    // @formatter:on
    @Test
    void e1_1_session_over_12_hours_ends_at_12_hour_mark() {
        // given
        at("2026-11-09T08:00:00Z");
        var anna = rider("anna");
        var b17 = bike("B-17");
        var session = rentals.riding(b17, CENTRAL, anna);

        // when
        ridesCheckedAt("2026-11-09T20:00:01Z");

        // then
        assertThat(rentals.activeRentalOf(anna).status()).isEqualTo(404);
        await().atMost(HAND_OFF).untilAsserted(() -> assertThat(rentals.published(RentalSessionEnded.class, event -> event.bikeId().equals(b17)))
            .containsExactly(new RentalSessionEnded(session, b17, anna, STARTED, TWELVE_HOUR_MARK, "MAX_RENTAL_TIME_EXCEEDED",
                                                    null, null, null)));
    }

    // @formatter:off
    @DisplayName(
        """
         E1.2 given Anna's rental session on B-17 started at 08:00 and is still running,
         when the rides are checked at exactly 20:00:00,
         then the session is still running
        """
    )
    // @formatter:on
    @Test
    void e1_2_session_of_exactly_12_hours_still_runs() {
        // given
        at("2026-11-09T08:00:00Z");
        var anna = rider("anna");
        var b17 = bike("B-17");
        rentals.riding(b17, CENTRAL, anna);

        // when
        ridesCheckedAt("2026-11-09T20:00:00Z");

        // then
        assertThat(rentals.activeRentalOf(anna).body().kind()).isEqualTo("SESSION");
        assertThat(rentals.published(RentalSessionEnded.class, event -> event.bikeId().equals(b17))).isEmpty();
    }

    // @formatter:off
    @DisplayName(
        """
         E1.3 given Anna's rental session on B-17 started at 08:00 and is still running,
         when the rides are checked only at 22:15,
         then the session ended at 20:00 with end reason maximum rental time exceeded
        """
    )
    // @formatter:on
    @Test
    void e1_3_late_check_still_ends_session_at_12_hour_mark() {
        // given
        at("2026-11-09T08:00:00Z");
        var anna = rider("anna");
        var b17 = bike("B-17");
        rentals.riding(b17, CENTRAL, anna);

        // when
        ridesCheckedAt("2026-11-09T22:15:00Z");

        // then
        await().atMost(HAND_OFF).untilAsserted(() -> assertThat(rentals.published(RentalSessionEnded.class, event -> event.bikeId().equals(b17)))
            .singleElement().satisfies(event -> {
                assertThat(event.endedAt()).isEqualTo(TWELVE_HOUR_MARK);
                assertThat(event.endReason()).isEqualTo("MAX_RENTAL_TIME_EXCEEDED");
            }));
    }

    // @formatter:off
    @DisplayName(
        """
         E2.1 given Anna's rental session on B-17 started at 08:00,
         when the rides are checked at 20:00:01,
         then B-17 is withdrawn with reason not returned (Bike Withdrawn) and isn't offered to riders
        """
    )
    // @formatter:on
    @Test
    void e2_1_bike_of_overdue_session_is_withdrawn_as_not_returned() {
        // given
        at("2026-11-09T08:00:00Z");
        var anna = rider("anna");
        var b17 = bike("B-17");
        rentals.riding(b17, CENTRAL, anna);

        // when
        ridesCheckedAt("2026-11-09T20:00:01Z");

        // then
        var bike = rentals.bike(b17).body();
        assertThat(bike.availability()).isEqualTo("WITHDRAWN");
        assertThat(bike.latitude()).isNull();
        assertThat(bike.longitude()).isNull();
        await().atMost(HAND_OFF).untilAsserted(() -> assertThat(rentals.published(BikeWithdrawn.class, event -> event.bikeId().equals(b17)))
            .containsExactly(new BikeWithdrawn(b17, "NOT_RETURNED", Instant.parse("2026-11-09T20:00:01Z"), null)));
    }

    // @formatter:off
    @DisplayName(
        """
         E3.1 given Anna's rental session on B-17 started at 08:00 and she returned B-17 at station Old Town at 19:59,
         when the rides are checked at 20:00:01,
         then nothing happens: the ride stays ended at Old Town at 19:59 and B-17 stays available
        """
    )
    // @formatter:on
    @Test
    void e3_1_session_returned_in_time_is_left_alone() {
        // given
        at("2026-11-09T08:00:00Z");
        var anna = rider("anna");
        var b17 = bike("B-17");
        rentals.riding(b17, CENTRAL, anna);
        at("2026-11-09T19:59:00Z");
        rentals.returnAtStation(b17, anna, "Old Town");

        // when
        ridesCheckedAt("2026-11-09T20:00:01Z");

        // then
        var bike = rentals.bike(b17).body();
        assertThat(bike.availability()).isEqualTo("AVAILABLE");
        assertThat(bike.stationId()).isEqualTo("Old Town");
        await().atMost(HAND_OFF).untilAsserted(() -> assertThat(rentals.published(RentalSessionEnded.class, event -> event.bikeId().equals(b17)))
            .singleElement().satisfies(event -> {
                assertThat(event.endedAt()).isEqualTo(Instant.parse("2026-11-09T19:59:00Z"));
                assertThat(event.endReason()).isEqualTo("STATION_RETURN");
            }));
        assertThat(rentals.published(BikeWithdrawn.class, event -> event.bikeId().equals(b17))).isEmpty();
    }
}
