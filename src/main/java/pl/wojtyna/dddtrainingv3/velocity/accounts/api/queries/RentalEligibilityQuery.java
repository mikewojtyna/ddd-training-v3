package pl.wojtyna.dddtrainingv3.velocity.accounts.api.queries;

/// Inbound query *Rental Eligibility* (Rentals, OHS): may this rider rent right now?
public record RentalEligibilityQuery(String riderId) {
}
