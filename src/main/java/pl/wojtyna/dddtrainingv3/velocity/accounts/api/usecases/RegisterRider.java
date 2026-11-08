package pl.wojtyna.dddtrainingv3.velocity.accounts.api.usecases;

/// Inbound command *Register* (rider app): a person registers with an email address.
public record RegisterRider(String email) {
}
