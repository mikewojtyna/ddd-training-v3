package pl.wojtyna.dddtrainingv3.velocity.customersupport.api.queries;

import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.util.List;

/// Answers the *Rider's incidents* query.
@PrimaryPort
public class RiderIncidentsQueryHandler {

    private final IncidentReadModel readModel;

    public RiderIncidentsQueryHandler(IncidentReadModel readModel) {
        this.readModel = readModel;
    }

    public List<IncidentView> handle(RiderIncidentsQuery query) {
        return readModel.incidentsOf(query.riderId());
    }
}
