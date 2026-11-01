package pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.secondary;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement.RideId;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement.Settlement;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement.SettlementRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@SecondaryAdapter
public class InMemorySettlementRepository implements SettlementRepository {

    private final Map<RideId, Settlement> settlements = new ConcurrentHashMap<>();

    @Override
    public Optional<Settlement> load(RideId rideId) {
        return Optional.ofNullable(settlements.get(rideId));
    }

    @Override
    public void save(Settlement settlement) {
        settlements.put(settlement.id(), settlement);
    }
}
