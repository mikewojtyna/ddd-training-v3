package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeEvent.BikeReserved;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeEvent.RentalSessionStarted;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeEvent.UnlockFailed;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.RentalEligibility.Eligibility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeFixture.*;

/// Spec `rider-unlocks-reserved-bike`.
class BikeUnlockTest {

    // @formatter:off
    @DisplayName(
        """
         given B-17 at station Central is reserved for Anna from 10:00 until 10:02,
         when Anna unlocks B-17 at 10:01 and the lock opens,
         then a rental session starts for Anna on B-17 at 10:01 (Rental Session Started) and the reservation is used up
        """
    )
    // @formatter:on
    @Test
    void holderUnlocksAndSessionStarts() {
        // given
        var b17 = reservedFor(ANNA, "B-17", at("10:00"));

        // when
        var events = b17.unlockForRide(ANNA).given(Eligibility.ELIGIBLE).at(at("10:01")).using(FakeBikeLocks.working());

        // then
        var started = eventsOf(events, RentalSessionStarted.class);
        assertThat(started).singleElement().satisfies(event -> {
            assertThat(event.rider()).isEqualTo(ANNA);
            assertThat(event.startedAt()).isEqualTo(at("10:01"));
            assertThat(event.sessionId()).isNotNull();
        });
        assertThat(b17.releaseIfExpired(at("10:03")).stream()).isEmpty();
    }

    // @formatter:off
    @DisplayName(
        """
         given B-17 is reserved for Anna from 10:00 until 10:02,
         when Anna unlocks B-17 at exactly 10:02:00 and the lock opens,
         then a rental session starts for Anna on B-17 at 10:02 (Rental Session Started)
        """
    )
    // @formatter:on
    @Test
    void unlockAtExactEndOfReservationStartsSession() {
        // given
        var b17 = reservedFor(ANNA, "B-17", at("10:00"));

        // when
        var events = b17.unlockForRide(ANNA).given(Eligibility.ELIGIBLE).at(at("10:02:00")).using(FakeBikeLocks.working());

        // then
        assertThat(eventsOf(events, RentalSessionStarted.class)).singleElement()
                                                                .extracting(RentalSessionStarted::startedAt)
                                                                .isEqualTo(at("10:02"));
    }

    // @formatter:off
    @DisplayName(
        """
         given B-17 at Central is reserved for Anna until 10:02,
         when Anna unlocks B-17 at 10:01 and the lock doesn't open,
         then no rental session starts, Anna holds no reservation and B-17 is available to anyone again (Unlock Failed)
        """
    )
    // @formatter:on
    @Test
    void failedUnlockReleasesReservationAndBikeIsAvailable() {
        // given
        var b17 = reservedFor(ANNA, "B-17", at("10:00"));

        // when
        var events = b17.unlockForRide(ANNA).given(Eligibility.ELIGIBLE).at(at("10:01")).using(FakeBikeLocks.failing());

        // then
        assertThat(all(events)).containsExactly(new UnlockFailed(new BikeId("B-17"), ANNA, at("10:01")));
        assertThat(b17.isHeldBy(ANNA, at("10:01"))).isFalse();
        assertThat(b17.reserveForRide(BEN).standingAt(near(CENTRAL, 5)).given(ELIGIBLE).at(at("10:01:10"))
                      .hasOccurredEventOfType(BikeReserved.class)).isTrue();
    }

    // @formatter:off
    @DisplayName(
        """
         given Anna's unlock of B-17 failed at 10:01,
         when Anna, standing 10 m from available B-22, reserves B-22 at 10:01:30,
         then B-22 is reserved for Anna (Bike Reserved)
        """
    )
    // @formatter:on
    @Test
    void afterFailedUnlockRiderReservesAnotherBike() {
        // given
        var b17 = reservedFor(ANNA, "B-17", at("10:00"));
        b17.unlockForRide(ANNA).given(Eligibility.ELIGIBLE).at(at("10:01")).using(FakeBikeLocks.failing());
        var b22 = availableAt("B-22", CENTRAL);
        var standing = new RiderStanding(Eligibility.ELIGIBLE, b17.isHeldBy(ANNA, at("10:01:30")));

        // when
        var events = b22.reserveForRide(ANNA).standingAt(near(CENTRAL, 10)).given(standing).at(at("10:01:30"));

        // then
        assertThat(eventsOf(events, BikeReserved.class)).hasSize(1);
    }

    // @formatter:off
    @DisplayName(
        """
         given B-17 is reserved for Anna until 10:02,
         when rider Ben unlocks B-17 at 10:01,
         then the unlock is rejected, the lock stays closed and B-17 stays reserved for Anna
        """
    )
    // @formatter:on
    @Test
    void otherRiderCannotUnlockReservedBike() {
        // given
        var b17 = reservedFor(ANNA, "B-17", at("10:00"));
        var locks = FakeBikeLocks.working();

        // when / then
        assertThatThrownBy(() -> b17.unlockForRide(BEN).given(Eligibility.ELIGIBLE).at(at("10:01")).using(locks))
            .isInstanceOf(NotReservationHolder.class);
        assertThat(locks.calls()).isEmpty();
        assertThat(b17.isHeldBy(ANNA, at("10:01"))).isTrue();
    }

    // @formatter:off
    @DisplayName(
        """
         given B-22 is available and Anna holds no reservation,
         when Anna unlocks B-22,
         then the unlock is rejected and the lock stays closed
        """
    )
    // @formatter:on
    @Test
    void unlockWithoutReservationIsRejected() {
        // given
        var b22 = availableAt("B-22", CENTRAL);
        var locks = FakeBikeLocks.working();

        // when / then
        assertThatThrownBy(() -> b22.unlockForRide(ANNA).given(Eligibility.ELIGIBLE).at(at("10:01")).using(locks))
            .isInstanceOf(NotReservationHolder.class);
        assertThat(locks.calls()).isEmpty();
    }

    // @formatter:off
    @DisplayName(
        """
         given B-17 was reserved for Anna until 10:02 and the reservation hasn't been released yet,
         when Anna unlocks B-17 at 10:02:30,
         then the unlock is rejected and the lock stays closed
        """
    )
    // @formatter:on
    @Test
    void unlockAfterReservationPeriodIsRejected() {
        // given
        var b17 = reservedFor(ANNA, "B-17", at("10:00"));
        var locks = FakeBikeLocks.working();

        // when / then
        assertThatThrownBy(() -> b17.unlockForRide(ANNA).given(Eligibility.ELIGIBLE).at(at("10:02:30")).using(locks))
            .isInstanceOf(NotReservationHolder.class);
        assertThat(locks.calls()).isEmpty();
    }

    // @formatter:off
    @DisplayName(
        """
         given eligible Anna reserved B-17 at 10:00, until 10:02, and her account was blocked at 10:01,
         when Anna unlocks B-17 at 10:01:30,
         then the unlock is rejected, the lock stays closed, no rental session starts and B-17 stays reserved for Anna
        """
    )
    // @formatter:on
    @Test
    void riderBlockedDuringReservationCannotUnlock() {
        // given
        var b17 = reservedFor(ANNA, "B-17", at("10:00"));
        var locks = FakeBikeLocks.working();

        // when / then
        assertThatThrownBy(() -> b17.unlockForRide(ANNA).given(Eligibility.NOT_ELIGIBLE).at(at("10:01:30")).using(locks))
            .isInstanceOf(RiderNotEligible.class);
        assertThat(locks.calls()).isEmpty();
        assertThat(b17.isHeldBy(ANNA, at("10:02"))).isTrue();
    }
}
