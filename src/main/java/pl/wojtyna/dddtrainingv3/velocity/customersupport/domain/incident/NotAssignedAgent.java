package pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident;

/// Only the assigned agent accepts, rejects, escalates or resolves an incident (DE-51).
public class NotAssignedAgent extends RuntimeException {

    public NotAssignedAgent(String message) {
        super(message);
    }

    public NotAssignedAgent(String message, Throwable cause) {
        super(message, cause);
    }
}
