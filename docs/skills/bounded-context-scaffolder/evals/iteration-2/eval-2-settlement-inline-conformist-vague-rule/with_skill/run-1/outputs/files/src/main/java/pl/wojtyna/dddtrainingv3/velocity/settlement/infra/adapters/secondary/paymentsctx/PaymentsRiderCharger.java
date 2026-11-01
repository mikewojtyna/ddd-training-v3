package pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.secondary.paymentsctx;

import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.RideId;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.RiderCharger;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.RiderId;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.joda.money.Money;

@SecondaryAdapter
public class PaymentsRiderCharger implements RiderCharger {

    @Override
    public void chargeRider(RideId rideId, RiderId riderId, Money amount) {
        // Canvas outbound command: "Charge Rider" -> Payments. Payments isn't in code yet.
        throw new UnsupportedOperationException("implement this");
    }
}
