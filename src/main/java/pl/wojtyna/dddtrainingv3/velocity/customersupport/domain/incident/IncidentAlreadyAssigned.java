package pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident;

/// An incident is picked up once (R1 of support-agent-decides-on-incident, DE-51).
public class IncidentAlreadyAssigned extends RuntimeException {

    public IncidentAlreadyAssigned(String message) {
        super(message);
    }

    public IncidentAlreadyAssigned(String message, Throwable cause) {
        super(message, cause);
    }
}
