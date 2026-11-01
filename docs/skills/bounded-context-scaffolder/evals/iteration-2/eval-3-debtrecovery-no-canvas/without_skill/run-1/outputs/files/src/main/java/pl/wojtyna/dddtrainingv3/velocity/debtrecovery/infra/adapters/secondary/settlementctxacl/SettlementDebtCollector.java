package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.infra.adapters.secondary.settlementctxacl;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recoveryprocess.DebtCollector;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recoveryprocess.RiderId;

/**
 * Calls Settlement's "attempt collection" open host service.
 */
@SecondaryAdapter
public class SettlementDebtCollector implements DebtCollector {

    @Override
    public void requestCollection(RiderId rider) {
        throw new UnsupportedOperationException("Implement once Settlement exposes its collection API");
    }
}
