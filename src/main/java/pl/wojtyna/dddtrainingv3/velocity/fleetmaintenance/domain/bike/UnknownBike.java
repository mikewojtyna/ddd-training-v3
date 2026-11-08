package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike;

/// VeloCity's fleet has no bike with this id (Rentals' answer).
public class UnknownBike extends RuntimeException {

    public UnknownBike(String message) {
        super(message);
    }

    public UnknownBike(String message, Throwable cause) {
        super(message, cause);
    }
}
