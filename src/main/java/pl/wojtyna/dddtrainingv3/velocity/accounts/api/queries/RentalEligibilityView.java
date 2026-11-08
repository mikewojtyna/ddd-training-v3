package pl.wojtyna.dddtrainingv3.velocity.accounts.api.queries;

/// Result of *Rental Eligibility*: `reason` is `ELIGIBLE | ONBOARDING_INCOMPLETE | BLOCKED | UNKNOWN_RIDER`.
public record RentalEligibilityView(String riderId, boolean eligible, String reason) {

    public static RentalEligibilityView unknownRider(String riderId) {
        return new RentalEligibilityView(riderId, false, "UNKNOWN_RIDER");
    }
}
