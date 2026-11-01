package pl.wojtyna.dddtrainingv3.velocity.settlement.api.usecases;

import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.RideId;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.RiderCharger;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.RiderId;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement.Settlement;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement.SettlementRepository;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

/// Starts the Settlement of a priced ride and makes its first Charge Attempt. The outcome of the attempt is reported
/// through `RecordChargeAttemptOutcomeUseCase` (plan, Q5). When retries are made is not modelled yet (plan).
@PrimaryPort
public class SettleRideUseCase {

    private final SettlementRepository settlements;
    private final RiderCharger riderCharger;

    public SettleRideUseCase(SettlementRepository settlements, RiderCharger riderCharger) {
        this.settlements = settlements;
        this.riderCharger = riderCharger;
    }

    public void handle(RidePriced ridePriced) {
        var rideId = new RideId(ridePriced.rideId());
        var riderId = new RiderId(ridePriced.riderId());
        settlements.save(Settlement.forPricedRide(rideId, riderId, ridePriced.price()));
        riderCharger.chargeRider(rideId, riderId, ridePriced.price());
    }
}
