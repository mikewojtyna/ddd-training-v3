package pl.wojtyna.dddtrainingv3.velocity.settlement.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.joda.money.Money;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement.ChargeRetryPolicy;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement.RideId;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement.RiderId;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement.Settlement;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement.SettlementId;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement.SettlementRepository;

import java.time.Instant;

/**
 * Starts settling a priced ride and makes the first charge attempt right away. A ride that is already being settled
 * is ignored, so a redelivered Ride Priced event never charges the rider twice.
 */
@PrimaryPort
public class SettleRideUseCase {

    private final SettlementRepository settlementRepository;
    private final AttemptChargeUseCase attemptChargeUseCase;
    private final ChargeRetryPolicy retryPolicy;

    public SettleRideUseCase(SettlementRepository settlementRepository, AttemptChargeUseCase attemptChargeUseCase,
                             ChargeRetryPolicy retryPolicy) {
        this.settlementRepository = settlementRepository;
        this.attemptChargeUseCase = attemptChargeUseCase;
        this.retryPolicy = retryPolicy;
    }

    public void settle(RideId rideId, RiderId riderId, Money totalFee, Instant pricedAt) {
        var settlementId = SettlementId.forRide(rideId);
        if (settlementRepository.load(settlementId).isPresent()) {
            return;
        }
        settlementRepository.save(Settlement.open(rideId, riderId, totalFee, pricedAt, retryPolicy));
        attemptChargeUseCase.attempt(settlementId);
    }
}
