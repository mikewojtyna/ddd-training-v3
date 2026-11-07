package pl.wojtyna.dddtrainingv3.velocity.rentals.infra.adapters.secondary;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import pl.wojtyna.dddtrainingv3.common.domain.OptimisticLockingException;
import pl.wojtyna.dddtrainingv3.common.domain.Version;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BikeId;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BikeStation;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BikeStationId;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BikeStationRepo;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@SecondaryAdapter
public class InMemoryBikeStationRepo implements BikeStationRepo {

    private final Map<BikeStationId, BikeStation.Snapshot> stations = new ConcurrentHashMap<>();

    @Override
    public Optional<BikeStation> load(BikeStationId id) {
        return Optional.ofNullable(stations.get(id)).map(BikeStation::restore);
    }

    @Override
    public Optional<BikeStation> loadContaining(BikeId bike) {
        return stations.values().stream().map(BikeStation::restore).filter(station -> station.holds(bike)).findFirst();
    }

    @Override
    public synchronized void save(BikeStation station) {
        var stored = stations.get(station.id());
        var storedVersion = stored == null ? Version.initial() : stored.version();
        if (!storedVersion.equals(station.version())) {
            throw new OptimisticLockingException("Bike station " + station.id().value() + " was modified concurrently");
        }
        var snapshot = station.snapshot();
        stations.put(station.id(), new BikeStation.Snapshot(snapshot.id(), station.version().next(), snapshot.bikes()));
    }
}
