package pl.wojtyna.dddtrainingv3.velocity.pricing.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist.PriceListRepository;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist.RentalSessionId;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist.ReturnChargesProvider;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist.Ride;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist.RiderId;

/// Canvas inbound event "Rental Session Ended" (from Rentals): prices the finished ride with the price list valid at
/// the ride's start and publishes "Ride Priced".
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

    public void handle(PriceRide command) {
        var rentalSessionId = new RentalSessionId(command.rentalSessionId());
        var ride = new Ride(command.startedAt(), command.endedAt());
        var priceList = priceLists.all().validAt(ride.startedAt());
        var returnCharges = returnChargesProvider.returnChargesFor(rentalSessionId);
        var events = priceList.priceRide(rentalSessionId, new RiderId(command.riderId()), ride, returnCharges);
        eventPublisher.publish(events);
    }
}
