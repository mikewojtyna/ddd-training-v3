package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery;

/// Why a recovery is paused: the closed list of DE-42/DE-89. A reason outside it can't be represented.
public enum PauseReason {
    DISPUTE, PAYMENT_PLAN, REGULATORY_HOLD
}
