package pl.wojtyna.dddtrainingv3.velocity.accounts.api.queries;

/// Inbound query *Account Status* (rider app): onboarding progress, activation and blocks.
public record AccountStatusQuery(String riderId) {
}
