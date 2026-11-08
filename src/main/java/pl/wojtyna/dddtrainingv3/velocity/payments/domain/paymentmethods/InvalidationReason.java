package pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods;

/// Why the processor reported a card as no longer usable.
public enum InvalidationReason {
    EXPIRED, CANCELLED, REPORTED_LOST
}
