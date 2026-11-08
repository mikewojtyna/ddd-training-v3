package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike;

/// A bike under maintenance is already being repaired: it isn't reserved or unlocked again.
public class UnderMaintenance extends RuntimeException {

    public UnderMaintenance(String message) {
        super(message);
    }

    public UnderMaintenance(String message, Throwable cause) {
        super(message, cause);
    }
}
