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
import static pl.wojtyna.dddtrainingv3.velocity.rentals.RentalsDriver.OLD_TOWN;

/// Acceptance tests of `docs/specs/rider-returns-bike-at-station.md`. Sessions are reached over REST (add, reserve,
/// unlock); the published `RentalSessionEnded` / `BikeWithdrawn` (for Settlement and Fleet Maintenance) are awaited.
@VelocitySpringTest
@DisplayName("Spec rider-returns-bike-at-station")
class RiderReturnsBikeAtStationSpecTest extends RentalsSpecTestBase {

    private static final Instant STARTED = Instant.parse("2026-11-08T10:01:00Z");
    private static final Instant RETURNED = Instant.parse("2026-11-08T10:40:00Z");

    // @formatter:off
    @DisplayName(
        """
         E1.1 given Anna's rental session on B-17 started at 10:01,
         when Anna returns B-17 at station Old Town at 10:40 and the lock closes,
         then the session ended at 10:40 at Old Town with end reason station return (Rental Session Ended) and B-17 is available at Old Town
        """
    )
    // @formatter:on
    @Test
    void e1_1_station_return_with_closed_lock_ends_ride_at_station() {
        // given
        at("2026-11-08T10:01:00Z");
        var anna = rider("anna");
        var b17 = bike("B-17");
        var session = rentals.riding(b17, CENTRAL, anna);

        // when
        at("2026-11-08T10:40:00Z");
        var returned = rentals.returnAtStation(b17, anna, "Old Town");

        // then
        assertThat(returned.status()).isEqualTo(200);
        assertThat(returned.body()).isEqualTo(new RentalsDriver.SessionEnd(session, b17, "STATION_RETURN", RETURNED));
        var bike = rentals.bike(b17).body();
        assertThat(bike.availability()).isEqualTo("AVAILABLE");
        assertThat(bike.hold()).isEqualTo("FREE");
        assertThat(bike.stationId()).isEqualTo("Old Town");
        assertThat(rentals.activeRentalOf(anna).status()).isEqualTo(404);
        await().atMost(HAND_OFF).untilAsserted(() -> assertThat(rentals.published(RentalSessionEnded.class, event -> event.bikeId().equals(b17)))
            .containsExactly(new RentalSessionEnded(session, b17, anna, STARTED, RETURNED, "STATION_RETURN", "Old Town",
                                                    BigDecimal.valueOf(52.249), BigDecimal.valueOf(21.0122))));
        assertThat(rentals.published(BikeWithdrawn.class, event -> event.bikeId().equals(b17))).isEmpty();
    }

    // @formatter:off
    @DisplayName(
        """
         E2.1 given Anna's rental session on B-17 started at 10:01,
         when Anna returns B-17 at Old Town at 10:40 and the lock doesn't close,
         then the session ended at 10:40 at Old Town with end reason lock failure, Anna is told the ride ended because the lock failed,
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
        at("2026-11-08T10:40:00Z");
        var returned = rentals.returnAtStation(b17, anna, "Old Town");

        // then
        assertThat(returned.body()).isEqualTo(new RentalsDriver.SessionEnd(session, b17, "LOCK_FAILURE", RETURNED));
        assertThat(rentals.bike(b17).body().availability()).isEqualTo("WITHDRAWN");
        await().atMost(HAND_OFF).untilAsserted(() -> {
            assertThat(rentals.published(RentalSessionEnded.class, event -> event.bikeId().equals(b17)))
                .singleElement().satisfies(event -> {
                    assertThat(event.endReason()).isEqualTo("LOCK_FAILURE");
                    assertThat(event.stationId()).isEqualTo("Old Town");
                });
            assertThat(rentals.published(BikeWithdrawn.class, event -> event.bikeId().equals(b17)))
                .containsExactly(new BikeWithdrawn(b17, "LOCK_FAILED", RETURNED, null));
        });
    }

    // @formatter:off
    @DisplayName(
        """
         E3.1 given Anna's rental session on B-17 started at 10:01,
         when Anna returns B-17 at Old Town at 10:40 reporting "chain slips" and the lock closes,
         then the session ended at 10:40 at Old Town with end reason station return
         and B-17 is withdrawn with reason fault reported at return (Bike Withdrawn)
        """
    )
    // @formatter:on
    @Test
    void e3_1_reported_fault_ends_ride_normally_and_withdraws_bike() {
        // given
        at("2026-11-08T10:01:00Z");
        var anna = rider("anna");
        var b17 = bike("B-17");
        var session = rentals.riding(b17, CENTRAL, anna);

        // when
        at("2026-11-08T10:40:00Z");
        var returned = rentals.returnAtStationReporting(b17, anna, "Old Town", "chain slips");

        // then
        assertThat(returned.body()).isEqualTo(new RentalsDriver.SessionEnd(session, b17, "STATION_RETURN", RETURNED));
        assertThat(rentals.bike(b17).body().availability()).isEqualTo("WITHDRAWN");
        await().atMost(HAND_OFF).untilAsserted(() -> assertThat(rentals.published(BikeWithdrawn.class, event -> event.bikeId().equals(b17)))
            .containsExactly(new BikeWithdrawn(b17, "FAULT_REPORTED_AT_RETURN", RETURNED, "chain slips")));
    }

    // @formatter:off
    @DisplayName(
        """
         E4.1 given Anna's rental session on B-17 started at 10:01,
         when Anna returns B-17 at 10:40 naming station Central and the lock closes,
         then the session ended at Central, without checking Anna's position
        """
    )
    // @formatter:on
    @Test
    void e4_1_ride_ends_at_named_station_without_position_check() {
        // given
        at("2026-11-08T10:01:00Z");
        var anna = rider("anna");
        var b17 = bike("B-17");
        rentals.riding(b17, OLD_TOWN, anna);

        // when
        at("2026-11-08T10:40:00Z");
        var returned = rentals.returnAtStation(b17, anna, "Central");

        // then
        assertThat(returned.body().endReason()).isEqualTo("STATION_RETURN");
        assertThat(rentals.bike(b17).body().stationId()).isEqualTo("Central");
        await().atMost(HAND_OFF).untilAsserted(() -> assertThat(rentals.published(RentalSessionEnded.class, event -> event.bikeId().equals(b17)))
            .singleElement().extracting(RentalSessionEnded::stationId).isEqualTo("Central"));
    }

    // @formatter:off
    @DisplayName(
        """
         E4.2 given Anna's rental session on B-17 is running,
         when Anna returns B-17 naming "Harbour Café", which is not a station,
         then the return is rejected and the session keeps running
        """
    )
    // @formatter:on
    @Test
    void e4_2_return_at_unknown_station_is_rejected() {
        // given
        at("2026-11-08T10:01:00Z");
        var anna = rider("anna");
        var b17 = bike("B-17");
        rentals.riding(b17, CENTRAL, anna);

        // when
        at("2026-11-08T10:40:00Z");
        var returned = rentals.returnAtStation(b17, anna, "Harbour Café");

        // then
        assertThat(returned.status()).isEqualTo(409);
        assertThat(returned.error()).isEqualTo("UnknownStation");
        assertThat(rentals.activeRentalOf(anna).body().kind()).isEqualTo("SESSION");
    }

    // @formatter:off
    @DisplayName(
        """
         E5.1 given Anna's rental session on B-17 is running,
         when rider Ben returns B-17 at Old Town,
         then the return is rejected and Anna's session keeps running
        """
    )
    // @formatter:on
    @Test
    void e5_1_other_rider_cannot_return_the_bike() {
        // given
        at("2026-11-08T10:01:00Z");
        var anna = rider("anna");
        var ben = rider("ben");
        var b17 = bike("B-17");
        rentals.riding(b17, CENTRAL, anna);

        // when
        at("2026-11-08T10:40:00Z");
        var returned = rentals.returnAtStation(b17, ben, "Old Town");

        // then
        assertThat(returned.status()).isEqualTo(409);
        assertThat(returned.error()).isEqualTo("NoRunningSession");
        assertThat(rentals.activeRentalOf(anna).body().kind()).isEqualTo("SESSION");
    }

    // @formatter:off
    @DisplayName(
        """
         E5.2 given Anna has no running rental session,
         when Anna returns B-22 at Old Town,
         then the return is rejected
        """
    )
    // @formatter:on
    @Test
    void e5_2_return_without_running_session_is_rejected() {
        // given
        at("2026-11-08T10:01:00Z");
        var anna = rider("anna");
        var b22 = rentals.bikeAt(bike("B-22"), CENTRAL);

        // when
        var returned = rentals.returnAtStation(b22, anna, "Old Town");

        // then
        assertThat(returned.status()).isEqualTo(409);
        assertThat(returned.error()).isEqualTo("NoRunningSession");
    }
}
