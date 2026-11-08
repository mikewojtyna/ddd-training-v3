package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike;

/// Only a broken bike can be reserved, unlocked or returned to circulation by a technician (DE-16).
public class NotBroken extends RuntimeException {

    public NotBroken(String message) {
        super(message);
    }

    public NotBroken(String message, Throwable cause) {
        super(message, cause);
    }
}
