package pl.wojtyna.dddtrainingv3.velocity.pricing.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist.NoPriceListValidAt;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist.PriceList;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist.PriceListRepository;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist.RentalSessionId;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist.ReturnChargesProvider;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist.Ride;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist.RiderId;

/// Reacts to the canvas inbound event "Rental Session Ended" (Rentals) by pricing the ride, which publishes
/// "Ride Priced" (outbound event to Settlement).
///
/// Pricing a ride doesn't change the price list, so there's no save step.
@PrimaryPort
public class PriceRideUseCase {

    private final PriceListRepository priceLists;
    private final ReturnChargesProvider returnChargesProvider;
    private final DomainEventPublisher eventPublisher;

    public PriceRideUseCase(PriceListRepository priceLists, ReturnChargesProvider returnChargesProvider,
                            DomainEventPublisher eventPublisher) {
        this.priceLists = priceLists;
        this.returnChargesProvider = returnChargesProvider;
        this.eventPublisher = eventPublisher;
    }

    public void handle(RentalSessionEnded event) {
        var ride = new Ride(new RentalSessionId(event.rentalSessionId()), new RiderId(event.riderId()),
                            event.startedAt(), event.endedAt());
        var priceList = PriceList.validAt(priceLists.all(), ride.startedAt())
                                 .orElseThrow(() -> new NoPriceListValidAt(ride.startedAt()));
        var returnCharges = returnChargesProvider.returnChargesFor(ride.rentalSessionId());
        var events = priceList.price(ride, returnCharges);
        eventPublisher.publish(events);
    }
}
