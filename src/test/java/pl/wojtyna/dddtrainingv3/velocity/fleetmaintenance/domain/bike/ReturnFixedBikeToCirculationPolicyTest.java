package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Return fixed bike to circulation examples")
class ReturnFixedBikeToCirculationPolicyTest {

    // @formatter:off
    @DisplayName(
        """
         given bike B-17 is broken and maintenance on it has started,
         when a technician returns B-17 to circulation,
         then Rentals is asked to make B-17 available
        """
    )
    // @formatter:on
    @Test
    void fixedBikeIsPutBackIntoCirculation() {
        // given
        var circulation = new RecordingCirculation();
        var policy = new ReturnFixedBikeToCirculationPolicy(circulation);
        var bike = Bike.inCirculation(new BikeId("B-17"));
        bike.reportFault();
        bike.startMaintenance(new TechnicianId("T-1"));

        // when
        bike.returnToCirculation(new TechnicianId("T-1"))
            .stream()
            .filter(BikeMaintenanceEvent.BikeFixed.class::isInstance)
            .map(BikeMaintenanceEvent.BikeFixed.class::cast)
            .forEach(policy::handle);

        // then
        assertThat(circulation.putBack).containsExactly(new BikeId("B-17"));
        assertThat(circulation.takenOut).isEmpty();
    }

    private static class RecordingCirculation implements Circulation {

        private final List<BikeId> takenOut = new ArrayList<>();
        private final List<BikeId> putBack = new ArrayList<>();

        @Override
        public void takeOut(BikeId bike) {
            takenOut.add(bike);
        }

        @Override
        public void putBack(BikeId bike) {
            putBack.add(bike);
        }
    }
}
