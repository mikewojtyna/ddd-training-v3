package pl.wojtyna.dddtrainingv3.velocity.rentals.infra.adapters.secondary.rideraccountsctxacl;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BorrowerId;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.eligibility.BorrowerEligibility;

/// Stub: Rider Accounts doesn't exist in code yet (plan D4). Tests use a fake `BorrowerEligibility`.
@SecondaryAdapter
public class RiderAccountsEligibilityAdapter implements BorrowerEligibility {

    @Override
    public boolean isEligible(BorrowerId borrower) {
        // Canvas outbound query: "Can this rider rent?" -> Rider Accounts (RiderEligibility)
        throw new UnsupportedOperationException("implement this");
    }
}
