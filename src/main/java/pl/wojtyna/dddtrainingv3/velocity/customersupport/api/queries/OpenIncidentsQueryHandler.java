package pl.wojtyna.dddtrainingv3.velocity.customersupport.api.queries;

import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.util.List;

/// Answers the *Open Incidents* query.
@PrimaryPort
public class OpenIncidentsQueryHandler {

    private final IncidentReadModel readModel;

    public OpenIncidentsQueryHandler(IncidentReadModel readModel) {
        this.readModel = readModel;
    }

    public List<OpenIncidentView> handle(OpenIncidentsQuery query) {
        return readModel.openIncidents();
    }
}
