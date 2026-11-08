package pl.wojtyna.dddtrainingv3.velocity.payments.api.usecases;

/// Result of *Verify Payment Method*: `declineReason` is `DECLINED` when the pre-authorization was refused, otherwise null.
public record PaymentMethodVerification(String paymentMethodId, boolean verified, String declineReason) {
}
