package pl.wojtyna.dddtrainingv3.velocity.rentals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.velocity.common.VelocitySpringTest;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.wojtyna.dddtrainingv3.velocity.rentals.RentalsDriver.CENTRAL;
import static pl.wojtyna.dddtrainingv3.velocity.rentals.RentalsDriver.OLD_TOWN;

/// Acceptance tests of `docs/specs/fleet-manager-adds-bike-to-fleet.md`, over Rentals' REST resources.
@VelocitySpringTest
@DisplayName("Spec fleet-manager-adds-bike-to-fleet")
class FleetManagerAddsBikeToFleetSpecTest extends RentalsSpecTestBase {

    // @formatter:off
    @DisplayName(
        """
         E1.1 given bike B-40 isn't part of the fleet yet,
         when fleet manager Fiona adds B-40 to the fleet at station Central at 07:30,
         then B-40 is available at Central from 07:30
        """
    )
    // @formatter:on
    @Test
    void e1_1_new_bike_added_at_station_is_available_there() {
        // given
        at("2026-11-08T07:30:00Z");
        var b40 = bike("B-40");
        assertThat(rentals.bike(b40).status()).isEqualTo(404);

        // when
        var added = rentals.addBike(b40, "Central");

        // then
        assertThat(added.status()).isEqualTo(201);
        assertThat(rentals.bike(b40).body()).isEqualTo(new RentalsDriver.Bike(
            b40, "AVAILABLE", "FREE", null, null, new BigDecimal("52.23"), new BigDecimal("21.011"), "Central"));
    }

    // @formatter:off
    @DisplayName(
        """
         E1.2 given Fiona added B-40 to the fleet at Central at 07:30,
         when rider Anna, standing next to B-40, reserves it at 07:45,
         then B-40 is reserved for Anna
        """
    )
    // @formatter:on
    @Test
    void e1_2_added_bike_can_be_reserved_by_rider_nearby() {
        // given
        var anna = rider("anna");
        at("2026-11-08T07:30:00Z");
        var b40 = rentals.bikeAt(bike("B-40"), CENTRAL);

        // when
        at("2026-11-08T07:45:00Z");
        var reservation = rentals.reserve(b40, anna, CENTRAL.northBy(3));

        // then
        assertThat(reservation.status()).isEqualTo(201);
        assertThat(reservation.body().expiresAt()).isEqualTo(Instant.parse("2026-11-08T07:47:00Z"));
        assertThat(rentals.bike(b40).body().holderId()).isEqualTo(anna);
        assertThat(rentals.bike(b40).body().hold()).isEqualTo("RESERVED");
    }

    // @formatter:off
    @DisplayName(
        """
         E2.1 given B-40 was added to the fleet at Central on 2 November,
         when Fiona adds B-40 again at station Old Town on 8 November,
         then it is rejected and B-40 stays where it is, in the state it is in
        """
    )
    // @formatter:on
    @Test
    void e2_1_adding_same_bike_again_is_rejected() {
        // given
        at("2026-11-02T09:00:00Z");
        var b40 = rentals.bikeAt(bike("B-40"), CENTRAL);

        // when
        at("2026-11-08T09:00:00Z");
        var again = rentals.addBike(b40, OLD_TOWN.stationId());

        // then
        assertThat(again.status()).isEqualTo(409);
        assertThat(again.error()).isEqualTo("BikeAlreadyInFleet");
        assertThat(rentals.bike(b40).body().stationId()).isEqualTo("Central");
        assertThat(rentals.bike(b40).body().availability()).isEqualTo("AVAILABLE");
    }

    // @formatter:off
    @DisplayName(
        """
         E3.1 given B-41 isn't part of the fleet yet and "Harbour Café" is not a station,
         when Fiona adds B-41 at "Harbour Café",
         then it is rejected and B-41 isn't part of the fleet
        """
    )
    // @formatter:on
    @Test
    void e3_1_adding_bike_outside_a_station_is_rejected() {
        // given
        at("2026-11-08T09:00:00Z");
        var b41 = bike("B-41");

        // when
        var added = rentals.addBike(b41, "Harbour Café");

        // then
        assertThat(added.status()).isEqualTo(409);
        assertThat(added.error()).isEqualTo("UnknownStation");
        assertThat(rentals.bike(b41).status()).isEqualTo(404);
    }
}
