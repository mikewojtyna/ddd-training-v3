package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeFixture.*;

/// Spec `rider-cancels-reservation`.
class BikeCancellationTest {

    @Test
    @DisplayName("cancelReservationOf sets the hold to Free and keeps the version")
    void cancelReservationOfSetsHoldToFree() {
        // given
        var b17 = reservedFor(ANNA, "B-17", at("10:00"));
        var versionBefore = b17.version();

        // when
        b17.cancelReservationOf(ANNA).at(at("10:01"));

        // then
        assertThat(b17.snapshot().hold()).isEqualTo(new Bike.Hold.Free());
        assertThat(b17.version()).isEqualTo(versionBefore);
    }
}
