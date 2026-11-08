package pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods;

/// Why a charge failed: the processor declined it, or the rider has no current card (DE-71).
public enum ChargeFailureReason {
    DECLINED, NO_VALID_PAYMENT_METHOD
}
