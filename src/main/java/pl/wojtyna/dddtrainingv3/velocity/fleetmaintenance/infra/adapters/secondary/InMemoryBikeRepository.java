package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.infra.adapters.secondary;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.Bike;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.BikeId;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.BikeRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@SecondaryAdapter
public class InMemoryBikeRepository implements BikeRepository {

    private final Map<BikeId, Bike> bikes = new ConcurrentHashMap<>();

    @Override
    public Optional<Bike> load(BikeId id) {
        return Optional.ofNullable(bikes.get(id));
    }

    @Override
    public void save(Bike bike) {
        bikes.put(bike.id(), bike);
    }
}
