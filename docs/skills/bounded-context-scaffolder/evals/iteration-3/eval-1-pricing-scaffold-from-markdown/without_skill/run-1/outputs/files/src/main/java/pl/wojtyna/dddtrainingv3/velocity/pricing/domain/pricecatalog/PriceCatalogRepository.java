package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricecatalog;

import org.jmolecules.architecture.hexagonal.SecondaryPort;

@SecondaryPort
public interface PriceCatalogRepository {

    PriceCatalog load();

    void save(PriceCatalog priceCatalog);
}
