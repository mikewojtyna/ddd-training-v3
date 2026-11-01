package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement;

import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.RideId;
import org.jmolecules.architecture.hexagonal.SecondaryPort;
import org.jmolecules.ddd.annotation.Repository;

import java.util.Optional;

@Repository
@SecondaryPort
public interface SettlementRepository {

    Optional<Settlement> load(RideId rideId);

    void save(Settlement settlement);
}
