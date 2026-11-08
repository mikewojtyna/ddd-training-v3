package pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident;

/// An incident is accepted or rejected once; an accepted incident is not decided again.
public class IncidentAlreadyDecided extends RuntimeException {

    public IncidentAlreadyDecided(String message) {
        super(message);
    }

    public IncidentAlreadyDecided(String message, Throwable cause) {
        super(message, cause);
    }
}
