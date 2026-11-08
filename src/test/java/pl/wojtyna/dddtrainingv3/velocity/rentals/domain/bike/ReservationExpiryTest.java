package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeEvent.BikeReserved;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeEvent.ReservationExpired;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.RentalEligibility.Eligibility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeFixture.*;

/// Specs `fleet-operations-releases-expired-reservations` and `technician-reserves-broken-bike` (R4).
class ReservationExpiryTest {

    // @formatter:off
    @DisplayName(
        """
         given B-17 at station Central was reserved for Anna at 10:00, until 10:02, and Anna hasn't unlocked it,
         when the reservations are checked at 10:02:01,
         then Anna's reservation expired and B-17 is available to anyone (Reservation Expired)
        """
    )
    // @formatter:on
    @Test
    void unusedRiderReservationExpiresAndBikeIsAvailable() {
        // given
        var b17 = reservedFor(ANNA, "B-17", at("10:00"));

        // when
        var events = b17.releaseIfExpired(at("10:02:01"));

        // then
        assertThat(all(events)).containsExactly(new ReservationExpired(new BikeId("B-17"), ANNA, at("10:00"), at("10:02:01")));
        assertThat(b17.isHeldBy(ANNA, at("10:02:01"))).isFalse();
    }

    // @formatter:off
    @DisplayName(
        """
         given B-17 was reserved for Anna at 10:00, until 10:02,
         when the reservations are checked at exactly 10:02:00,
         then B-17 is still reserved for Anna
        """
    )
    // @formatter:on
    @Test
    void riderReservationStillHoldsAtExactEnd() {
        // given
        var b17 = reservedFor(ANNA, "B-17", at("10:00"));

        // when
        var events = b17.releaseIfExpired(at("10:02:00"));

        // then
        assertThat(all(events)).isEmpty();
        assertThat(b17.isHeldBy(ANNA, at("10:02:00"))).isTrue();
    }

    // @formatter:off
    @DisplayName(
        """
         given Anna's reservation of B-17 expired at 10:02:01,
         when rider Ben, standing 10 m from B-17, reserves it at 10:04,
         then B-17 is reserved for Ben until 10:06 (Bike Reserved)
        """
    )
    // @formatter:on
    @Test
    void nextRiderReservesBikeAfterExpiry() {
        // given
        var b17 = reservedFor(ANNA, "B-17", at("10:00"));
        b17.releaseIfExpired(at("10:02:01"));

        // when
        var events = b17.reserveForRide(BEN).standingAt(near(CENTRAL, 10)).given(ELIGIBLE).at(at("10:04"));

        // then
        assertThat(all(events)).containsExactly(new BikeReserved(new BikeId("B-17"), BEN, at("10:04"), at("10:06")));
    }

    // @formatter:off
    @DisplayName(
        """
         given B-17 was reserved for Anna at 10:00 and Anna unlocked it at 10:01, starting her ride,
         when the reservations are checked at 10:03,
         then nothing expires and Anna's ride on B-17 keeps running
        """
    )
    // @formatter:on
    @Test
    void unlockedReservationDoesNotExpire() {
        // given
        var b17 = inSessionFor(ANNA, "B-17", at("10:01"));

        // when
        var events = b17.releaseIfExpired(at("10:03"));

        // then
        assertThat(all(events)).isEmpty();
        assertThat(b17.isHeldBy(ANNA, at("10:03"))).isTrue();
    }

    // @formatter:off
    @DisplayName(
        """
         given Anna's unlock of B-17 failed at 10:01, releasing her reservation,
         when the reservations are checked at 10:03,
         then nothing expires
        """
    )
    // @formatter:on
    @Test
    void reservationReleasedByFailedUnlockDoesNotExpire() {
        // given
        var b17 = reservedFor(ANNA, "B-17", at("10:00"));
        b17.unlockForRide(ANNA).given(Eligibility.ELIGIBLE).at(at("10:01")).using(FakeBikeLocks.failing());

        // when
        var events = b17.releaseIfExpired(at("10:03"));

        // then
        assertThat(all(events)).isEmpty();
    }

    // @formatter:off
    @DisplayName(
        """
         given broken B-31 was reserved for technician Tom at 09:00, until 09:30, and Tom hasn't unlocked it,
         when the reservations are checked at 09:30:01,
         then Tom's reservation expired and B-31 stays withdrawn from riders, waiting for a technician to reserve it again
        """
    )
    // @formatter:on
    @Test
    void unusedTechnicianReservationExpiresAndBikeStaysWithdrawn() {
        // given
        var b31 = reservedForService(TOM, "B-31", at("09:00"));

        // when
        var events = b31.releaseIfExpired(at("09:30:01"));

        // then
        assertThat(all(events)).containsExactly(new ReservationExpired(new BikeId("B-31"), TOM, at("09:00"), at("09:30:01")));
        assertThatThrownBy(() -> b31.reserveForRide(ANNA).standingAt(near(OLD_TOWN, 5)).given(ELIGIBLE).at(at("09:31")))
            .isInstanceOf(BikeNotAvailable.class);
        assertThat(b31.reserveForService(TARA).at(at("09:32")).hasOccurredEventOfType(BikeReserved.class)).isTrue();
    }

    // @formatter:off
    @DisplayName(
        """
         given broken B-31 was reserved for Tom at 09:00,
         when the reservations are checked at 09:29 or at exactly 09:30:00,
         then B-31 is still reserved for Tom
        """
    )
    // @formatter:on
    @Test
    void technicianReservationStillHoldsUntilAndIncludingItsEnd() {
        // given
        var b31 = reservedForService(TOM, "B-31", at("09:00"));

        // when
        var at0929 = b31.releaseIfExpired(at("09:29"));
        var at0930 = b31.releaseIfExpired(at("09:30:00"));

        // then
        assertThat(all(at0929)).isEmpty();
        assertThat(all(at0930)).isEmpty();
        assertThat(b31.isHeldBy(TOM, at("09:30:00"))).isTrue();
    }
}
