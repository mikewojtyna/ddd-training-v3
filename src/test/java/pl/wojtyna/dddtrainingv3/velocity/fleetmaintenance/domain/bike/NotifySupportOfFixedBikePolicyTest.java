package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Notify support of fixed bike examples")
class NotifySupportOfFixedBikePolicyTest {

    // @formatter:off
    @DisplayName(
        """
         given B-17 under maintenance,
         when a technician returns B-17 to circulation,
         then Customer Support is told B-17 is fixed
        """
    )
    // @formatter:on
    @Test
    void supportIsToldBikeIsFixed() {
        // given
        List<BikeId> notified = new ArrayList<>();
        var policy = new NotifySupportOfFixedBikePolicy(notified::add);
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
        assertThat(notified).containsExactly(new BikeId("B-17"));
    }
}
