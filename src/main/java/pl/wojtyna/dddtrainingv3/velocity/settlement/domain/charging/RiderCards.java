package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging;

import org.jmolecules.architecture.hexagonal.SecondaryPort;

/// Charging the rider's card (Payments decides which card: the rider's current one, DE-5). Called by use cases,
/// never by domain objects.
@SecondaryPort
public interface RiderCards {

    ChargeOutcome charge(Charge charge);
}
