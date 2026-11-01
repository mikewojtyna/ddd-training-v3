package pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.secondary.paymentsctx;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charge.ChargeResult;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charge.ChargeRider;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charge.PaymentsCharging;

@SecondaryAdapter
public class PaymentsChargingAdapter implements PaymentsCharging {

    @Override
    public ChargeResult charge(ChargeRider command) {
        // Canvas outbound command: "Charge Rider" -> Payments (conformist, no ACL).
        // Payments doesn't exist in code yet: call its published charging API here once it does.
        throw new UnsupportedOperationException("implement this");
    }
}
