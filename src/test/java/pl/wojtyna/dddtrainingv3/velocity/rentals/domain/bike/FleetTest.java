package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeEvent.BikeAddedToFleet;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeEvent.BikeReserved;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.Station.StationId;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeFixture.*;

class FleetTest {

    // @formatter:off
    @DisplayName(
        """
         given bike B-40 isn't part of the fleet yet,
         when fleet manager Fiona adds B-40 to the fleet at station Central at 07:30,
         then B-40 is available at Central from 07:30 (Bike Added to Fleet)
        """
    )
    // @formatter:on
    @Test
    void newBikeAddedAtStationIsAvailableThere() {
        // given
        var fleet = new Fleet(bike -> false, STATIONS);

        // when
        var added = fleet.add(new BikeId("B-40"), CENTRAL.id(), at("07:30"));

        // then
        assertThat(all(added.events())).containsExactly(
            new BikeAddedToFleet(new BikeId("B-40"), CENTRAL.id(), CENTRAL.position(), at("07:30")));
        var reservation = added.bike().reserveForRide(ANNA).standingAt(near(CENTRAL, 5)).given(ELIGIBLE).at(at("07:45"));
        assertThat(eventsOf(reservation, BikeReserved.class)).hasSize(1);
    }

    // @formatter:off
    @DisplayName(
        """
         given B-40 was added to the fleet at Central on 2 November,
         when Fiona adds B-40 again at station Old Town on 8 November,
         then it is rejected
        """
    )
    // @formatter:on
    @Test
    void addingSameBikeAgainIsRejected() {
        // given
        var fleet = new Fleet(Set.of(new BikeId("B-40"))::contains, STATIONS);

        // when / then
        assertThatThrownBy(() -> fleet.add(new BikeId("B-40"), OLD_TOWN.id(), at("08:00")))
            .isInstanceOf(BikeAlreadyInFleet.class);
    }

    // @formatter:off
    @DisplayName(
        """
         given B-41 isn't part of the fleet yet and "Harbour Café" is not a station,
         when Fiona adds B-41 at "Harbour Café",
         then it is rejected
        """
    )
    // @formatter:on
    @Test
    void addingBikeOutsideAStationIsRejected() {
        // given
        var fleet = new Fleet(bike -> false, STATIONS);

        // when / then
        assertThatThrownBy(() -> fleet.add(new BikeId("B-41"), new StationId("Harbour Café"), at("08:00")))
            .isInstanceOf(UnknownStation.class);
    }
}
