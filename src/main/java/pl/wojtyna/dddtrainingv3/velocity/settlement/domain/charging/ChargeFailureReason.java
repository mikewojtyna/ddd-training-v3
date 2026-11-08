package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging;

/// Why a charge attempt failed, as Payments reports it: the card was declined, or the rider has no valid payment
/// method (no current card, DE-71).
public enum ChargeFailureReason {
    DECLINED, NO_VALID_PAYMENT_METHOD
}
