package pl.wojtyna.dddtrainingv3.velocity.settlement.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.RiderCards;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.RiderId;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.debt.NoOutstandingDebtException;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.debt.RiderDebtRepository;

import java.time.Clock;

/// *Pay Off Outstanding Debt* (DE-36): the whole debt in one charge. A declined charge is a result with
/// `collected=false` (R2); a rider without debt is rejected with `NoOutstandingDebtException` (R3).
@PrimaryPort
public class PayOffOutstandingDebtUseCase {

    private final OutstandingDebtCollector collector;

    public PayOffOutstandingDebtUseCase(RiderDebtRepository debts, RiderCards riderCards,
                                        DomainEventPublisher eventPublisher, Clock clock) {
        this.collector = new OutstandingDebtCollector(debts, riderCards, eventPublisher, clock);
    }

    /// @throws NoOutstandingDebtException if the rider owes nothing
    public DebtCollectionResult handle(PayOffOutstandingDebt command) {
        return collector.collect(new RiderId(command.riderId()));
    }
}
