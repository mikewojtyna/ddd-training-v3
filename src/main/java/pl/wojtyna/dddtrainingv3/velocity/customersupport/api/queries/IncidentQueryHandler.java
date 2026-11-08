package pl.wojtyna.dddtrainingv3.velocity.customersupport.api.queries;

import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.util.Optional;

/// Answers the *Incident* query; the rider's view hides other riders' incidents.
@PrimaryPort
public class IncidentQueryHandler {

    private final IncidentReadModel readModel;

    public IncidentQueryHandler(IncidentReadModel readModel) {
        this.readModel = readModel;
    }

    public Optional<IncidentView> handle(IncidentQuery query) {
        return readModel.incident(query.incidentId())
                        .filter(view -> query.riderId() == null || query.riderId().equals(view.riderId()));
    }
}
