package pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident;

/// Only a rejected incident can be reopened.
public class IncidentNotRejected extends RuntimeException {

    public IncidentNotRejected(String message) {
        super(message);
    }

    public IncidentNotRejected(String message, Throwable cause) {
        super(message, cause);
    }
}
