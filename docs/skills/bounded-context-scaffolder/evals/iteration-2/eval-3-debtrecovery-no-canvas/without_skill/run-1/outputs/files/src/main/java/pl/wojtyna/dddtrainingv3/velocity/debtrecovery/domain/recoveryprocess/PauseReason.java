package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recoveryprocess;

import org.jmolecules.ddd.annotation.ValueObject;

/**
 * Legitimate reasons to pause recovery without giving up on it (from the vision's business rules).
 */
@ValueObject
public enum PauseReason {
    DISPUTE_UNDER_REVIEW,
    PAYMENT_PLAN_NEGOTIATION,
    REGULATORY_HOLD
}
