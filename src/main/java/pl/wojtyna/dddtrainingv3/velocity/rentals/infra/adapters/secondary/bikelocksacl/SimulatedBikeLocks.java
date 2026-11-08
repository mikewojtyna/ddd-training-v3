package pl.wojtyna.dddtrainingv3.velocity.rentals.infra.adapters.secondary.bikelocksacl;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeId;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeLocks;

import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/// Simulated lock hardware: every lock works, except for bikes registered as faulty (every open and close fails) and
/// for outcomes a test scripted for a bike's next lock operations. Tests use their own bike ids, so scripting one bike
/// never affects another test.
@SecondaryAdapter
public class SimulatedBikeLocks implements BikeLocks {

    private final Set<String> faulty = ConcurrentHashMap.newKeySet();
    private final Map<String, Queue<LockResult>> scripted = new ConcurrentHashMap<>();

    public void registerFaulty(String bikeId) {
        faulty.add(bikeId);
    }

    public void registerRepaired(String bikeId) {
        faulty.remove(bikeId);
        scripted.remove(bikeId);
    }

    /// The bike's next lock operations (open or close, in order) answer these results; afterwards the default applies.
    public void scriptNext(String bikeId, LockResult... results) {
        var queue = scripted.computeIfAbsent(bikeId, id -> new ConcurrentLinkedQueue<>());
        for (var result : results) {
            queue.add(result);
        }
    }

    @Override
    public LockResult open(BikeId bike) {
        return outcomeFor(bike);
    }

    @Override
    public LockResult close(BikeId bike) {
        return outcomeFor(bike);
    }

    private LockResult outcomeFor(BikeId bike) {
        var queue = scripted.get(bike.value());
        var next = queue == null ? null : queue.poll();
        if (next != null) {
            return next;
        }
        return faulty.contains(bike.value()) ? LockResult.FAILED : LockResult.SUCCEEDED;
    }
}
