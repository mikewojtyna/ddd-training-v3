package pl.wojtyna.dddtrainingv3.velocity.accounts.api.usecases;

/// Result of *Add Payment Method*: whether Payments verified the card, and its reference when it did (else null).
public record PaymentMethodAddition(boolean verified, String paymentMethodId) {
}
