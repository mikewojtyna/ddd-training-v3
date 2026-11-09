package pl.wojtyna.dddtrainingv3.velocity.rentals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pl.wojtyna.dddtrainingv3.velocity.common.VelocitySpringTest;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.events.BikeUnlocked;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.events.BikeWithdrawn;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.events.RentalSessionEnded;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.events.UnlockFailed;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases.EndOverdueRentalSessions;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases.EndOverdueRentalSessionsUseCase;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static pl.wojtyna.dddtrainingv3.velocity.rentals.RentalsDriver.CENTRAL;
import static pl.wojtyna.dddtrainingv3.velocity.rentals.RentalsDriver.OLD_TOWN;
import static pl.wojtyna.dddtrainingv3.velocity.rentals.RentalsDriver.RIVERSIDE;

/// Rentals' half of the Fleet Maintenance intents, over Rentals' REST resources as Fleet Maintenance's ACL will call
/// them: Withdraw Bike (`fleet-operations-takes-broken-bike-out-of-circulation`), Reserve Bike for service
/// (`technician-reserves-broken-bike`), Unlock Bike for service (`technician-starts-maintenance`) and Make Bike
/// Available (`technician-returns-fixed-bike-to-circulation`). The Fleet Maintenance side of those specs is tested by
/// that context's own acceptance tests; here the published events it will consume are asserted.
@VelocitySpringTest
@DisplayName("Rentals' service resources for Fleet Maintenance")
class RentalsServiceForFleetMaintenanceRestTest extends RentalsSpecTestBase {

    @Autowired
    EndOverdueRentalSessionsUseCase endOverdueRentalSessions;

    private String withdrawnBike(String name, RentalsDriver.Place station) {
        var bikeId = rentals.bikeAt(bike(name), station);
        rentals.withdraw(bikeId);
        return bikeId;
    }

    @Nested
    @DisplayName("Withdraw Bike (spec fleet-operations-takes-broken-bike-out-of-circulation, Rentals half of R2)")
    class WithdrawingOnRequest {

        // @formatter:off
        @DisplayName(
            """
             E2.1 given B-17 is available at Central and isn't broken,
             when Fleet Maintenance asks Rentals to withdraw B-17 at 10:00,
             then B-17 is withdrawn with reason requested (Bike Withdrawn)
            """
        )
        // @formatter:on
        @Test
        void e2_1_available_bike_is_withdrawn_with_reason_requested() {
            // given
            at("2026-11-08T09:00:00Z");
            var b17 = rentals.bikeAt(bike("B-17"), CENTRAL);

            // when
            at("2026-11-08T10:00:00Z");
            var withdrawal = rentals.withdraw(b17);

            // then
            assertThat(withdrawal.status()).isEqualTo(200);
            assertThat(rentals.bike(b17).body().availability()).isEqualTo("WITHDRAWN");
            await().atMost(HAND_OFF).untilAsserted(() -> assertThat(rentals.published(BikeWithdrawn.class, event -> event.bikeId().equals(b17)))
                .containsExactly(new BikeWithdrawn(b17, "REQUESTED", Instant.parse("2026-11-08T10:00:00Z"), null)));
        }

        // @formatter:off
        @DisplayName(
            """
             E2.2 given B-17 is reserved for rider Ben until 10:01,
             when Fleet Maintenance asks Rentals to withdraw B-17 at 10:00,
             then Ben's reservation is cancelled and B-17 is withdrawn
            """
        )
        // @formatter:on
        @Test
        void e2_2_withdrawal_cancels_riders_reservation() {
            // given
            at("2026-11-08T09:59:00Z");
            var ben = rider("ben");
            var b17 = bike("B-17");
            rentals.reserved(b17, CENTRAL, ben);

            // when
            at("2026-11-08T10:00:00Z");
            rentals.withdraw(b17);

            // then
            assertThat(rentals.activeRentalOf(ben).status()).isEqualTo(404);
            assertThat(rentals.bike(b17).body().availability()).isEqualTo("WITHDRAWN");
            assertThat(rentals.bike(b17).body().hold()).isEqualTo("FREE");
        }

        // @formatter:off
        @DisplayName(
            """
             E2.3 given Anna's rental session on B-17 started at 10:00,
             when Fleet Maintenance asks Rentals to withdraw B-17 at 10:10,
             then B-17 is withdrawn and Anna's ride goes on until she returns B-17 (DE-86)
            """
        )
        // @formatter:on
        @Test
        void e2_3_withdrawal_during_ride_lets_ride_go_on() {
            // given
            at("2026-11-08T10:00:00Z");
            var anna = rider("anna");
            var b17 = bike("B-17");
            rentals.riding(b17, CENTRAL, anna);

            // when
            at("2026-11-08T10:10:00Z");
            rentals.withdraw(b17);

            // then
            assertThat(rentals.activeRentalOf(anna).body().kind()).isEqualTo("SESSION");
            assertThat(rentals.bike(b17).body().availability()).isEqualTo("WITHDRAWN");
            at("2026-11-08T10:30:00Z");
            assertThat(rentals.returnAtStation(b17, anna, "Old Town").body().endReason()).isEqualTo("STATION_RETURN");
            assertThat(rentals.bike(b17).body().availability()).isEqualTo("WITHDRAWN");
            await().during(Duration.ofMillis(300)).atMost(HAND_OFF)
                   .untilAsserted(() -> assertThat(rentals.published(BikeWithdrawn.class, event -> event.bikeId().equals(b17))).hasSize(1));
        }

        @Test
        @DisplayName("withdrawing an already withdrawn bike changes nothing; an unknown bike answers 404")
        void withdrawing_withdrawn_bike_changes_nothing() {
            // given
            at("2026-11-08T09:00:00Z");
            var b17 = withdrawnBike("B-17", CENTRAL);

            // when
            var again = rentals.withdraw(b17);

            // then
            assertThat(again.status()).isEqualTo(200);
            await().during(Duration.ofMillis(300)).atMost(HAND_OFF)
                   .untilAsserted(() -> assertThat(rentals.published(BikeWithdrawn.class, event -> event.bikeId().equals(b17))).hasSize(1));
            assertThat(rentals.withdraw(bike("B-unknown")).status()).isEqualTo(404);
        }
    }

    @Nested
    @DisplayName("Reserve Bike for service (spec technician-reserves-broken-bike)")
    class ReservingForService {

        // @formatter:off
        @DisplayName(
            """
             E1.1 given bike B-17 is withdrawn at station Old Town and nobody holds it,
             when technician Tom reserves B-17 at 09:00,
             then B-17 is reserved for Tom until 09:30
            """
        )
        // @formatter:on
        @Test
        void e1_1_technician_reserves_withdrawn_bike_for_30_minutes() {
            // given
            at("2026-11-08T08:00:00Z");
            var b17 = withdrawnBike("B-17", OLD_TOWN);
            var tom = "tom-" + suffix;

            // when
            at("2026-11-08T09:00:00Z");
            var reservation = rentals.reserveForService(b17, tom);

            // then
            assertThat(reservation.status()).isEqualTo(201);
            assertThat(reservation.body()).isEqualTo(new RentalsDriver.Reservation(
                b17, "TECHNICIAN", tom, Instant.parse("2026-11-08T09:00:00Z"), Instant.parse("2026-11-08T09:30:00Z")));
            assertThat(rentals.bike(b17).body().holderType()).isEqualTo("TECHNICIAN");
        }

        // @formatter:off
        @DisplayName(
            """
             E1.2 given B-22 was withdrawn as not returned and its position isn't known,
             when Tom reserves B-22 at 09:00 without giving his own position,
             then B-22 is reserved for Tom until 09:30
            """
        )
        // @formatter:on
        @Test
        void e1_2_technician_reserves_bike_with_unknown_position() {
            // given
            at("2026-11-07T20:00:00Z");
            var ben = rider("ben");
            var b22 = bike("B-22");
            rentals.riding(b22, CENTRAL, ben);
            at("2026-11-08T08:30:00Z");
            endOverdueRentalSessions.handle(new EndOverdueRentalSessions(Instant.parse("2026-11-08T08:30:00Z")));
            assertThat(rentals.bike(b22).body().latitude()).isNull();
            var tom = "tom-" + suffix;

            // when
            at("2026-11-08T09:00:00Z");
            var reservation = rentals.reserveForService(b22, tom);

            // then
            assertThat(reservation.status()).isEqualTo(201);
            assertThat(reservation.body().expiresAt()).isEqualTo(Instant.parse("2026-11-08T09:30:00Z"));
        }

        // @formatter:off
        @DisplayName(
            """
             E1.3 given B-17 is reserved for Tom until 09:30 and B-22 is withdrawn at Central,
             when Tom reserves B-22 at 09:05,
             then B-22 is reserved for Tom until 09:35, and Tom still holds B-17
            """
        )
        // @formatter:on
        @Test
        void e1_3_technician_holds_several_reservations() {
            // given
            at("2026-11-08T08:00:00Z");
            var b17 = withdrawnBike("B-17", OLD_TOWN);
            var b22 = withdrawnBike("B-22", CENTRAL);
            var tom = "tom-" + suffix;
            at("2026-11-08T09:00:00Z");
            rentals.reserveForService(b17, tom);

            // when
            at("2026-11-08T09:05:00Z");
            var reservation = rentals.reserveForService(b22, tom);

            // then
            assertThat(reservation.status()).isEqualTo(201);
            assertThat(reservation.body().expiresAt()).isEqualTo(Instant.parse("2026-11-08T09:35:00Z"));
            assertThat(rentals.bike(b17).body().holderId()).isEqualTo(tom);
        }

        // @formatter:off
        @DisplayName(
            """
             E2.1 given bike B-40 is available at Central and isn't withdrawn,
             when Tom reserves B-40 at 09:00,
             then the reservation is rejected and B-40 stays available to riders
            """
        )
        // @formatter:on
        @Test
        void e2_1_technician_cannot_reserve_bike_that_is_not_withdrawn() {
            // given
            at("2026-11-08T09:00:00Z");
            var b40 = rentals.bikeAt(bike("B-40"), CENTRAL);

            // when
            var reservation = rentals.reserveForService(b40, "tom-" + suffix);

            // then
            assertThat(reservation.status()).isEqualTo(409);
            assertThat(reservation.error()).isEqualTo("BikeNotWithdrawn");
            assertThat(rentals.bike(b40).body().availability()).isEqualTo("AVAILABLE");
            assertThat(rentals.bike(b40).body().hold()).isEqualTo("FREE");
        }

        // @formatter:off
        @DisplayName(
            """
             E3.1 given B-17 is reserved for Tom until 09:30,
             when technician Tara reserves B-17 at 09:10,
             then the reservation is rejected and B-17 stays reserved for Tom
            """
        )
        // @formatter:on
        @Test
        void e3_1_second_technician_cannot_reserve_reserved_bike() {
            // given
            at("2026-11-08T08:00:00Z");
            var b17 = withdrawnBike("B-17", OLD_TOWN);
            var tom = "tom-" + suffix;
            at("2026-11-08T09:00:00Z");
            rentals.reserveForService(b17, tom);

            // when
            at("2026-11-08T09:10:00Z");
            var reservation = rentals.reserveForService(b17, "tara-" + suffix);

            // then
            assertThat(reservation.status()).isEqualTo(409);
            assertThat(reservation.error()).isEqualTo("BikeAlreadyHeld");
            assertThat(rentals.bike(b17).body().holderId()).isEqualTo(tom);
        }
    }

    @Nested
    @DisplayName("Unlock Bike for service (spec technician-starts-maintenance)")
    class UnlockingForService {

        // @formatter:off
        @DisplayName(
            """
             E1.1/E2.1 given bike B-17 is withdrawn and reserved for technician Tom from 09:00 until 09:30,
             when Tom unlocks B-17 at 09:20 and the lock opens,
             then B-17 is unlocked for Tom (Bike Unlocked) and no rental session exists, so nothing is priced or charged
            """
        )
        // @formatter:on
        @Test
        void e1_1_technician_unlock_publishes_bike_unlocked_and_starts_no_session() {
            // given
            at("2026-11-08T08:00:00Z");
            var b17 = withdrawnBike("B-17", OLD_TOWN);
            var tom = "tom-" + suffix;
            at("2026-11-08T09:00:00Z");
            rentals.reserveForService(b17, tom);

            // when
            at("2026-11-08T09:20:00Z");
            var unlock = rentals.unlockForService(b17, tom);

            // then
            assertThat(unlock.status()).isEqualTo(200);
            assertThat(unlock.body()).isEqualTo(new RentalsDriver.Unlock(b17, "UNLOCKED", null));
            var bike = rentals.bike(b17).body();
            assertThat(bike.hold()).isEqualTo("UNLOCKED");
            assertThat(bike.holderType()).isEqualTo("TECHNICIAN");
            await().atMost(HAND_OFF).untilAsserted(() -> assertThat(rentals.published(BikeUnlocked.class, event -> event.bikeId().equals(b17)))
                .containsExactly(new BikeUnlocked(b17, "TECHNICIAN", tom, Instant.parse("2026-11-08T09:20:00Z"))));
            endOverdueRentalSessions.handle(new EndOverdueRentalSessions(Instant.parse("2026-11-08T23:00:00Z")));
            assertThat(rentals.published(RentalSessionEnded.class, event -> event.bikeId().equals(b17))).isEmpty();
            assertThat(rentals.bike(b17).body().hold()).isEqualTo("UNLOCKED");
        }

        // @formatter:off
        @DisplayName(
            """
             E3.1 given B-17 is reserved for Tom until 09:30,
             when technician Tara unlocks B-17 at 09:20,
             then the unlock is rejected and B-17 stays reserved for Tom
            """
        )
        // @formatter:on
        @Test
        void e3_1_other_technician_cannot_unlock() {
            // given
            at("2026-11-08T08:00:00Z");
            var b17 = withdrawnBike("B-17", OLD_TOWN);
            var tom = "tom-" + suffix;
            at("2026-11-08T09:00:00Z");
            rentals.reserveForService(b17, tom);

            // when
            at("2026-11-08T09:20:00Z");
            var unlock = rentals.unlockForService(b17, "tara-" + suffix);

            // then
            assertThat(unlock.status()).isEqualTo(409);
            assertThat(unlock.error()).isEqualTo("NotReservationHolder");
            assertThat(rentals.bike(b17).body().holderId()).isEqualTo(tom);
        }

        // @formatter:off
        @DisplayName(
            """
             E3.2 given Tom's reservation of B-17 expired at 09:30 and B-17 is still withdrawn,
             when Tom unlocks B-17 at 09:35,
             then the unlock is rejected
            """
        )
        // @formatter:on
        @Test
        void e3_2_expired_reservation_cannot_unlock() {
            // given
            at("2026-11-08T08:00:00Z");
            var b17 = withdrawnBike("B-17", OLD_TOWN);
            var tom = "tom-" + suffix;
            at("2026-11-08T09:00:00Z");
            rentals.reserveForService(b17, tom);

            // when
            at("2026-11-08T09:35:00Z");
            var unlock = rentals.unlockForService(b17, tom);

            // then
            assertThat(unlock.status()).isEqualTo(409);
            assertThat(unlock.error()).isEqualTo("NotReservationHolder");
        }

        // @formatter:off
        @DisplayName(
            """
             E4.1 given B-17 is withdrawn and reserved for Tom until 09:30,
             when Tom unlocks B-17 at 09:20 and the lock doesn't open,
             then Tom's reservation is released (Unlock Failed), B-17 is still withdrawn and any technician can reserve it again
            """
        )
        // @formatter:on
        @Test
        void e4_1_failed_unlock_releases_reservation_and_bike_stays_withdrawn() {
            // given
            at("2026-11-08T08:00:00Z");
            var b17 = withdrawnBike("B-17", OLD_TOWN);
            var tom = "tom-" + suffix;
            at("2026-11-08T09:00:00Z");
            rentals.reserveForService(b17, tom);
            locks.registerFaulty(b17);

            // when
            at("2026-11-08T09:20:00Z");
            var unlock = rentals.unlockForService(b17, tom);

            // then
            assertThat(unlock.body()).isEqualTo(new RentalsDriver.Unlock(b17, "UNLOCK_FAILED", null));
            var bike = rentals.bike(b17).body();
            assertThat(bike.availability()).isEqualTo("WITHDRAWN");
            assertThat(bike.hold()).isEqualTo("FREE");
            await().atMost(HAND_OFF).untilAsserted(() -> assertThat(rentals.published(UnlockFailed.class, event -> event.bikeId().equals(b17)))
                .containsExactly(new UnlockFailed(b17, "TECHNICIAN", tom, Instant.parse("2026-11-08T09:20:00Z"))));
            assertThat(rentals.reserveForService(b17, "tara-" + suffix).status()).isEqualTo(201);
        }
    }

    @Nested
    @DisplayName("Make Bike Available (spec technician-returns-fixed-bike-to-circulation, Rentals half of R1)")
    class MakingAvailable {

        private String unlockedForTom(String name) {
            at("2026-11-08T08:00:00Z");
            var bikeId = withdrawnBike(name, OLD_TOWN);
            at("2026-11-08T09:00:00Z");
            rentals.reserveForService(bikeId, "tom-" + suffix);
            at("2026-11-08T09:20:00Z");
            rentals.unlockForService(bikeId, "tom-" + suffix);
            return bikeId;
        }

        // @formatter:off
        @DisplayName(
            """
             E1.1 given bike B-17 is under maintenance (withdrawn, unlocked for technician Tom),
             when Fleet Maintenance makes B-17 available at station Central at 11:00,
             then B-17 is locked, free and available to riders at Central
            """
        )
        // @formatter:on
        @Test
        void e1_1_bike_made_available_at_station() {
            // given
            var b17 = unlockedForTom("B-17");

            // when
            at("2026-11-08T11:00:00Z");
            var made = rentals.makeAvailableAtStation(b17, "Central");

            // then
            assertThat(made.status()).isEqualTo(200);
            assertThat(rentals.bike(b17).body()).isEqualTo(new RentalsDriver.Bike(
                b17, "AVAILABLE", "FREE", null, null, new java.math.BigDecimal("52.23"), new java.math.BigDecimal("21.011"), "Central"));
        }

        // @formatter:off
        @DisplayName(
            """
             E1.2 given B-22 is under maintenance by Tom,
             when Fleet Maintenance makes B-22 available at position (52.2297, 21.0122) at 11:00,
             then B-22 is available to riders at that position
            """
        )
        // @formatter:on
        @Test
        void e1_2_bike_made_available_at_position() {
            // given
            var b22 = unlockedForTom("B-22");

            // when
            at("2026-11-08T11:00:00Z");
            var made = rentals.makeAvailableAt(b22, 52.2297, 21.0122);

            // then
            assertThat(made.status()).isEqualTo(200);
            var bike = rentals.bike(b22).body();
            assertThat(bike.availability()).isEqualTo("AVAILABLE");
            assertThat(bike.stationId()).isNull();
            assertThat(bike.latitude()).isEqualByComparingTo("52.2297");
        }

        @Test
        @DisplayName("a withdrawn bike reserved for a technician but not unlocked can't be made available (409); an unknown bike answers 404")
        void reserved_but_not_unlocked_bike_is_rejected() {
            // given
            at("2026-11-08T08:00:00Z");
            var b17 = withdrawnBike("B-17", OLD_TOWN);
            at("2026-11-08T09:00:00Z");
            rentals.reserveForService(b17, "tom-" + suffix);

            // when
            var made = rentals.makeAvailableAtStation(b17, "Central");

            // then
            assertThat(made.status()).isEqualTo(409);
            assertThat(made.error()).isEqualTo("BikeNotUnlockedForService");
            assertThat(rentals.makeAvailableAtStation(bike("B-unknown"), "Central").status()).isEqualTo(404);
        }

        @Test
        @DisplayName("a bike in a rider's session can't be made available (409)")
        void bike_in_ride_is_rejected() {
            // given
            at("2026-11-08T10:00:00Z");
            var anna = rider("anna");
            var b17 = bike("B-17");
            rentals.riding(b17, RIVERSIDE, anna);
            rentals.withdraw(b17);

            // when
            var made = rentals.makeAvailableAtStation(b17, "Central");

            // then
            assertThat(made.status()).isEqualTo(409);
            assertThat(rentals.activeRentalOf(anna).body().kind()).isEqualTo("SESSION");
        }
    }
}
