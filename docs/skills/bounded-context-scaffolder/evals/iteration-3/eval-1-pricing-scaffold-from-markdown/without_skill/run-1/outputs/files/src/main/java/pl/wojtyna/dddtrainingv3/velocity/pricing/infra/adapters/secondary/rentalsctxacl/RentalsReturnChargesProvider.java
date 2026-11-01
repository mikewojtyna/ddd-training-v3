package pl.wojtyna.dddtrainingv3.velocity.pricing.infra.adapters.secondary.rentalsctxacl;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricecatalog.RentalSessionId;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricecatalog.ReturnCharges;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricecatalog.ReturnChargesProvider;

@SecondaryAdapter
public class RentalsReturnChargesProvider implements ReturnChargesProvider {

    @Override
    public ReturnCharges returnChargesFor(RentalSessionId sessionId) {
        // Rentals doesn't expose a Get Return Charges query yet. When it does, call it here and translate
        // its drop-zone / unauthorized-zone charges into ReturnCharges.
        throw new UnsupportedOperationException("implement this");
    }
}
