package pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident;

/// Only an accepted, escalated or reopened incident is resolved by its agent, and only once.
public class IncidentNotResolvable extends RuntimeException {

    public IncidentNotResolvable(String message) {
        super(message);
    }

    public IncidentNotResolvable(String message, Throwable cause) {
        super(message, cause);
    }
}
