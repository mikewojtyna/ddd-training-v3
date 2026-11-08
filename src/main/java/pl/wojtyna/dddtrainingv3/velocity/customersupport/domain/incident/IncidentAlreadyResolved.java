package pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident;

/// A resolved incident is closed: nobody picks it up any more.
public class IncidentAlreadyResolved extends RuntimeException {

    public IncidentAlreadyResolved(String message) {
        super(message);
    }

    public IncidentAlreadyResolved(String message, Throwable cause) {
        super(message, cause);
    }
}
