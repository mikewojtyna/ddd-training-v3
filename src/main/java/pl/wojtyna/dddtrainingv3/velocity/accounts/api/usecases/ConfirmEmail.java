package pl.wojtyna.dddtrainingv3.velocity.accounts.api.usecases;

/// Inbound command *Confirm Email* (rider app, after the messaging provider checked the link, DE-1).
public record ConfirmEmail(String riderId) {
}
