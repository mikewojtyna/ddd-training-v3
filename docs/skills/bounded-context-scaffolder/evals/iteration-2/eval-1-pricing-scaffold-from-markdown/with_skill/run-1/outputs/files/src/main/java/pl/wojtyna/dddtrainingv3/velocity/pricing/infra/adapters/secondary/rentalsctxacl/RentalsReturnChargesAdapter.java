package pl.wojtyna.dddtrainingv3.velocity.pricing.infra.adapters.secondary.rentalsctxacl;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist.RentalSessionId;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist.ReturnCharges;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist.ReturnChargesProvider;

@SecondaryAdapter
public class RentalsReturnChargesAdapter implements ReturnChargesProvider {

    @Override
    public ReturnCharges returnChargesFor(RentalSessionId rentalSessionId) {
        // Canvas outbound query: "Get Return Charges" -> Rentals.
        // Rentals has no return-charges query in code yet, so there is no published API to translate from.
        throw new UnsupportedOperationException("implement this");
    }
}
