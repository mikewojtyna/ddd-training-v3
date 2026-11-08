package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike;

/// The technician named a station VeloCity doesn't have.
public class UnknownStation extends RuntimeException {

    public UnknownStation(String message) {
        super(message);
    }

    public UnknownStation(String message, Throwable cause) {
        super(message, cause);
    }
}
