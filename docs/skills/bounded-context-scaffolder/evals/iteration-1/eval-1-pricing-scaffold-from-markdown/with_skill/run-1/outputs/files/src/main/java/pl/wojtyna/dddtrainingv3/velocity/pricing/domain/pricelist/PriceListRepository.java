package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist;

import org.jmolecules.architecture.hexagonal.SecondaryPort;
import org.jmolecules.ddd.annotation.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
@SecondaryPort
public interface PriceListRepository {

    Optional<PriceList> load(PriceListId id);

    /// All published price lists. Pick the one valid at a moment with `PriceList.validAt(...)`, so the
    /// "price valid at ride start" rule stays in the domain and out of the adapters.
    Collection<PriceList> all();

    void save(PriceList priceList);
}
