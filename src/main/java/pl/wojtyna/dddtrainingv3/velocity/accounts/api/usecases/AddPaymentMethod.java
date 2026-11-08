package pl.wojtyna.dddtrainingv3.velocity.accounts.api.usecases;

/// Inbound command *Add Payment Method* (rider app): during onboarding, or to replace the card later (DE-5).
public record AddPaymentMethod(String riderId, String cardToken) {
}
