package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
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
}
