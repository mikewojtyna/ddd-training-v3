package pl.wojtyna.dddtrainingv3.velocity.customersupport.infra.adapters.secondary.persistence;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import pl.wojtyna.dddtrainingv3.common.domain.OptimisticLockingException;
import pl.wojtyna.dddtrainingv3.common.domain.Version;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.Incident;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.IncidentId;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.IncidentStatus;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.Incidents;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/// Incidents in memory, with optimistic locking on the incident's `Version`.
@SecondaryAdapter
public class InMemoryIncidents implements Incidents {

    private final Map<IncidentId, Incident.Snapshot> incidents = new ConcurrentHashMap<>();

    @Override
    public Optional<Incident> load(IncidentId id) {
        return Optional.ofNullable(incidents.get(id)).map(Incident::restore);
    }

    @Override
    public void save(Incident incident) {
        incidents.compute(incident.id(), (id, stored) -> {
            var storedVersion = stored == null ? Version.initial() : stored.version();
            if (!storedVersion.equals(incident.version())) {
                throw new OptimisticLockingException("Incident " + id.value() + " was modified concurrently");
            }
            return incident.snapshot().withVersion(incident.version().next());
        });
    }

    @Override
    public List<Incident> rejected() {
        return incidents.values().stream()
                        .filter(snapshot -> snapshot.status() == IncidentStatus.REJECTED)
                        .map(Incident::restore)
                        .toList();
    }
}
