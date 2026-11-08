package pl.wojtyna.dddtrainingv3.velocity.payments.api.usecases;

/// Inbound event *Card Invalidated* from the payment processor, as our command: the card known by `cardToken` is no
/// longer usable; `reason` is `EXPIRED`, `CANCELLED` or `REPORTED_LOST`.
public record InvalidatePaymentMethod(String cardToken, String reason) {
}
