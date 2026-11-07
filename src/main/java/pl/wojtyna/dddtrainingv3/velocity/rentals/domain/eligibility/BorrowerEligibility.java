package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.eligibility;

import org.jmolecules.architecture.hexagonal.SecondaryPort;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BorrowerId;

/// Canvas outbound query *Can this rider rent?* → Rider Accounts, in Rentals' language: is this borrower eligible?
/// A borrower is eligible when Rider Accounts reports the account as activated (rule R6).
/// The translation from Rider Accounts' account status lives in `infra.adapters.secondary.rideraccountsctxacl`.
@SecondaryPort
public interface BorrowerEligibility {

    boolean isEligible(BorrowerId borrower);
}
