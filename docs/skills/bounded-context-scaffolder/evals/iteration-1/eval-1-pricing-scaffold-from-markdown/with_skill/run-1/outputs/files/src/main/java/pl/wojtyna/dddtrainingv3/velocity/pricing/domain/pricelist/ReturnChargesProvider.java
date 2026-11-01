package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist;

import org.jmolecules.architecture.hexagonal.SecondaryPort;

/// Return charges for a finished ride, in Pricing's language.
///
/// Canvas outbound query: "Get Return Charges" → Rentals (with ACL). Canvas assumption: return charges are always
/// known by the time the session ends, so this is a synchronous query.
@SecondaryPort
public interface ReturnChargesProvider {

    ReturnCharges returnChargesFor(RentalSessionId rentalSessionId);
}
