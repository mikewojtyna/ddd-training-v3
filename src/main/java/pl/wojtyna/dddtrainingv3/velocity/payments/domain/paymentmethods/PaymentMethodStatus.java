package pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods;

/// Where a payment method stands. Only a `VERIFIED` method is the rider's current card.
public enum PaymentMethodStatus {
    VERIFIED, DECLINED, REPLACED, INVALIDATED
}
