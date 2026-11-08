package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.ride;

import org.jmolecules.architecture.hexagonal.SecondaryPort;
import org.jmolecules.ddd.annotation.Repository;
import pl.wojtyna.dddtrainingv3.common.domain.OptimisticLockingException;

import java.util.List;
import java.util.Optional;

/// Rides being settled. `save` accepts a ride only if the stored version still equals `ride.version()` (a new ride:
/// only if no ride with its session exists, which is what keeps a ride from being recorded twice, DE-95).
@Repository
@SecondaryPort
public interface RideSettlementRepository {

    Optional<RideSettlement> load(SessionId session);

    /// Pending rides with at least one failed attempt; whether a retry is due now is the ride's decision.
    List<RideSettlement> loadAwaitingRetry();

    /// @throws OptimisticLockingException if the stored version differs from `ride.version()`
    void save(RideSettlement ride);
}
