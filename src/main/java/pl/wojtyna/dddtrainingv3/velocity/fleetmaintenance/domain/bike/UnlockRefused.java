package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike;

/// Rentals refused a technician's unlock for a reason Fleet Maintenance has no word for.
public class UnlockRefused extends RuntimeException {

    public UnlockRefused(String message) {
        super(message);
    }

    public UnlockRefused(String message, Throwable cause) {
        super(message, cause);
    }
}
