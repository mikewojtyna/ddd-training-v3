package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging;

import org.jmolecules.architecture.hexagonal.SecondaryPort;
import org.joda.money.Money;

/// Canvas outbound command "Charge Rider" to Payments.
///
/// Settlement is conformist to Payments (plan, D1): once Payments exists in code, this port switches to Payments'
/// published types instead of our own.
@SecondaryPort
public interface RiderCharger {

    void chargeRider(RideId rideId, RiderId riderId, Money amount);
}
