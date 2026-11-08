package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.ride;

import org.jmolecules.architecture.hexagonal.SecondaryPort;
import org.joda.money.Money;

/// The fee of a finished ride (Pricing calculates it). Called by the use case, never by domain objects.
@SecondaryPort
public interface RideFees {

    Money feeOf(FinishedRide ride);
}
