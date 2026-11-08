package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike;

import org.jmolecules.architecture.hexagonal.SecondaryPort;
import org.jmolecules.ddd.annotation.Repository;
import pl.wojtyna.dddtrainingv3.common.domain.OptimisticLockingException;

import java.util.Optional;

/// Every bike Fleet Maintenance has heard of (from its first fault report on).
@Repository
@SecondaryPort
public interface Bikes {

    Optional<Bike> load(BikeId id);

    /// The bike as Fleet Maintenance knows it: a bike it never heard of isn't broken (`Bike.unknown`).
    default Bike loadOrUnknown(BikeId id) {
        return load(id).orElseGet(() -> Bike.unknown(id));
    }

    /// @throws OptimisticLockingException if the stored version differs from `bike.version()`
    void save(Bike bike);
}
