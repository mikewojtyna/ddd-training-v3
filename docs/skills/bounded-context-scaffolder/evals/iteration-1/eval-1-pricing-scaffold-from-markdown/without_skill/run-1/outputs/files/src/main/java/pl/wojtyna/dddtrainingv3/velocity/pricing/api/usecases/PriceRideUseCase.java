package pl.wojtyna.dddtrainingv3.velocity.pricing.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricecatalog.PriceCatalogRepository;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricecatalog.ReturnChargesProvider;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricecatalog.Ride;

/**
 * Prices a finished ride. Triggered by Rentals' Rental Session Ended, results in Ride Priced.
 */
@PrimaryPort
public class PriceRideUseCase {

    private final PriceCatalogRepository priceCatalogRepository;
    private final ReturnChargesProvider returnChargesProvider;
    private final DomainEventPublisher eventPublisher;

    public PriceRideUseCase(PriceCatalogRepository priceCatalogRepository,
                            ReturnChargesProvider returnChargesProvider,
                            DomainEventPublisher eventPublisher) {
        this.priceCatalogRepository = priceCatalogRepository;
        this.returnChargesProvider = returnChargesProvider;
        this.eventPublisher = eventPublisher;
    }

    public void price(Ride ride) {
        var priceCatalog = priceCatalogRepository.load();
        var returnCharges = returnChargesProvider.returnChargesFor(ride.sessionId());
        var events = priceCatalog.price(ride).withReturnCharges(returnCharges);
        // pricing doesn't change the catalog, so there is nothing to save
        eventPublisher.publish(events);
    }
}
