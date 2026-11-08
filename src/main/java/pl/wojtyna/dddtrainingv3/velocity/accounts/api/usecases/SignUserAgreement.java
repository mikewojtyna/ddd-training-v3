package pl.wojtyna.dddtrainingv3.velocity.accounts.api.usecases;

/// Inbound command *Sign User Agreement* (rider app): the last onboarding step, activates the account (DE-4).
public record SignUserAgreement(String riderId) {
}
