package pl.wojtyna.dddtrainingv3.velocity.accounts.api.queries;

import org.jmolecules.architecture.hexagonal.SecondaryPort;

import java.util.Optional;

/// Read side of the accounts. Implementations answer from the `Account` itself, whose methods own the meaning of
/// "next onboarding step" and "rental eligibility".
@SecondaryPort
public interface AccountsReadModel {

    Optional<AccountStatus> statusOf(String riderId);

    Optional<RentalEligibilityView> rentalEligibilityOf(String riderId);
}
