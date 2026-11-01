package pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.secondary.paymentsctx;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement.ChargeResult;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement.ChargeRider;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement.RiderCharging;

@SecondaryAdapter
public class PaymentsRiderChargingAdapter implements RiderCharging {

    @Override
    public ChargeResult charge(ChargeRider command) {
        // Canvas outbound command: "Charge Rider" → Payments (conformist, D1). Payments isn't in code yet.
        throw new UnsupportedOperationException("implement this");
    }
}
