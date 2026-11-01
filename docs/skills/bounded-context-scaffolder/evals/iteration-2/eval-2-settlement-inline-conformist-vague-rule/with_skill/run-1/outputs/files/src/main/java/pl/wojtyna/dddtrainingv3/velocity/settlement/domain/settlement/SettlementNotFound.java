package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement;

import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.RideId;

public class SettlementNotFound extends RuntimeException {

    public SettlementNotFound(RideId rideId) {
        super("No settlement for ride " + rideId.value());
    }
}
