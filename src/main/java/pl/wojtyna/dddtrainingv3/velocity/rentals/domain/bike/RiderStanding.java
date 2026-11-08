package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike;

import org.jmolecules.ddd.annotation.ValueObject;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.RentalEligibility.Eligibility;

/// What a reservation needs to know about the rider beyond this bike: Accounts' eligibility answer and whether the
/// rider already holds another bike (a reservation that still holds or a running session). The use case gathers both
/// (the eligibility port, the repository) and passes them in, so the bike decides (DE-13, DE-14).
@ValueObject
public record RiderStanding(Eligibility eligibility, boolean holdsAnotherBike) {

    public static RiderStanding eligibleHoldingNothing() {
        return new RiderStanding(Eligibility.ELIGIBLE, false);
    }
}
