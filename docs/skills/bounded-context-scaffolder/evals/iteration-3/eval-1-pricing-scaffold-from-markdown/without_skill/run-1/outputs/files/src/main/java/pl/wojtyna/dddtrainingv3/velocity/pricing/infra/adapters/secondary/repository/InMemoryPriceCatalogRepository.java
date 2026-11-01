package pl.wojtyna.dddtrainingv3.velocity.pricing.infra.adapters.secondary.repository;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricecatalog.PriceCatalog;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricecatalog.PriceCatalogId;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricecatalog.PriceCatalogRepository;

@SecondaryAdapter
public class InMemoryPriceCatalogRepository implements PriceCatalogRepository {

    private PriceCatalog priceCatalog = new PriceCatalog(PriceCatalogId.VELOCITY);

    @Override
    public synchronized PriceCatalog load() {
        return priceCatalog;
    }

    @Override
    public synchronized void save(PriceCatalog priceCatalog) {
        this.priceCatalog = priceCatalog;
    }
}
