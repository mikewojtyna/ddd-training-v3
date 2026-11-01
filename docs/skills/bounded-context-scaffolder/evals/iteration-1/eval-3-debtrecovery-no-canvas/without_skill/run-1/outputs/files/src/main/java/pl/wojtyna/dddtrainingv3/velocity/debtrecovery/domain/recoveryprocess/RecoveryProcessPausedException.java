package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recoveryprocess;

/**
 * Thrown when a paused recovery process is asked to pursue the debt, or is paused again.
 */
public class RecoveryProcessPausedException extends RuntimeException {

    public RecoveryProcessPausedException(String message) {
        super(message);
    }

    public RecoveryProcessPausedException(String message, Throwable cause) {
        super(message, cause);
    }
}
