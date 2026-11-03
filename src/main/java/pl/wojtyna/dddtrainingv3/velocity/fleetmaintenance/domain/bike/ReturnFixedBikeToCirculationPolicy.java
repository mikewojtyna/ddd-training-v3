package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike;

import pl.wojtyna.dddtrainingv3.common.domain.DomainPolicy;

import static pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.BikeMaintenanceEvent.BikeFixed;

/// R2: whenever a bike is fixed, Rentals is asked to make it available again.
public class ReturnFixedBikeToCirculationPolicy implements DomainPolicy {

    private final Circulation circulation;

    public ReturnFixedBikeToCirculationPolicy(Circulation circulation) {
        this.circulation = circulation;
    }

    public void handle(BikeFixed event) {
        circulation.putBack(event.bikeId());
    }
}
