package pl.wojtyna.dddtrainingv3.velocity.pricing.infra.adapters.secondary.rentalsctxacl;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelists.RentalSessionId;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelists.ReturnCharges;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelists.ReturnChargesProvider;

@SecondaryAdapter
public class RentalsReturnChargesAdapter implements ReturnChargesProvider {

    @Override
    public ReturnCharges forSession(RentalSessionId rentalSessionId) {
        // Canvas outbound query: "Get Return Charges" → Rentals. Rentals has no return-charges query in its api yet.
        throw new UnsupportedOperationException("implement this");
    }
}
