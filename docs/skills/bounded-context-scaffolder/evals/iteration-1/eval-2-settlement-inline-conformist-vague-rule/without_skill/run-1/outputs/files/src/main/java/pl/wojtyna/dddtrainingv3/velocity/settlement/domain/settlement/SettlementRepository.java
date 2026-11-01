package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement;

import org.jmolecules.architecture.hexagonal.SecondaryPort;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@SecondaryPort
public interface SettlementRepository {

    Optional<Settlement> load(SettlementId id);

    void save(Settlement settlement);

    /**
     * Open settlements whose next charge attempt is due at {@code now} or earlier.
     */
    List<SettlementId> findDueForChargeAttempt(Instant now);
}
