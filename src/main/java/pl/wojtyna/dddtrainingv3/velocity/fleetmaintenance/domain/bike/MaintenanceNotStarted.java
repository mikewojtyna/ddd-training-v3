package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike;

/// A bike returns to circulation only after a technician's unlock started its maintenance.
public class MaintenanceNotStarted extends RuntimeException {

    public MaintenanceNotStarted(String message) {
        super(message);
    }

    public MaintenanceNotStarted(String message, Throwable cause) {
        super(message, cause);
    }
}
