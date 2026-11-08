package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.debt;

import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.common.domain.DomainPolicy;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.ride.RideSettlementEvent.RideFeeUnpaid;

/// Whenever a ride's fee stays unpaid after its 3rd attempt, the fee is added to the rider's outstanding debt
/// (Rental Fee Unpaid, DE-35).
public class UnpaidRideFeeBecomesOutstandingDebtPolicy implements DomainPolicy {

    private final RiderDebtRepository debts;
    private final DomainEventPublisher eventPublisher;

    public UnpaidRideFeeBecomesOutstandingDebtPolicy(RiderDebtRepository debts, DomainEventPublisher eventPublisher) {
        this.debts = debts;
        this.eventPublisher = eventPublisher;
    }

    public void handle(RideFeeUnpaid rideFeeUnpaid) {
        var debt = debts.load(rideFeeUnpaid.rider()).orElseGet(() -> RiderDebt.none(rideFeeUnpaid.rider()));
        var events = debt.addUnpaidFee(rideFeeUnpaid.session()).of(rideFeeUnpaid.fee()).at(rideFeeUnpaid.unpaidAt());
        debts.save(debt);
        eventPublisher.publish(events);
    }
}
