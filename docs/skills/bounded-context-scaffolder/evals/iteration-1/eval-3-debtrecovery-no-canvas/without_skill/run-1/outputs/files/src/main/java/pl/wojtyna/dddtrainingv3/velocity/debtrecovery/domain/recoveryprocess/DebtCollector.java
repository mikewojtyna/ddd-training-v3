package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recoveryprocess;

import org.jmolecules.architecture.hexagonal.SecondaryPort;

/**
 * Settlement is the only context that moves money, so Debt Recovery asks it to collect. The outcome comes back
 * asynchronously as Settlement's "Outstanding Debt Cleared".
 */
@SecondaryPort
public interface DebtCollector {

    void requestCollection(RiderId rider);
}
