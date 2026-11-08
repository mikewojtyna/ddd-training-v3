package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery;

/// Where a rider's recovery process stands. *Running* and *paused* are **open**; *succeeded* and *exhausted* ended
/// it. `NONE`: the rider has no recovery process yet (never stored).
public enum RecoveryStatus {
    NONE, RUNNING, PAUSED, SUCCEEDED, EXHAUSTED;

    public boolean isOpen() {
        return this == RUNNING || this == PAUSED;
    }
}
