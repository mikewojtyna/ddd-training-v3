package pl.wojtyna.dddtrainingv3.velocity.pricing.infra.adapters.secondary;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelists.PriceLists;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelists.PriceListsId;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelists.PriceListsRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@SecondaryAdapter
public class InMemoryPriceListsRepository implements PriceListsRepository {

    private final Map<PriceListsId, PriceLists> priceLists = new ConcurrentHashMap<>();

    @Override
    public Optional<PriceLists> load(PriceListsId id) {
        return Optional.ofNullable(priceLists.get(id));
    }

    @Override
    public void save(PriceLists priceLists) {
        this.priceLists.put(priceLists.id(), priceLists);
    }
}
