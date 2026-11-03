package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike;

import org.jmolecules.architecture.hexagonal.SecondaryPort;

/// Telling the people who care that a bike is fixed. Canvas outbound command *Notify Bike Fixed*, carried out
/// by Customer Support so it can resolve the fleet-issue incident. Fleet Maintenance is downstream: it calls
/// Customer Support's command instead of publishing an event to it (plan D9).
@SecondaryPort
public interface FixNotifications {

    void bikeFixed(BikeId bike);
}
