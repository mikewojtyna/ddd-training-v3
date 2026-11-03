package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.infra.adapters.secondary.customersupportctxacl;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.BikeId;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.FixNotifications;

/// Stub: Customer Support isn't in code yet, so the name and shape of its command are unknown.
@SecondaryAdapter
public class CustomerSupportFixNotificationsAdapter implements FixNotifications {

    @Override
    public void bikeFixed(BikeId bike) {
        // Canvas outbound command: "Notify Bike Fixed" -> Customer Support (so it can resolve the fleet-issue incident)
        throw new UnsupportedOperationException("implement this");
    }
}
