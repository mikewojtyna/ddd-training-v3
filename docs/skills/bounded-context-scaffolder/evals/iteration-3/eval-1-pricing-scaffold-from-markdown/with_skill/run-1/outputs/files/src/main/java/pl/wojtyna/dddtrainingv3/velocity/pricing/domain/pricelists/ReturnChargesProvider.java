package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelists;

import org.jmolecules.architecture.hexagonal.SecondaryPort;

/// Canvas outbound query "Get Return Charges" (Rentals), in Pricing's language.
@SecondaryPort
public interface ReturnChargesProvider {

    ReturnCharges forSession(RentalSessionId rentalSessionId);
}
