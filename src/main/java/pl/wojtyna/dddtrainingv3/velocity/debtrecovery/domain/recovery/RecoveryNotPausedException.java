package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery;

/// Rule: only a paused recovery can be resumed (DE-89).
public class RecoveryNotPausedException extends RuntimeException {

    public RecoveryNotPausedException(String message) {
        super(message);
    }

    public RecoveryNotPausedException(String message, Throwable cause) {
        super(message, cause);
    }
}
