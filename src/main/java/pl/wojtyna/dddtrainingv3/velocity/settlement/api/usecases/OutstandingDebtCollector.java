package pl.wojtyna.dddtrainingv3.velocity.settlement.api.usecases;

import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.common.domain.OptimisticLockingException;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.ChargeOutcome;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.Pln;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.RiderCards;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.RiderId;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.debt.DebtCollection;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.debt.RiderDebt;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.debt.RiderDebtRepository;

import java.time.Clock;
import java.util.UUID;

/// *Collection*, one behaviour for two askers, the rider and Debt Recovery (plan `rider-pays-off-outstanding-debt`
/// D5, D6): load the rider's debt (no debt: `NoOutstandingDebtException`) → charge the whole of it through Payments →
/// record the outcome → save → publish. Clearing the debt publishes Outstanding Debt Cleared, which lifts the block
/// (policy `UnblockRiderWhenDebtClearedPolicy`).
///
/// If another unpaid fee was added to the debt while the charge was made, the save conflicts; the debt is reloaded and
/// the same (already charged) collection recorded again, which clears only the rides it charged.
class OutstandingDebtCollector {

    private static final int MAX_SAVES = 3;

    private final RiderDebtRepository debts;
    private final RiderCards riderCards;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;

    OutstandingDebtCollector(RiderDebtRepository debts, RiderCards riderCards, DomainEventPublisher eventPublisher,
                             Clock clock) {
        this.debts = debts;
        this.riderCards = riderCards;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    DebtCollectionResult collect(RiderId rider) {
        var debt = load(rider);
        var collection = debt.collection();
        var outcome = riderCards.charge(collection.chargeAs("DEBT-" + rider.value() + "-" + UUID.randomUUID()));
        var now = clock.instant();
        for (var save = 1; ; save++) {
            var events = debt.record(collection).as(outcome).at(now);
            try {
                debts.save(debt);
                eventPublisher.publish(events);
                return result(collection, outcome, debt);
            } catch (OptimisticLockingException concurrentChange) {
                if (save == MAX_SAVES) {
                    throw concurrentChange;
                }
                debt = load(rider);
            }
        }
    }

    private RiderDebt load(RiderId rider) {
        return debts.load(rider).orElseGet(() -> RiderDebt.none(rider));
    }

    private static DebtCollectionResult result(DebtCollection collection, ChargeOutcome outcome, RiderDebt debt) {
        return new DebtCollectionResult(outcome.succeeded(),
                                        (outcome.succeeded() ? collection.amount() : Pln.zero()).getAmount().toPlainString(),
                                        debt.outstandingDebt().getAmount().toPlainString());
    }
}
