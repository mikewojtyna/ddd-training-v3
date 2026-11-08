package pl.wojtyna.dddtrainingv3.velocity.rentals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.velocity.common.VelocitySpringTest;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.wojtyna.dddtrainingv3.velocity.rentals.RentalsDriver.CENTRAL;

/// Acceptance tests of `docs/specs/rider-reserves-bike.md`. Riders are onboarded over Accounts' REST API; Rentals asks
/// Accounts for their eligibility over REST (accountsctxacl).
@VelocitySpringTest
@DisplayName("Spec rider-reserves-bike")
class RiderReservesBikeSpecTest extends RentalsSpecTestBase {

    // @formatter:off
    @DisplayName(
        """
         E1.1 given bike B-17 is available at station Central and rider Anna is eligible to rent,
         when Anna, standing 20 m from B-17, reserves it at 10:00,
         then B-17 is reserved for Anna until 10:02
        """
    )
    // @formatter:on
    @Test
    void e1_1_eligible_rider_nearby_reserves_available_bike_for_two_minutes() {
        // given
        at("2026-11-08T09:00:00Z");
        var anna = rider("anna");
        var b17 = rentals.bikeAt(bike("B-17"), CENTRAL);

        // when
        at("2026-11-08T10:00:00Z");
        var reservation = rentals.reserve(b17, anna, CENTRAL.northBy(20));

        // then
        assertThat(reservation.status()).isEqualTo(201);
        assertThat(reservation.body()).isEqualTo(new RentalsDriver.Reservation(
            b17, "RIDER", anna, Instant.parse("2026-11-08T10:00:00Z"), Instant.parse("2026-11-08T10:02:00Z")));
        var active = rentals.activeRentalOf(anna).body();
        assertThat(active.kind()).isEqualTo("RESERVATION");
        assertThat(active.bikeId()).isEqualTo(b17);
        assertThat(active.expiresAt()).isEqualTo(Instant.parse("2026-11-08T10:02:00Z"));
    }

    // @formatter:off
    @DisplayName(
        """
         E2.1 given B-17 is available at Central and Anna is eligible to rent,
         when Anna, standing 400 m from B-17, reserves it,
         then the reservation is rejected and B-17 stays available
        """
    )
    // @formatter:on
    @Test
    void e2_1_rider_400_m_away_is_rejected() {
        // given
        at("2026-11-08T09:00:00Z");
        var anna = rider("anna");
        var b17 = rentals.bikeAt(bike("B-17"), CENTRAL);

        // when
        var reservation = rentals.reserve(b17, anna, CENTRAL.northBy(400));

        // then
        assertThat(reservation.status()).isEqualTo(409);
        assertThat(reservation.error()).isEqualTo("TooFarFromBike");
        assertThat(rentals.bike(b17).body().hold()).isEqualTo("FREE");
    }

    // @formatter:off
    @DisplayName(
        """
         E2.2 given B-17 is available at Central and Anna is eligible to rent,
         when Anna, standing exactly 50 m from B-17, reserves it at 10:00,
         then B-17 is reserved for Anna until 10:02
        """
    )
    // @formatter:on
    @Test
    void e2_2_rider_exactly_50_m_away_reserves() {
        // given
        at("2026-11-08T09:00:00Z");
        var anna = rider("anna");
        var b17 = rentals.bikeAt(bike("B-17"), CENTRAL);

        // when
        at("2026-11-08T10:00:00Z");
        var reservation = rentals.reserve(b17, anna, CENTRAL.northBy(50));

        // then
        assertThat(reservation.status()).isEqualTo(201);
        assertThat(reservation.body().expiresAt()).isEqualTo(Instant.parse("2026-11-08T10:02:00Z"));
    }

    // @formatter:off
    @DisplayName(
        """
         E3.1 given rider Ben has confirmed his email and phone and given his personal data but hasn't added a payment method yet,
         when Ben, standing 10 m from available B-17, reserves it,
         then the reservation is rejected
        """
    )
    // @formatter:on
    @Test
    void e3_1_rider_without_payment_method_is_rejected() {
        // given
        at("2026-11-08T09:00:00Z");
        var ben = rentals.riderWithoutPaymentMethod("ben-" + suffix);
        var b17 = rentals.bikeAt(bike("B-17"), CENTRAL);

        // when
        var reservation = rentals.reserve(b17, ben, CENTRAL.northBy(10));

        // then
        assertThat(reservation.status()).isEqualTo(409);
        assertThat(reservation.error()).isEqualTo("RiderNotEligible");
        assertThat(rentals.bike(b17).body().hold()).isEqualTo("FREE");
    }

    // @formatter:off
    @DisplayName(
        """
         E3.2 given rider Carla's account is activated but blocked because of outstanding debt,
         when Carla, standing 10 m from available B-17, reserves it,
         then the reservation is rejected
        """
    )
    // @formatter:on
    @Test
    void e3_2_blocked_rider_is_rejected() {
        // given
        at("2026-11-08T09:00:00Z");
        var carla = rentals.riderBlockedForDebt("carla-" + suffix);
        var b17 = rentals.bikeAt(bike("B-17"), CENTRAL);

        // when
        var reservation = rentals.reserve(b17, carla, CENTRAL.northBy(10));

        // then
        assertThat(reservation.status()).isEqualTo(409);
        assertThat(reservation.error()).isEqualTo("RiderNotEligible");
    }

    // @formatter:off
    @DisplayName(
        """
         E4.1 given Anna holds a reservation for B-17 until 10:02,
         when Anna, standing 15 m from available B-22, reserves B-22 at 10:01,
         then the reservation is rejected and B-22 stays available
        """
    )
    // @formatter:on
    @Test
    void e4_1_rider_holding_a_reservation_cannot_reserve_another_bike() {
        // given
        at("2026-11-08T10:00:00Z");
        var anna = rider("anna");
        rentals.reserved(bike("B-17"), CENTRAL, anna);
        var b22 = rentals.bikeAt(bike("B-22"), CENTRAL);

        // when
        at("2026-11-08T10:01:00Z");
        var reservation = rentals.reserve(b22, anna, CENTRAL.northBy(15));

        // then
        assertThat(reservation.status()).isEqualTo(409);
        assertThat(reservation.error()).isEqualTo("OneRiderOneBike");
        assertThat(rentals.bike(b22).body().hold()).isEqualTo("FREE");
    }

    // @formatter:off
    @DisplayName(
        """
         E4.2 given Anna's ride on B-17 started at 09:30 and is still running,
         when Anna, standing 15 m from available B-22, reserves B-22 at 10:00,
         then the reservation is rejected
        """
    )
    // @formatter:on
    @Test
    void e4_2_rider_in_running_ride_cannot_reserve() {
        // given
        at("2026-11-08T09:30:00Z");
        var anna = rider("anna");
        rentals.riding(bike("B-17"), CENTRAL, anna);
        var b22 = rentals.bikeAt(bike("B-22"), CENTRAL);

        // when
        at("2026-11-08T10:00:00Z");
        var reservation = rentals.reserve(b22, anna, CENTRAL.northBy(15));

        // then
        assertThat(reservation.status()).isEqualTo(409);
        assertThat(reservation.error()).isEqualTo("OneRiderOneBike");
    }

    // @formatter:off
    @DisplayName(
        """
         E5.1 given B-17 is reserved for Anna until 10:02,
         when eligible rider Ben, standing 5 m from B-17, reserves it at 10:01,
         then the reservation is rejected and B-17 stays reserved for Anna
        """
    )
    // @formatter:on
    @Test
    void e5_1_bike_reserved_for_someone_else_is_rejected() {
        // given
        at("2026-11-08T10:00:00Z");
        var anna = rider("anna");
        var ben = rider("ben");
        var b17 = bike("B-17");
        rentals.reserved(b17, CENTRAL, anna);

        // when
        at("2026-11-08T10:01:00Z");
        var reservation = rentals.reserve(b17, ben, CENTRAL.northBy(5));

        // then
        assertThat(reservation.status()).isEqualTo(409);
        assertThat(reservation.error()).isEqualTo("BikeNotAvailable");
        assertThat(rentals.bike(b17).body().holderId()).isEqualTo(anna);
    }

    // @formatter:off
    @DisplayName(
        """
         E5.2 given B-17 is broken and withdrawn from riders at Central,
         when eligible Anna, standing 20 m from B-17, reserves it,
         then the reservation is rejected
        """
    )
    // @formatter:on
    @Test
    void e5_2_withdrawn_bike_is_rejected() {
        // given
        at("2026-11-08T09:00:00Z");
        var anna = rider("anna");
        var b17 = rentals.bikeAt(bike("B-17"), CENTRAL);
        rentals.signalFault(b17, "BRAKE_SENSOR");
        assertThat(rentals.bike(b17).body().availability()).isEqualTo("WITHDRAWN");

        // when
        var reservation = rentals.reserve(b17, anna, CENTRAL.northBy(20));

        // then
        assertThat(reservation.status()).isEqualTo(409);
        assertThat(reservation.error()).isEqualTo("BikeNotAvailable");
    }
}
