package pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.secondary;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.joda.money.Money;
import pl.wojtyna.dddtrainingv3.common.domain.OptimisticLockingException;
import pl.wojtyna.dddtrainingv3.common.domain.Version;
import pl.wojtyna.dddtrainingv3.velocity.settlement.api.queries.RiderBalanceView;
import pl.wojtyna.dddtrainingv3.velocity.settlement.api.queries.RiderBalanceView.UnpaidRideView;
import pl.wojtyna.dddtrainingv3.velocity.settlement.api.queries.RiderBalancesReadModel;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.Pln;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.RiderId;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.debt.RiderDebt;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.debt.RiderDebtRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/// Riders' outstanding debts in memory, with optimistic locking on `Version`. Also serves the Rider Balance query.
@SecondaryAdapter
public class InMemoryRiderDebtRepository implements RiderDebtRepository, RiderBalancesReadModel {

    private final Map<RiderId, RiderDebt.Snapshot> debts = new ConcurrentHashMap<>();

    @Override
    public Optional<RiderDebt> load(RiderId rider) {
        return Optional.ofNullable(debts.get(rider)).map(RiderDebt::restore);
    }

    @Override
    public void save(RiderDebt debt) {
        var snapshot = debt.snapshot();
        debts.compute(snapshot.rider(), (rider, stored) -> {
            var storedVersion = stored == null ? Version.initial() : stored.version();
            if (!storedVersion.equals(snapshot.version())) {
                throw new OptimisticLockingException("Outstanding debt of rider " + rider.value() + " was modified concurrently");
            }
            return new RiderDebt.Snapshot(rider, snapshot.version().next(), snapshot.unpaidRides());
        });
    }

    @Override
    public Optional<RiderBalanceView> balanceOf(String riderId) {
        return Optional.ofNullable(debts.get(new RiderId(riderId)))
                       .map(debt -> new RiderBalanceView(
                           riderId,
                           debt.unpaidRides().values().stream().reduce(Pln.zero(), Money::plus).getAmount().toPlainString(),
                           debt.unpaidRides().entrySet().stream()
                               .map(ride -> new UnpaidRideView(ride.getKey().value(), ride.getValue().getAmount().toPlainString()))
                               .toList()));
    }
}
