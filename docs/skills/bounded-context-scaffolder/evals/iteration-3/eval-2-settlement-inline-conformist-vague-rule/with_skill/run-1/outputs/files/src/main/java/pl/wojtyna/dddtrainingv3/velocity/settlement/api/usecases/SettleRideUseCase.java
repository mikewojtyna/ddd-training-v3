package pl.wojtyna.dddtrainingv3.velocity.settlement.api.usecases;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement.ChargeResult;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement.ChargeRider;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement.RideId;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement.RiderCharging;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement.RiderId;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement.Settlement;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement.SettlementRepository;

/// Reacts to "Ride Priced": opens the Settlement for the ride and makes the first Charge Attempt through Payments.
/// Retrying a failed attempt is not triggered yet: when retries happen is awaiting a strict GWT rule (see the plan).
@PrimaryPort
public class SettleRideUseCase {

    private final SettlementRepository settlements;
    private final RiderCharging riderCharging;
    private final DomainEventPublisher eventPublisher;

    public SettleRideUseCase(SettlementRepository settlements,
                             RiderCharging riderCharging,
                             DomainEventPublisher eventPublisher) {
        this.settlements = settlements;
        this.riderCharging = riderCharging;
        this.eventPublisher = eventPublisher;
    }

    public void handle(RidePriced ridePriced) {
        var price = Money.of(CurrencyUnit.of(ridePriced.currency()), ridePriced.amount());
        var settlement = new Settlement(new RideId(ridePriced.rideId()), new RiderId(ridePriced.riderId()), price);

        ChargeResult result = riderCharging.charge(new ChargeRider(ridePriced.rideId(),
                                                                   ridePriced.riderId(),
                                                                   ridePriced.amount(),
                                                                   ridePriced.currency()));
        var events = result.succeeded() ? settlement.chargeAttemptSucceeded() : settlement.chargeAttemptFailed();

        settlements.save(settlement);
        eventPublisher.publish(events);
    }
}
