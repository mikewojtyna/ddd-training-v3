package pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.primary;

import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.context.event.EventListener;
import pl.wojtyna.dddtrainingv3.velocity.pricing.api.events.RidePriced;
import pl.wojtyna.dddtrainingv3.velocity.settlement.api.usecases.SettleRideUseCase;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement.RideId;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement.RiderId;

/**
 * Consumes Pricing's Ride Priced event. Settlement conforms to Pricing's published language.
 */
@PrimaryAdapter
public class RidePricedListener {

    private final SettleRideUseCase settleRideUseCase;

    public RidePricedListener(SettleRideUseCase settleRideUseCase) {
        this.settleRideUseCase = settleRideUseCase;
    }

    @EventListener
    public void on(RidePriced ridePriced) {
        settleRideUseCase.settle(new RideId(ridePriced.rideId()), new RiderId(ridePriced.riderId()),
                                 ridePriced.totalFee(), ridePriced.pricedAt());
    }
}
