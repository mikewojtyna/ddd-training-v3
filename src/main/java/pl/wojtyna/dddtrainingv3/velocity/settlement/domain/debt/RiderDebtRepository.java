package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.debt;

import org.jmolecules.architecture.hexagonal.SecondaryPort;
import org.jmolecules.ddd.annotation.Repository;
import pl.wojtyna.dddtrainingv3.common.domain.OptimisticLockingException;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.RiderId;

import java.util.Optional;

/// Riders' outstanding debts. `save` accepts a debt only if the stored version still equals `debt.version()`.
@Repository
@SecondaryPort
public interface RiderDebtRepository {

    Optional<RiderDebt> load(RiderId rider);

    /// @throws OptimisticLockingException if the stored version differs from `debt.version()`
    void save(RiderDebt debt);
}
