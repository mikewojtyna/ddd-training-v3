package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.infra.event;

import org.springframework.context.event.EventListener;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.BikeMaintenanceEvent.BikeFixed;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.NotifySupportOfFixedBikePolicy;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.ReturnFixedBikeToCirculationPolicy;

/// Routes Fleet Maintenance's own domain events to its policies.
public class FleetMaintenanceLocalEventsHandler {

    private final ReturnFixedBikeToCirculationPolicy returnFixedBikeToCirculation;
    private final NotifySupportOfFixedBikePolicy notifySupportOfFixedBike;

    public FleetMaintenanceLocalEventsHandler(ReturnFixedBikeToCirculationPolicy returnFixedBikeToCirculation,
                                              NotifySupportOfFixedBikePolicy notifySupportOfFixedBike) {
        this.returnFixedBikeToCirculation = returnFixedBikeToCirculation;
        this.notifySupportOfFixedBike = notifySupportOfFixedBike;
    }

    @EventListener
    public void returnToCirculation(BikeFixed bikeFixed) {
        returnFixedBikeToCirculation.handle(bikeFixed);
    }

    @EventListener
    public void notifySupport(BikeFixed bikeFixed) {
        notifySupportOfFixedBike.handle(bikeFixed);
    }
}
