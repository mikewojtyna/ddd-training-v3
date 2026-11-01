package pl.wojtyna.dddtrainingv3.velocity.pricing.infra.adapters.secondary;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist.PriceList;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist.PriceListId;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist.PriceListRepository;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist.PriceLists;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SecondaryAdapter
public class InMemoryPriceListRepository implements PriceListRepository {

    private final Map<PriceListId, PriceList> priceLists = new ConcurrentHashMap<>();

    @Override
    public void save(PriceList priceList) {
        priceLists.put(priceList.id(), priceList);
    }

    @Override
    public PriceLists all() {
        return new PriceLists(List.copyOf(priceLists.values()));
    }
}
