package pl.wojtyna.dddtrainingv3.velocity.settlement.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.RiderCards;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.RiderId;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.debt.NoOutstandingDebtException;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.debt.RiderDebtRepository;

import java.time.Clock;

/// *Collect Outstanding Debt*, for Debt Recovery (Customer/Supplier): the same collection as the rider's payoff.
/// Unlike the payoff, a rider who owes nothing (e.g. the debt was just paid off in the app) isn't an error for Debt
/// Recovery: the answer is `collected=false`, `outstandingDebt=0.00`, which Debt Recovery reads as succeeded (plan
/// `collections-attempts-debt-collection`, risks).
@PrimaryPort
public class CollectOutstandingDebtUseCase {

    private final OutstandingDebtCollector collector;

    public CollectOutstandingDebtUseCase(RiderDebtRepository debts, RiderCards riderCards,
                                         DomainEventPublisher eventPublisher, Clock clock) {
        this.collector = new OutstandingDebtCollector(debts, riderCards, eventPublisher, clock);
    }

    public DebtCollectionResult handle(CollectOutstandingDebt command) {
        try {
            return collector.collect(new RiderId(command.riderId()));
        } catch (NoOutstandingDebtException nothingOwed) {
            return new DebtCollectionResult(false, "0.00", "0.00");
        }
    }
}
