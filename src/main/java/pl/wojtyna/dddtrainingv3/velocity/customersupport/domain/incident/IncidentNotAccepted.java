package pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident;

/// Only an accepted incident is escalated, and at most once (DE-97).
public class IncidentNotAccepted extends RuntimeException {

    public IncidentNotAccepted(String message) {
        super(message);
    }

    public IncidentNotAccepted(String message, Throwable cause) {
        super(message, cause);
    }
}
