package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
}
