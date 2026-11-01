package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelists;

import org.jmolecules.architecture.hexagonal.SecondaryPort;
import org.jmolecules.ddd.annotation.Repository;

import java.util.Optional;

@Repository
@SecondaryPort
public interface PriceListsRepository {

    Optional<PriceLists> load(PriceListsId id);

    void save(PriceLists priceLists);
}
