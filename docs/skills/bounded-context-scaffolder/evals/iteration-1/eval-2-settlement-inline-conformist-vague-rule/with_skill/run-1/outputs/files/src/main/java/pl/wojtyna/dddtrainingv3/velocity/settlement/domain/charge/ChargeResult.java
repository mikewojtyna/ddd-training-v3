package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charge;

/// Outcome of one Charge Attempt, as Payments reports it.
///
/// Placeholder for Payments' published result type (Settlement is conformist). The canvas only says that a charge
/// can fail; it doesn't say how Payments reports it, so only the two outcomes the rule needs are here.
public enum ChargeResult {
    CHARGED,
    FAILED
}
