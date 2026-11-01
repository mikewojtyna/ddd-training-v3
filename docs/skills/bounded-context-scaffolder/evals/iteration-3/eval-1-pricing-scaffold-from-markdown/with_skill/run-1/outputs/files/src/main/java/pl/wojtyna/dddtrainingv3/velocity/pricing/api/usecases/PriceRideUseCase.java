package pl.wojtyna.dddtrainingv3.velocity.pricing.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelists.*;

/// Prices a finished ride when its rental session ends. Pricing doesn't change `PriceLists`, so nothing is saved.
@PrimaryPort
public class PriceRideUseCase {

    private final PriceListsRepository priceListsRepository;
    private final ReturnChargesProvider returnChargesProvider;
    private final DomainEventPublisher eventPublisher;

    public PriceRideUseCase(PriceListsRepository priceListsRepository, ReturnChargesProvider returnChargesProvider,
                            DomainEventPublisher eventPublisher) {
        this.priceListsRepository = priceListsRepository;
        this.returnChargesProvider = returnChargesProvider;
        this.eventPublisher = eventPublisher;
    }

    public void handle(RentalSessionEnded event) {
        var ride = new Ride(new RentalSessionId(event.rentalSessionId()), new RiderId(event.riderId()),
                            event.startedAt(), event.endedAt());
        var priceLists = priceListsRepository.load(PriceListsId.VELOCITY)
                                             .orElseThrow(() -> new NoPriceListValidAt(ride.startedAt()));
        var returnCharges = returnChargesProvider.forSession(ride.rentalSessionId());
        var events = priceLists.price(ride, returnCharges);
        eventPublisher.publish(events);
    }
}
