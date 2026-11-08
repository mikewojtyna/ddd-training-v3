package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.infra.adapters.secondary;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import pl.wojtyna.dddtrainingv3.common.domain.OptimisticLockingException;
import pl.wojtyna.dddtrainingv3.common.domain.Version;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.queries.BrokenBikeView;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.queries.BrokenBikesReadModel;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.Bike;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.BikeCondition;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.BikeId;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.Bikes;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.FaultDescription;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.IncidentId;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.Maintenance;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.TechnicianReservation;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/// Fleet Maintenance's bikes in memory, with optimistic locking on the bike's `Version`; also their read side.
@SecondaryAdapter
public class InMemoryBikes implements Bikes, BrokenBikesReadModel {

    private final Map<BikeId, Bike.Snapshot> bikes = new ConcurrentHashMap<>();

    @Override
    public Optional<Bike> load(BikeId id) {
        return Optional.ofNullable(bikes.get(id)).map(Bike::restore);
    }

    @Override
    public void save(Bike bike) {
        bikes.compute(bike.id(), (id, stored) -> {
            var storedVersion = stored == null ? Version.initial() : stored.version();
            if (!storedVersion.equals(bike.version())) {
                throw new OptimisticLockingException("Bike " + id.value() + " was modified concurrently");
            }
            return bike.snapshot().withVersion(bike.version().next());
        });
    }

    @Override
    public Optional<BrokenBikeView> bike(String bikeId) {
        return Optional.ofNullable(bikes.get(new BikeId(bikeId))).map(InMemoryBikes::view);
    }

    @Override
    public List<BrokenBikeView> brokenBikes() {
        return bikes.values().stream()
                    .filter(snapshot -> snapshot.condition() != BikeCondition.IN_CIRCULATION)
                    .sorted(Comparator.comparing(snapshot -> snapshot.reportedAt().orElse(Instant.MIN)))
                    .map(InMemoryBikes::view)
                    .toList();
    }

    private static BrokenBikeView view(Bike.Snapshot bike) {
        var technician = bike.maintenance().map(Maintenance::technician)
                             .or(() -> bike.reservation().map(TechnicianReservation::technician));
        return new BrokenBikeView(bike.id().value(), bike.condition().name(),
                                  bike.faultSource().map(Enum::name).orElse(null),
                                  bike.reportedAt().orElse(null),
                                  bike.faultDescription().map(FaultDescription::value).orElse(null),
                                  technician.map(id -> id.value()).orElse(null),
                                  bike.reservation().map(TechnicianReservation::until).orElse(null),
                                  bike.incidents().stream().map(IncidentId::value).toList(),
                                  null, null, null);
    }
}
