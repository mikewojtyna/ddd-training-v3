package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery;

/// Rule: only a running recovery can be paused (DE-89).
public class RecoveryNotRunningException extends RuntimeException {

    public RecoveryNotRunningException(String message) {
        super(message);
    }

    public RecoveryNotRunningException(String message, Throwable cause) {
        super(message, cause);
    }
}
