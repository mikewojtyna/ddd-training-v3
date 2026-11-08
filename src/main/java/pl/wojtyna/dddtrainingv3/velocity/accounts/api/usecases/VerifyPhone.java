package pl.wojtyna.dddtrainingv3.velocity.accounts.api.usecases;

/// Inbound command *Verify Phone* (rider app, after the messaging provider checked the SMS code, DE-1).
public record VerifyPhone(String riderId, String phoneNumber) {
}
