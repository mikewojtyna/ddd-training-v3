package pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident;

/// A reopened incident must be resolved, never rejected again (DE-54, DE-97).
public class ReopenedIncidentCannotBeRejected extends RuntimeException {

    public ReopenedIncidentCannotBeRejected(String message) {
        super(message);
    }

    public ReopenedIncidentCannotBeRejected(String message, Throwable cause) {
        super(message, cause);
    }
}
