package pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident;

/// No incident with this id exists.
public class IncidentNotFound extends RuntimeException {

    public IncidentNotFound(IncidentId id) {
        super("Incident " + id.value() + " not found");
    }
}
