package pl.wojtyna.dddtrainingv3.velocity.payments.api.usecases;

/// Inbound command *Verify Payment Method* (from Accounts): verify the rider's card, known by the processor's token.
public record VerifyPaymentMethod(String riderId, String cardToken) {
}
