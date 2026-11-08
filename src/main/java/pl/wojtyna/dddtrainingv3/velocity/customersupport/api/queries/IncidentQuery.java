package pl.wojtyna.dddtrainingv3.velocity.customersupport.api.queries;

/// Canvas query *Incident* (rider app, support agent). With `riderId` it is the rider's view: an incident of
/// another rider is not found. `riderId` is `null` for the support agent.
public record IncidentQuery(String incidentId, String riderId) {

    public static IncidentQuery forAgent(String incidentId) {
        return new IncidentQuery(incidentId, null);
    }
}
