package pl.wojtyna.dddtrainingv3.velocity.pricing.infra.adapters.primary.rentalsctxacl;

import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.context.event.EventListener;
import pl.wojtyna.dddtrainingv3.velocity.pricing.api.usecases.PriceRideUseCase;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricecatalog.RentalSessionId;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricecatalog.Ride;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricecatalog.RiderId;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.events.RentalSessionEnded;

@PrimaryAdapter
public class RentalSessionEndedListener {

    private final PriceRideUseCase priceRideUseCase;

    public RentalSessionEndedListener(PriceRideUseCase priceRideUseCase) {
        this.priceRideUseCase = priceRideUseCase;
    }

    @EventListener
    public void on(RentalSessionEnded event) {
        priceRideUseCase.price(toRide(event));
    }

    private Ride toRide(RentalSessionEnded event) {
        return new Ride(new RentalSessionId(event.rentalSessionId()), new RiderId(event.riderId()),
                        event.startedAt(), event.endedAt());
    }
}
