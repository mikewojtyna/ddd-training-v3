package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike;

import pl.wojtyna.dddtrainingv3.common.domain.DomainPolicy;

import static pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.BikeMaintenanceEvent.BikeFixed;

/// R6: whenever a bike is fixed, Customer Support is told.
public class NotifySupportOfFixedBikePolicy implements DomainPolicy {

    private final FixNotifications fixNotifications;

    public NotifySupportOfFixedBikePolicy(FixNotifications fixNotifications) {
        this.fixNotifications = fixNotifications;
    }

    public void handle(BikeFixed event) {
        fixNotifications.bikeFixed(event.bikeId());
    }
}
