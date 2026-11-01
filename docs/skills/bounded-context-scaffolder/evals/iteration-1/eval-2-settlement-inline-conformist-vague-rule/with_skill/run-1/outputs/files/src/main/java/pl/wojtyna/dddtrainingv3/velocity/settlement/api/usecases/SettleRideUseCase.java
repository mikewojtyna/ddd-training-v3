package pl.wojtyna.dddtrainingv3.velocity.settlement.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charge.ChargeResult;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charge.ChargeRider;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charge.PaymentsCharging;

/// Starts the Settlement of a priced ride: the canvas says "once a ride is priced we charge the rider through their
/// payment method".
///
/// It makes the first Charge Attempt only. What happens after a failed attempt (how many retries, when, and when
/// Outstanding Debt is recorded) is the rule "retry a few times shortly after the ride, then record debt", which
/// waits for a strict Given/When/Then version. Until then there is no Settlement aggregate and nothing to save or
/// publish.
@PrimaryPort
public class SettleRideUseCase {

    private final PaymentsCharging payments;

    public SettleRideUseCase(PaymentsCharging payments) {
        this.payments = payments;
    }

    public ChargeResult handle(RidePriced ridePriced) {
        return payments.charge(new ChargeRider(ridePriced.rideId(), ridePriced.riderId(), ridePriced.price()));
    }
}
