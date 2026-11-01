package pl.wojtyna.dddtrainingv3.velocity.pricing.infra.adapters.secondary;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist.PriceList;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist.PriceListId;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist.PriceListRepository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@SecondaryAdapter
public class InMemoryPriceListRepository implements PriceListRepository {

    private final Map<PriceListId, PriceList> priceLists = new ConcurrentHashMap<>();

    @Override
    public Optional<PriceList> load(PriceListId id) {
        return Optional.ofNullable(priceLists.get(id));
    }

    @Override
    public Collection<PriceList> all() {
        return List.copyOf(priceLists.values());
    }

    @Override
    public void save(PriceList priceList) {
        priceLists.put(priceList.id(), priceList);
    }
}
