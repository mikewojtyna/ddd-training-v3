package pl.wojtyna.dddtrainingv3.velocity.customersupport.api.queries;

import org.jmolecules.architecture.hexagonal.SecondaryPort;

import java.util.List;
import java.util.Optional;

/// Read side of the incidents, implemented next to the incident repository.
@SecondaryPort
public interface IncidentReadModel {

    Optional<IncidentView> incident(String incidentId);

    List<OpenIncidentView> openIncidents();

    List<IncidentView> incidentsOf(String riderId);
}
