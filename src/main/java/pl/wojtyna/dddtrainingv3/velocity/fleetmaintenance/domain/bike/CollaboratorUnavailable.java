package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike;

/// Another context didn't answer.
public class CollaboratorUnavailable extends RuntimeException {

    public CollaboratorUnavailable(String message) {
        super(message);
    }

    public CollaboratorUnavailable(String message, Throwable cause) {
        super(message, cause);
    }
}
