package pl.wojtyna.dddtrainingv3.velocity.pricing.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricecatalog.PriceCatalogRepository;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricecatalog.PriceList;

/**
 * Publish Price List: the back office publishes a new price list valid from a given moment.
 */
@PrimaryPort
public class PublishPriceListUseCase {

    private final PriceCatalogRepository priceCatalogRepository;
    private final DomainEventPublisher eventPublisher;

    public PublishPriceListUseCase(PriceCatalogRepository priceCatalogRepository,
                                   DomainEventPublisher eventPublisher) {
        this.priceCatalogRepository = priceCatalogRepository;
        this.eventPublisher = eventPublisher;
    }

    public void publish(PriceList priceList) {
        var priceCatalog = priceCatalogRepository.load();
        var events = priceCatalog.publish(priceList);
        priceCatalogRepository.save(priceCatalog);
        eventPublisher.publish(events);
    }
}
