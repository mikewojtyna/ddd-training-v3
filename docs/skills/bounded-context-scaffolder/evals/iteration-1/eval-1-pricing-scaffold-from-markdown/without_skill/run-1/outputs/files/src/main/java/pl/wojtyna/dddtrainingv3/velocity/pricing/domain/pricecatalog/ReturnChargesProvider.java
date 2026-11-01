package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricecatalog;

import org.jmolecules.architecture.hexagonal.SecondaryPort;

/**
 * Get Return Charges: drop-zone or unauthorized-zone charges of a rental session. Rentals owns them.
 */
@SecondaryPort
public interface ReturnChargesProvider {

    ReturnCharges returnChargesFor(RentalSessionId sessionId);
}
