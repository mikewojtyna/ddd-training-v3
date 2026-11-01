package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist;

import org.jmolecules.architecture.hexagonal.SecondaryPort;
import org.jmolecules.ddd.annotation.Repository;

@Repository
@SecondaryPort
public interface PriceListRepository {

    void save(PriceList priceList);

    PriceLists all();
}
