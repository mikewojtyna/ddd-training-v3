package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.eligibility;

import org.jmolecules.ddd.annotation.ValueObject;

/// Whether a borrower may reserve: eligible when Rider Accounts reports the account as activated (rule R6).
/// The use case asks `BorrowerEligibility` and passes the answer into the command.
@ValueObject
public enum Eligibility {
    ELIGIBLE,
    NOT_ELIGIBLE
}
