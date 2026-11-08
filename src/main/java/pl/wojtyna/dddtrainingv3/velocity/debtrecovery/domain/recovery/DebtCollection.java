package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery;

import org.jmolecules.architecture.hexagonal.SecondaryPort;

/// Asks Settlement, the only context that moves money, to collect the rider's whole outstanding debt at once.
@SecondaryPort
public interface DebtCollection {

    CollectionOutcome collect(RiderId rider);
}
