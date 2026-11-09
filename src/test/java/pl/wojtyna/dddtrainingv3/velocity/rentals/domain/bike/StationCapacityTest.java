package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.Station.StationId;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeFixture.CENTRAL;

/// Exercise 11: a station docks at most a fixed number of bikes.
class StationCapacityTest {

    // @formatter:off
    @DisplayName(
        """
         given Central docks 19 bikes,
         when a bike is returned at Central,
         then it is admitted
        """
    )
    // @formatter:on
    @Test
    void stationWithRoomAdmitsReturnedBike() {
        assertThatCode(() -> CENTRAL.admitReturn(19)).doesNotThrowAnyException();
    }

    // @formatter:off
    @DisplayName(
        """
         given Central docks 20 bikes,
         when a bike is returned at Central,
         then it is turned away: the station is full
        """
    )
    // @formatter:on
    @Test
    void fullStationTurnsReturnedBikeAway() {
        assertThatThrownBy(() -> CENTRAL.admitReturn(20)).isInstanceOf(StationFull.class);
    }

    // @formatter:off
    @DisplayName(
        """
         given the small station Park Gate has room for 10 bikes and docks 10,
         when a bike is returned at Park Gate,
         then it is turned away: the station is full
        """
    )
    // @formatter:on
    @Test
    void smallStationIsFullAtItsOwnCapacity() {
        var parkGate = new Station(new StationId("Park Gate"), new Position(52.2350, 21.0200), 10);

        assertThatThrownBy(() -> parkGate.admitReturn(10)).isInstanceOf(StationFull.class);
    }
}
