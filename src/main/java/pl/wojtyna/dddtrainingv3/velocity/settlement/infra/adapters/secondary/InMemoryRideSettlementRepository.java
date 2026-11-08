package pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.secondary;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import pl.wojtyna.dddtrainingv3.common.domain.OptimisticLockingException;
import pl.wojtyna.dddtrainingv3.common.domain.Version;
import pl.wojtyna.dddtrainingv3.velocity.settlement.api.queries.RideSettlementView;
import pl.wojtyna.dddtrainingv3.velocity.settlement.api.queries.RideSettlementsReadModel;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.ride.RideSettlement;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.ride.RideSettlementRepository;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.ride.RideStatus;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.ride.SessionId;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/// Rides in memory, with optimistic locking on `Version`; a second new ride for the same session is refused. Also
/// serves the Ride Settlement query.
@SecondaryAdapter
public class InMemoryRideSettlementRepository implements RideSettlementRepository, RideSettlementsReadModel {

    private final Map<SessionId, RideSettlement.Snapshot> rides = new ConcurrentHashMap<>();

    @Override
    public Optional<RideSettlement> load(SessionId session) {
        return Optional.ofNullable(rides.get(session)).map(RideSettlement::restore);
    }

    @Override
    public List<RideSettlement> loadAwaitingRetry() {
        return rides.values().stream()
                    .filter(ride -> ride.status() == RideStatus.PENDING && ride.failedAttempts() > 0)
                    .map(RideSettlement::restore)
                    .toList();
    }

    @Override
    public void save(RideSettlement ride) {
        var snapshot = ride.snapshot();
        rides.compute(snapshot.session(), (session, stored) -> {
            var storedVersion = stored == null ? Version.initial() : stored.version();
            if (!storedVersion.equals(snapshot.version())) { // also refuses a second new ride for the same session
                throw new OptimisticLockingException("Ride " + session.value() + " was modified concurrently");
            }
            return snapshot.withVersion(snapshot.version().next());
        });
    }

    @Override
    public Optional<RideSettlementView> rideSettlementOf(String sessionId) {
        return Optional.ofNullable(rides.get(new SessionId(sessionId)))
                       .map(ride -> new RideSettlementView(ride.session().value(),
                                                           ride.rider().value(),
                                                           ride.fee().getAmount().toPlainString(),
                                                           ride.status().name(),
                                                           ride.failedAttempts(),
                                                           ride.nextAttemptDueAt(),
                                                           ride.lastFailureReason() == null ? null : ride.lastFailureReason().name()));
    }
}
