package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeEvent.BikeMadeAvailable;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeEvent.BikeReserved;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeEvent.BikeUnlocked;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeEvent.UnlockFailed;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeFixture.*;

/// Rentals' side of Fleet Maintenance: specs `technician-reserves-broken-bike`, `technician-starts-maintenance`,
/// `technician-returns-fixed-bike-to-circulation`. Rentals speaks of *withdrawn* bikes; *broken* is Fleet Maintenance's word.
class TechnicianServiceTest {

    @Nested
    class Reserving {

        // @formatter:off
        @DisplayName(
            """
             given bike B-17 is withdrawn at station Old Town and nobody holds it,
             when technician Tom reserves B-17 at 09:00,
             then B-17 is reserved for Tom until 09:30 (Bike Reserved)
            """
        )
        // @formatter:on
        @Test
        void technicianReservesWithdrawnBikeForThirtyMinutes() {
            // given
            var b17 = withdrawnAt("B-17", OLD_TOWN, at("08:00"));

            // when
            var events = b17.reserveForService(TOM).at(at("09:00"));

            // then
            assertThat(all(events)).containsExactly(new BikeReserved(new BikeId("B-17"), TOM, at("09:00"), at("09:30")));
        }

        // @formatter:off
        @DisplayName(
            """
             given B-22 was withdrawn as not returned and its position isn't known,
             when Tom reserves B-22 at 09:00 without giving his own position,
             then B-22 is reserved for Tom until 09:30 (Bike Reserved)
            """
        )
        // @formatter:on
        @Test
        void technicianReservesBikeWithUnknownPosition() {
            // given
            var b22 = inSessionFor(BEN, "B-22", RIDE_STARTED_EVENING_BEFORE);
            b22.endIfOverdue(at("08:00"));

            // when
            var events = b22.reserveForService(TOM).at(at("09:00"));

            // then
            assertThat(all(events)).containsExactly(new BikeReserved(new BikeId("B-22"), TOM, at("09:00"), at("09:30")));
        }

        // @formatter:off
        @DisplayName(
            """
             given B-17 is reserved for Tom until 09:30 and B-22 is withdrawn at Central,
             when Tom reserves B-22 at 09:05,
             then B-22 is reserved for Tom until 09:35 (Bike Reserved), and Tom still holds B-17
            """
        )
        // @formatter:on
        @Test
        void technicianHoldsSeveralReservations() {
            // given
            var b17 = reservedForService(TOM, "B-17", at("09:00"));
            var b22 = withdrawnAt("B-22", CENTRAL, at("08:00"));

            // when
            var events = b22.reserveForService(TOM).at(at("09:05"));

            // then
            assertThat(all(events)).containsExactly(new BikeReserved(new BikeId("B-22"), TOM, at("09:05"), at("09:35")));
            assertThat(b17.isHeldBy(TOM, at("09:05"))).isTrue();
        }

        // @formatter:off
        @DisplayName(
            """
             given bike B-40 is available at Central and isn't withdrawn,
             when Tom reserves B-40 at 09:00,
             then the reservation is rejected and B-40 stays available to riders
            """
        )
        // @formatter:on
        @Test
        void technicianCannotReserveBikeThatIsNotWithdrawn() {
            // given
            var b40 = availableAt("B-40", CENTRAL);

            // when / then
            assertThatThrownBy(() -> b40.reserveForService(TOM).at(at("09:00"))).isInstanceOf(BikeNotWithdrawn.class);
            assertThat(b40.reserveForRide(ANNA).standingAt(near(CENTRAL, 5)).given(ELIGIBLE).at(at("09:01"))
                          .hasOccurredEventOfType(BikeReserved.class)).isTrue();
        }

        // @formatter:off
        @DisplayName(
            """
             given B-17 is reserved for Tom until 09:30,
             when technician Tara reserves B-17 at 09:10,
             then the reservation is rejected and B-17 stays reserved for Tom
            """
        )
        // @formatter:on
        @Test
        void secondTechnicianCannotReserveReservedBike() {
            // given
            var b17 = reservedForService(TOM, "B-17", at("09:00"));

            // when / then
            assertThatThrownBy(() -> b17.reserveForService(TARA).at(at("09:10"))).isInstanceOf(BikeAlreadyHeld.class);
            assertThat(b17.isHeldBy(TOM, at("09:10"))).isTrue();
        }
    }

    @Nested
    class Unlocking {

        // @formatter:off
        @DisplayName(
            """
             given bike B-17 is withdrawn and reserved for technician Tom from 09:00 until 09:30,
             when Tom unlocks B-17 at 09:20 and the lock opens,
             then B-17 is unlocked for Tom (Bike Unlocked) and no rental session starts
            """
        )
        // @formatter:on
        @Test
        void technicianUnlockNeverStartsASession() {
            // given
            var b17 = reservedForService(TOM, "B-17", at("09:00"));

            // when
            var events = b17.unlockForService(TOM).at(at("09:20")).using(FakeBikeLocks.working());

            // then
            assertThat(all(events)).containsExactly(new BikeUnlocked(new BikeId("B-17"), TOM, at("09:20")));
            assertThat(b17.isHeldBy(TOM, at("11:00"))).isTrue();
            assertThat(all(b17.endIfOverdue(at("23:00")))).isEmpty();
        }

        // @formatter:off
        @DisplayName(
            """
             given B-17 is reserved for Tom until 09:30,
             when technician Tara unlocks B-17 at 09:20,
             then the unlock is rejected and B-17 stays reserved for Tom
            """
        )
        // @formatter:on
        @Test
        void otherTechnicianCannotUnlock() {
            // given
            var b17 = reservedForService(TOM, "B-17", at("09:00"));
            var locks = FakeBikeLocks.working();

            // when / then
            assertThatThrownBy(() -> b17.unlockForService(TARA).at(at("09:20")).using(locks)).isInstanceOf(NotReservationHolder.class);
            assertThat(locks.calls()).isEmpty();
            assertThat(b17.isHeldBy(TOM, at("09:20"))).isTrue();
        }

        // @formatter:off
        @DisplayName(
            """
             given Tom's reservation of B-17 expired at 09:30 and B-17 is still withdrawn,
             when Tom unlocks B-17 at 09:35,
             then the unlock is rejected; Tom has to reserve B-17 again
            """
        )
        // @formatter:on
        @Test
        void expiredReservationCannotUnlock() {
            // given
            var b17 = reservedForService(TOM, "B-17", at("09:00"));
            b17.releaseIfExpired(at("09:30:01"));

            // when / then
            assertThatThrownBy(() -> b17.unlockForService(TOM).at(at("09:35")).using(FakeBikeLocks.working()))
                .isInstanceOf(NotReservationHolder.class);
        }

        // @formatter:off
        @DisplayName(
            """
             given B-17 is withdrawn and reserved for Tom until 09:30,
             when Tom unlocks B-17 at 09:20 and the lock doesn't open,
             then Tom's reservation is released (Unlock Failed), B-17 stays withdrawn and any technician can reserve it again
            """
        )
        // @formatter:on
        @Test
        void failedUnlockReleasesReservationAndBikeStaysWithdrawn() {
            // given
            var b17 = reservedForService(TOM, "B-17", at("09:00"));

            // when
            var events = b17.unlockForService(TOM).at(at("09:20")).using(FakeBikeLocks.failing());

            // then
            assertThat(all(events)).containsExactly(new UnlockFailed(new BikeId("B-17"), TOM, at("09:20")));
            assertThatThrownBy(() -> b17.reserveForRide(ANNA).standingAt(near(OLD_TOWN, 5)).given(ELIGIBLE).at(at("09:21")))
                .isInstanceOf(BikeNotAvailable.class);
            assertThat(b17.reserveForService(TARA).at(at("09:22")).hasOccurredEventOfType(BikeReserved.class)).isTrue();
        }
    }

    @Nested
    class MakingAvailable {

        // @formatter:off
        @DisplayName(
            """
             given withdrawn B-17 is unlocked for technician Tom,
             when Fleet Maintenance makes B-17 available at station Riverside at 11:00,
             then B-17 is locked, free and available at Riverside's position (Bike Made Available)
            """
        )
        // @formatter:on
        @Test
        void bikeMadeAvailableAtStation() {
            // given
            var b17 = unlockedForService(TOM, "B-17", at("09:20"));

            // when
            var events = b17.makeAvailableAt(RIVERSIDE).at(at("11:00"));

            // then
            assertThat(all(events)).containsExactly(
                new BikeMadeAvailable(new BikeId("B-17"), RIVERSIDE.id(), RIVERSIDE.position(), at("11:00")));
            assertThat(b17.isHeldBy(TOM, at("11:00"))).isFalse();
            assertThat(b17.reserveForRide(ANNA).standingAt(near(RIVERSIDE, 10)).given(ELIGIBLE).at(at("11:01"))
                          .hasOccurredEventOfType(BikeReserved.class)).isTrue();
        }

        // @formatter:off
        @DisplayName(
            """
             given withdrawn B-22 is unlocked for technician Tom,
             when Fleet Maintenance makes B-22 available at position (52.2297, 21.0122) at 11:00,
             then B-22 is available at that position (Bike Made Available)
            """
        )
        // @formatter:on
        @Test
        void bikeMadeAvailableAtPosition() {
            // given
            var b22 = unlockedForService(TOM, "B-22", at("09:20"));
            var oldTownSquare = new Position(52.2297, 21.0122);

            // when
            var events = b22.makeAvailableAt(oldTownSquare).at(at("11:00"));

            // then
            assertThat(all(events)).containsExactly(new BikeMadeAvailable(new BikeId("B-22"), null, oldTownSquare, at("11:00")));
        }

        // @formatter:off
        @DisplayName(
            """
             given withdrawn B-17 is reserved for technician Tom but not unlocked,
             when Fleet Maintenance makes B-17 available at Central,
             then it is rejected
            """
        )
        // @formatter:on
        @Test
        void reservedButNotUnlockedBikeCannotBeMadeAvailable() {
            // given
            var b17 = reservedForService(TOM, "B-17", at("09:00"));

            // when / then
            assertThatThrownBy(() -> b17.makeAvailableAt(CENTRAL).at(at("09:15"))).isInstanceOf(BikeNotUnlockedForService.class);
        }

        // @formatter:off
        @DisplayName(
            """
             given a rider's session runs on B-17,
             when Fleet Maintenance makes B-17 available at Central,
             then it is rejected
            """
        )
        // @formatter:on
        @Test
        void bikeInRideCannotBeMadeAvailable() {
            // given
            var b17 = inSessionFor(ANNA, "B-17", at("10:00"));
            b17.withdraw(at("10:05"));

            // when / then
            assertThatThrownBy(() -> b17.makeAvailableAt(CENTRAL).at(at("10:10"))).isInstanceOf(BikeNotUnlockedForService.class);
            assertThat(b17.isHeldBy(ANNA, at("10:10"))).isTrue();
        }
    }

    /// Ben's ride on B-22 started the evening before and was ended as overdue at 08:00, so B-22's position is unknown.
    private static final Instant RIDE_STARTED_EVENING_BEFORE = Instant.parse("2026-11-07T19:45:00Z");
}
