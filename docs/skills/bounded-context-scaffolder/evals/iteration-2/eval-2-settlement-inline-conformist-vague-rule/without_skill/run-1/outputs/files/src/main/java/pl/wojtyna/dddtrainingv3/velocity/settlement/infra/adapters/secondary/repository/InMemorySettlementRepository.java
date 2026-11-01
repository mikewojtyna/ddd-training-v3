package pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.secondary.repository;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement.Settlement;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement.SettlementId;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement.SettlementRepository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@SecondaryAdapter
public class InMemorySettlementRepository implements SettlementRepository {

    private final Map<SettlementId, Settlement> settlements = new ConcurrentHashMap<>();

    @Override
    public Optional<Settlement> load(SettlementId id) {
        return Optional.ofNullable(settlements.get(id));
    }

    @Override
    public void save(Settlement settlement) {
        settlements.put(settlement.id(), settlement);
    }

    @Override
    public List<SettlementId> findDueForChargeAttempt(Instant now) {
        return settlements.values().stream()
                          .filter(settlement -> settlement.isDueForChargeAttempt(now))
                          .map(Settlement::id)
                          .toList();
    }
}
