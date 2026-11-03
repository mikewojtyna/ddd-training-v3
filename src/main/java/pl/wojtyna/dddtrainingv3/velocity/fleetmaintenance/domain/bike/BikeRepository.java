package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike;

import org.jmolecules.architecture.hexagonal.SecondaryPort;
import org.jmolecules.ddd.annotation.Repository;

import java.util.Optional;

@Repository
@SecondaryPort
public interface BikeRepository {

    Optional<Bike> load(BikeId id);

    void save(Bike bike);
}
