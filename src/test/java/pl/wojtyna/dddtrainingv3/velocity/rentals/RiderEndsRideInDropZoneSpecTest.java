package pl.wojtyna.dddtrainingv3.velocity.rentals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.velocity.common.VelocitySpringTest;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.events.BikeWithdrawn;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.events.RentalSessionEnded;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static pl.wojtyna.dddtrainingv3.velocity.rentals.RentalsDriver.CENTRAL;

/// Acceptance tests of `docs/specs/rider-ends-ride-in-drop-zone.md`. Rentals doesn't judge zones: it publishes the
/// position in `RentalSessionEnded` and Pricing decides the return fee.
@VelocitySpringTest
@DisplayName("Spec rider-ends-ride-in-drop-zone")
class RiderEndsRideInDropZoneSpecTest extends RentalsSpecTestBase {

    private static final Instant STARTED = Instant.parse("2026-11-08T10:01:00Z");
    private static final Instant ENDED = Instant.parse("2026-11-08T10:30:00Z");

    // @formatter:off
    @DisplayName(
        """
         E1.1 given Anna's rental session on B-17 started at 10:01,
         when Anna ends the ride at position (52.2297, 21.0122), inside a drop zone, at 10:30 and the lock closes,
         then the session ended at 10:30 at (52.2297, 21.0122) with end reason free-floating return and B-17 is available there
        """
    )
    // @formatter:on
    @Test
    void e1_1_ride_ends_at_position_in_drop_zone() {
        // given
        at("2026-11-08T10:01:00Z");
        var anna = rider("anna");
        var b17 = bike("B-17");
        var session = rentals.riding(b17, CENTRAL, anna);

        // when
        at("2026-11-08T10:30:00Z");
        var ended = rentals.endRideHere(b17, anna, 52.2297, 21.0122);

        // then
        assertThat(ended.status()).isEqualTo(200);
        assertThat(ended.body()).isEqualTo(new RentalsDriver.SessionEnd(session, b17, "FREE_FLOATING_RETURN", ENDED));
        var bike = rentals.bike(b17).body();
        assertThat(bike.availability()).isEqualTo("AVAILABLE");
        assertThat(bike.stationId()).isNull();
        assertThat(bike.latitude()).isEqualByComparingTo("52.2297");
        assertThat(bike.longitude()).isEqualByComparingTo("21.0122");
        await().atMost(HAND_OFF).untilAsserted(() -> assertThat(rentals.published(RentalSessionEnded.class, event -> event.bikeId().equals(b17)))
            .containsExactly(new RentalSessionEnded(session, b17, anna, STARTED, ENDED, "FREE_FLOATING_RETURN", null,
                                                    BigDecimal.valueOf(52.2297), BigDecimal.valueOf(21.0122))));
    }

    // @formatter:off
    @DisplayName(
        """
         E1.2 given Anna's rental session on B-17 started at 10:01,
         when Anna ends the ride at (52.2500, 21.1000), in an unauthorized zone, at 10:30 and the lock closes,
         then the session ended at 10:30 at (52.2500, 21.1000) with end reason free-floating return
        """
    )
    // @formatter:on
    @Test
    void e1_2_ride_ends_at_position_in_unauthorized_zone() {
        // given
        at("2026-11-08T10:01:00Z");
        var anna = rider("anna");
        var b17 = bike("B-17");
        rentals.riding(b17, CENTRAL, anna);

        // when
        at("2026-11-08T10:30:00Z");
        var ended = rentals.endRideHere(b17, anna, 52.2500, 21.1000);

        // then
        assertThat(ended.body().endReason()).isEqualTo("FREE_FLOATING_RETURN");
        await().atMost(HAND_OFF).untilAsserted(() -> assertThat(rentals.published(RentalSessionEnded.class, event -> event.bikeId().equals(b17)))
            .singleElement().satisfies(event -> {
                assertThat(event.latitude()).isEqualByComparingTo("52.25");
                assertThat(event.longitude()).isEqualByComparingTo("21.1");
                assertThat(event.endedAt()).isEqualTo(ENDED);
            }));
    }

    // @formatter:off
    @DisplayName(
        """
         E2.1 given Anna's rental session on B-17 started at 10:01,
         when Anna ends the ride at (52.2297, 21.0122) at 10:30 and the lock doesn't close,
         then the session ended at 10:30 at (52.2297, 21.0122) with end reason lock failure, Anna is told why,
         and B-17 is withdrawn with reason lock failed (Bike Withdrawn)
        """
    )
    // @formatter:on
    @Test
    void e2_1_failed_lock_ends_ride_and_withdraws_bike() {
        // given
        at("2026-11-08T10:01:00Z");
        var anna = rider("anna");
        var b17 = bike("B-17");
        var session = rentals.riding(b17, CENTRAL, anna);
        locks.registerFaulty(b17);

        // when
        at("2026-11-08T10:30:00Z");
        var ended = rentals.endRideHere(b17, anna, 52.2297, 21.0122);

        // then
        assertThat(ended.body()).isEqualTo(new RentalsDriver.SessionEnd(session, b17, "LOCK_FAILURE", ENDED));
        assertThat(rentals.bike(b17).body().availability()).isEqualTo("WITHDRAWN");
        await().atMost(HAND_OFF).untilAsserted(() -> {
            assertThat(rentals.published(RentalSessionEnded.class, event -> event.bikeId().equals(b17)))
                .singleElement().satisfies(event -> {
                    assertThat(event.endReason()).isEqualTo("LOCK_FAILURE");
                    assertThat(event.latitude()).isEqualByComparingTo("52.2297");
                });
            assertThat(rentals.published(BikeWithdrawn.class, event -> event.bikeId().equals(b17)))
                .containsExactly(new BikeWithdrawn(b17, "LOCK_FAILED", ENDED, null));
        });
    }

    // @formatter:off
    @DisplayName(
        """
         E3.1 given Anna's rental session on B-17 is running,
         when rider Ben ends the ride on B-17 at (52.2297, 21.0122),
         then it is rejected and Anna's session keeps running
        """
    )
    // @formatter:on
    @Test
    void e3_1_other_rider_cannot_end_the_ride() {
        // given
        at("2026-11-08T10:01:00Z");
        var anna = rider("anna");
        var ben = rider("ben");
        var b17 = bike("B-17");
        rentals.riding(b17, CENTRAL, anna);

        // when
        at("2026-11-08T10:30:00Z");
        var ended = rentals.endRideHere(b17, ben, 52.2297, 21.0122);

        // then
        assertThat(ended.status()).isEqualTo(409);
        assertThat(ended.error()).isEqualTo("NoRunningSession");
        assertThat(rentals.activeRentalOf(anna).body().kind()).isEqualTo("SESSION");
    }

    // @formatter:off
    @DisplayName(
        """
         E3.2 given Anna has no running rental session,
         when Anna ends a ride on B-22 at (52.2297, 21.0122),
         then it is rejected
        """
    )
    // @formatter:on
    @Test
    void e3_2_ending_without_running_session_is_rejected() {
        // given
        at("2026-11-08T10:01:00Z");
        var anna = rider("anna");
        var b22 = rentals.bikeAt(bike("B-22"), CENTRAL);

        // when
        var ended = rentals.endRideHere(b22, anna, 52.2297, 21.0122);

        // then
        assertThat(ended.status()).isEqualTo(409);
        assertThat(ended.error()).isEqualTo("NoRunningSession");
    }
}
