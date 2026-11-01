package pl.wojtyna.dddtrainingv3.velocity.rentals.infra.adapters.secondary.repository;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BikeStation;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BikeStationId;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BikeStationRepo;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@SecondaryAdapter
public class InMemoryBikeStationRepo implements BikeStationRepo {

    private final Map<BikeStationId, BikeStation> stations = new ConcurrentHashMap<>();

    @Override
    public Optional<BikeStation> load(BikeStationId id) {
        return Optional.ofNullable(stations.get(id));
    }

    @Override
    public void save(BikeStation bikeStation) {
        stations.put(bikeStation.id(), bikeStation);
    }
}
