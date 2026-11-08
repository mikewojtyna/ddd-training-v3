package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeEvent.BikeReserved;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.RentalEligibility.Eligibility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeFixture.*;

/// Spec `rider-reserves-bike`.
class BikeReservationTest {

    // @formatter:off
    @DisplayName(
        """
         given bike B-17 is available at station Central and rider Anna is eligible to rent,
         when Anna, standing 20 m from B-17, reserves it at 10:00,
         then B-17 is reserved for Anna until 10:02 (Bike Reserved)
        """
    )
    // @formatter:on
    @Test
    void eligibleRiderNearbyReservesAvailableBikeForTwoMinutes() {
        // given
        var b17 = availableAt("B-17", CENTRAL);

        // when
        var events = b17.reserveForRide(ANNA).standingAt(near(CENTRAL, 20)).given(ELIGIBLE).at(at("10:00"));

        // then
        assertThat(all(events)).containsExactly(new BikeReserved(new BikeId("B-17"), ANNA, at("10:00"), at("10:02")));
        assertThat(b17.isHeldBy(ANNA, at("10:02"))).isTrue();
    }

    // @formatter:off
    @DisplayName(
        """
         given B-17 is available at Central and Anna is eligible to rent,
         when Anna, standing 400 m from B-17, reserves it,
         then the reservation is rejected and B-17 stays available
        """
    )
    // @formatter:on
    @Test
    void riderFarAwayIsRejected() {
        // given
        var b17 = availableAt("B-17", CENTRAL);

        // when / then
        assertThatThrownBy(() -> b17.reserveForRide(ANNA).standingAt(near(CENTRAL, 400)).given(ELIGIBLE).at(at("10:00")))
            .isInstanceOf(TooFarFromBike.class);
        assertThat(b17.reserveForRide(BEN).standingAt(near(CENTRAL, 5)).given(ELIGIBLE).at(at("10:00")).hasOccurredEventOfType(BikeReserved.class)).isTrue();
    }

    // @formatter:off
    @DisplayName(
        """
         given B-17 is available at Central and Anna is eligible to rent,
         when Anna, standing exactly 50 m from B-17, reserves it at 10:00,
         then B-17 is reserved for Anna until 10:02 (Bike Reserved)
        """
    )
    // @formatter:on
    @Test
    void riderExactlyFiftyMetresAwayReserves() {
        // given
        var b17 = availableAt("B-17", CENTRAL);

        // when
        var events = b17.reserveForRide(ANNA).standingAt(near(CENTRAL, 50)).given(ELIGIBLE).at(at("10:00"));

        // then
        assertThat(all(events)).containsExactly(new BikeReserved(new BikeId("B-17"), ANNA, at("10:00"), at("10:02")));
    }

    // @formatter:off
    @DisplayName(
        """
         given rider Ben hasn't finished onboarding, or Carla is blocked (Accounts answers: not eligible),
         when either of them, standing 10 m from available B-17, reserves it,
         then the reservation is rejected
        """
    )
    // @formatter:on
    @Test
    void ineligibleRiderIsRejected() {
        // given
        var b17 = availableAt("B-17", CENTRAL);
        var notEligible = new RiderStanding(Eligibility.NOT_ELIGIBLE, false);

        // when / then
        assertThatThrownBy(() -> b17.reserveForRide(BEN).standingAt(near(CENTRAL, 10)).given(notEligible).at(at("10:00")))
            .isInstanceOf(RiderNotEligible.class);
        assertThat(b17.isHeldBy(BEN, at("10:00"))).isFalse();
    }

    // @formatter:off
    @DisplayName(
        """
         given Anna holds a reservation for B-17 until 10:02,
         when Anna, standing 15 m from available B-22, reserves B-22 at 10:01,
         then the reservation is rejected and B-22 stays available
        """
    )
    // @formatter:on
    @Test
    void riderHoldingAReservationCannotReserveAnotherBike() {
        // given
        var b17 = reservedFor(ANNA, "B-17", at("10:00"));
        var b22 = availableAt("B-22", CENTRAL);
        var standing = new RiderStanding(Eligibility.ELIGIBLE, b17.isHeldBy(ANNA, at("10:01")));

        // when / then
        assertThatThrownBy(() -> b22.reserveForRide(ANNA).standingAt(near(CENTRAL, 15)).given(standing).at(at("10:01")))
            .isInstanceOf(OneRiderOneBike.class);
        assertThat(b22.isHeldBy(ANNA, at("10:01"))).isFalse();
    }

    // @formatter:off
    @DisplayName(
        """
         given Anna's ride on B-17 started at 09:30 and is still running,
         when Anna, standing 15 m from available B-22, reserves B-22 at 10:00,
         then the reservation is rejected
        """
    )
    // @formatter:on
    @Test
    void riderInRunningRideCannotReserve() {
        // given
        var b17 = inSessionFor(ANNA, "B-17", at("09:30"));
        var b22 = availableAt("B-22", CENTRAL);
        var standing = new RiderStanding(Eligibility.ELIGIBLE, b17.isHeldBy(ANNA, at("10:00")));

        // when / then
        assertThatThrownBy(() -> b22.reserveForRide(ANNA).standingAt(near(CENTRAL, 15)).given(standing).at(at("10:00")))
            .isInstanceOf(OneRiderOneBike.class);
    }

    // @formatter:off
    @DisplayName(
        """
         given B-17 is reserved for Anna until 10:02,
         when eligible rider Ben, standing 5 m from B-17, reserves it at 10:01,
         then the reservation is rejected and B-17 stays reserved for Anna
        """
    )
    // @formatter:on
    @Test
    void bikeReservedForSomeoneElseIsRejected() {
        // given
        var b17 = reservedFor(ANNA, "B-17", at("10:00"));

        // when / then
        assertThatThrownBy(() -> b17.reserveForRide(BEN).standingAt(near(CENTRAL, 5)).given(ELIGIBLE).at(at("10:01")))
            .isInstanceOf(BikeNotAvailable.class);
        assertThat(b17.isHeldBy(ANNA, at("10:01"))).isTrue();
    }

    // @formatter:off
    @DisplayName(
        """
         given B-17 is broken and withdrawn from riders at Central,
         when eligible Anna, standing 20 m from B-17, reserves it,
         then the reservation is rejected
        """
    )
    // @formatter:on
    @Test
    void withdrawnBikeIsRejected() {
        // given
        var b17 = withdrawnAt("B-17", CENTRAL, at("09:00"));

        // when / then
        assertThatThrownBy(() -> b17.reserveForRide(ANNA).standingAt(near(CENTRAL, 20)).given(ELIGIBLE).at(at("10:00")))
            .isInstanceOf(BikeNotAvailable.class);
    }
}
