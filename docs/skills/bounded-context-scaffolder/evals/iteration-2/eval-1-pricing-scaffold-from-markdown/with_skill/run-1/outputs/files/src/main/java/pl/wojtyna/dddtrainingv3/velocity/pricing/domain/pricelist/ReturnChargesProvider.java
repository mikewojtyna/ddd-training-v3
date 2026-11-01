package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist;

import org.jmolecules.architecture.hexagonal.SecondaryPort;

/// Canvas outbound query "Get Return Charges" (to Rentals), in Pricing's language.
@SecondaryPort
public interface ReturnChargesProvider {

    ReturnCharges returnChargesFor(RentalSessionId rentalSessionId);
}
