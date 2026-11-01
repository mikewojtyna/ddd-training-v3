package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recoveryprocess;

/**
 * Thrown when a recovery process that is not paused is asked to resume.
 */
public class RecoveryProcessNotPausedException extends RuntimeException {

    public RecoveryProcessNotPausedException(String message) {
        super(message);
    }

    public RecoveryProcessNotPausedException(String message, Throwable cause) {
        super(message, cause);
    }
}
