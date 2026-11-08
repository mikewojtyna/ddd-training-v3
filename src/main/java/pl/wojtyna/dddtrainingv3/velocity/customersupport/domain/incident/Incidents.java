package pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident;

import org.jmolecules.architecture.hexagonal.SecondaryPort;
import org.jmolecules.ddd.annotation.Repository;
import pl.wojtyna.dddtrainingv3.common.domain.OptimisticLockingException;

import java.util.List;
import java.util.Optional;

/// All incidents Customer Support works.
@Repository
@SecondaryPort
public interface Incidents {

    Optional<Incident> load(IncidentId id);

    /// Stores the incident.
    ///
    /// @throws OptimisticLockingException if the stored version differs from {@code incident.version()}
    void save(Incident incident);

    /// Rejected incidents, the candidates for resolving uncontested rejections (DE-87).
    List<Incident> rejected();
}
