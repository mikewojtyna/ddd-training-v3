package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike;

import org.jmolecules.architecture.hexagonal.SecondaryPort;

/// "May this rider rent right now?" Accounts decides; Rentals only asks, at reservation and again at unlock (DE-80).
@SecondaryPort
public interface RentalEligibility {

    enum Eligibility {
        ELIGIBLE,
        NOT_ELIGIBLE
    }

    /// @throws RentalEligibilityUnknown if the answer can't be obtained (Rentals then refuses: fail closed)
    Eligibility of(Holder rider);
}
