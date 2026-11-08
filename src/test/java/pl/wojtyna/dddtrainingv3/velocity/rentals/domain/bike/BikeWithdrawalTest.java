package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeEvent.BikeWithdrawn;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeEvent.RentalSessionEnded;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeEvent.ReservationCancelled;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeFixture.*;

/// Spec `fleet-operations-takes-broken-bike-out-of-circulation` (Rentals half of R2): a client asks Rentals to withdraw a bike.
class BikeWithdrawalTest {

    // @formatter:off
    @DisplayName(
        """
         given B-17 is available at Central and nobody holds it,
         when Fleet Maintenance asks Rentals to withdraw B-17 at 10:00,
         then B-17 is withdrawn with reason requested (Bike Withdrawn)
        """
    )
    // @formatter:on
    @Test
    void freeAvailableBikeIsWithdrawnOnRequest() {
        // given
        var b17 = availableAt("B-17", CENTRAL);

        // when
        var events = b17.withdraw(at("10:00"));

        // then
        assertThat(all(events)).containsExactly(new BikeWithdrawn(new BikeId("B-17"), WithdrawalReason.REQUESTED, null, at("10:00")));
        assertThatThrownBy(() -> b17.reserveForRide(ANNA).standingAt(near(CENTRAL, 5)).given(ELIGIBLE).at(at("10:01")))
            .isInstanceOf(BikeNotAvailable.class);
    }

    // @formatter:off
    @DisplayName(
        """
         given B-17 is reserved for rider Ben until 10:01,
         when Fleet Maintenance asks Rentals to withdraw B-17 at 10:00,
         then Ben's reservation is cancelled and B-17 is withdrawn with reason requested
        """
    )
    // @formatter:on
    @Test
    void withdrawingReservedBikeCancelsReservation() {
        // given
        var b17 = reservedFor(BEN, "B-17", at("09:59"));

        // when
        var events = b17.withdraw(at("10:00"));

        // then
        assertThat(all(events)).containsExactly(
            new ReservationCancelled(new BikeId("B-17"), BEN, at("10:00")),
            new BikeWithdrawn(new BikeId("B-17"), WithdrawalReason.REQUESTED, null, at("10:00")));
        assertThat(b17.isHeldBy(BEN, at("10:00"))).isFalse();
    }

    // @formatter:off
    @DisplayName(
        """
         given Anna's rental session on B-17 started at 10:00,
         when Fleet Maintenance asks Rentals to withdraw B-17 at 10:10,
         then B-17 is withdrawn at once, Anna's ride goes on until she returns B-17, and B-17 isn't available afterwards (DE-86)
        """
    )
    // @formatter:on
    @Test
    void withdrawingBikeInRideLetsRideGoOn() {
        // given
        var b17 = inSessionFor(ANNA, "B-17", at("10:00"));

        // when
        var events = b17.withdraw(at("10:10"));

        // then
        assertThat(all(events)).containsExactly(new BikeWithdrawn(new BikeId("B-17"), WithdrawalReason.REQUESTED, null, at("10:10")));
        assertThat(b17.isHeldBy(ANNA, at("10:20"))).isTrue();
        var returned = b17.returnAtStation(OLD_TOWN).by(ANNA).reporting(null).at(at("10:30")).using(FakeBikeLocks.working());
        assertThat(eventsOf(returned, RentalSessionEnded.class)).hasSize(1);
        assertThat(eventsOf(returned, BikeWithdrawn.class)).isEmpty();
        assertThatThrownBy(() -> b17.reserveForRide(BEN).standingAt(near(OLD_TOWN, 5)).given(ELIGIBLE).at(at("10:31")))
            .isInstanceOf(BikeNotAvailable.class);
    }

    // @formatter:off
    @DisplayName(
        """
         given B-17 was withdrawn at 09:00,
         when Fleet Maintenance asks Rentals to withdraw B-17 again at 10:00,
         then nothing changes
        """
    )
    // @formatter:on
    @Test
    void withdrawingWithdrawnBikeChangesNothing() {
        // given
        var b17 = withdrawnAt("B-17", CENTRAL, at("09:00"));

        // when
        var events = b17.withdraw(at("10:00"));

        // then
        assertThat(all(events)).isEmpty();
    }
}
