package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recoveryprocess;

public enum RecoveryStatus {
    ACTIVE,
    PAUSED,
    SUCCEEDED,
    EXHAUSTED;

    boolean isClosed() {
        return this == SUCCEEDED || this == EXHAUSTED;
    }
}
