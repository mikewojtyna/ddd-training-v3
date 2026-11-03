package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike;

/// Thrown when a technician tries to return a bike to circulation that isn't under maintenance (rule R1).
public class MaintenanceNotStarted extends RuntimeException {

    public MaintenanceNotStarted(BikeId id) {
        super("Bike " + id.value() + " can't return to circulation: maintenance on it has not started");
    }
}
