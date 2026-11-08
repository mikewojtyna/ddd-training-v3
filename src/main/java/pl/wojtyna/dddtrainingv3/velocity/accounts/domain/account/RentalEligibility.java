package pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account;

/// The answer to "may this rider rent right now?": only an activated account without blocks is eligible.
public enum RentalEligibility {
    ELIGIBLE, ONBOARDING_INCOMPLETE, BLOCKED;

    public boolean eligible() {
        return this == ELIGIBLE;
    }
}
